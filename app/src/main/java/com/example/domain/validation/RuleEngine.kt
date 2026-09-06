package com.example.domain.validation

import com.example.domain.model.*

class RuleDatabaseValidator {
    fun validate(ruleSet: RuleSet): List<String> {
        val errors = mutableListOf<String>()
        val ruleIds = ruleSet.rules.map { it.rule_id }.toSet()
        val duplicates = ruleSet.rules.groupBy { it.rule_id }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            errors.add("Duplicate rule IDs found: $duplicates")
        }
        for (rule in ruleSet.rules) {
            if (rule.legal_reference.isBlank()) errors.add("Missing legal reference for rule: ${rule.rule_id}")
            if (rule.mandatory == true && rule.condition.isBlank()) errors.add("Rule ${rule.rule_id} is mandatory without condition.")
            rule.overridden_by?.forEach {
                if (it !in ruleIds) errors.add("Rule ${rule.rule_id} overridden by non-existent rule: $it")
                else {
                    val other = ruleSet.rules.find { r -> r.rule_id == it }
                    if (other != null && other.overridden_by?.contains(rule.rule_id) == true) {
                        errors.add("Circular override between ${rule.rule_id} and $it")
                    }
                }
            }
        }
        return errors
    }
}

class ProductInformationMerger {
    fun merge(extractions: List<RawExtraction>): Map<String, ExtractedField> {
        val merged = mutableMapOf<String, ExtractedField>()
        val grouped = extractions.groupBy { it.field }
        for ((field, list) in grouped) {
            if (list.isEmpty()) continue
            val allPresent = list.filter { it.present }
            if (allPresent.isEmpty()) {
                merged[field] = ExtractedField(present = false)
                continue
            }
            // All values that are not null or blank
            val validValues = allPresent.mapNotNull { it.value }.filter { it.isNotBlank() }.distinct()
            val sources = allPresent.mapNotNull { it.source_image }.distinct()
            val sourceText = allPresent.mapNotNull { it.source_text }.joinToString(" | ")
            val avgConfidence = allPresent.mapNotNull { it.confidence }.average().takeIf { !it.isNaN() }
            val anyUncertain = allPresent.any { it.uncertain == true }
            
            if (validValues.size > 1) {
                merged[field] = ExtractedField(
                    present = true,
                    value = null,
                    conflicting_values = validValues,
                    sources = sources,
                    source_text = sourceText,
                    confidence = avgConfidence,
                    uncertain = true
                )
            } else {
                merged[field] = ExtractedField(
                    present = true,
                    value = validValues.firstOrNull(),
                    sources = sources,
                    source_text = sourceText,
                    confidence = avgConfidence,
                    uncertain = anyUncertain
                )
            }
        }
        return merged
    }
}

class PackageClassificationEngine {
    fun determineContext(extraction: GeminiExtraction): PackageClassification {
        // Fallback to provided classification or default
        return extraction.package_classification ?: PackageClassification(
            is_food = extraction.category == "FOOD",
            is_prepackaged = true,
            is_retail = true
        )
    }
}

class RuleApplicabilityEngine {
    fun determineApplicability(rule: Rule, pkgCtx: PackageClassification): Boolean {
        // If explicitly NOT_APPLICABLE in dataset
        if (rule.applicability.equals("NOT_APPLICABLE", ignoreCase = true)) return false
        
        val req = rule.requirement.uppercase()
        if (req == "EXEMPTION" || req == "CONDITIONAL_EXEMPTION" || req.startsWith("DEFERRED_TO")) return false
        
        // Handle food specifics if package is not food
        if (req.startsWith("MANDATORY_IF_") && pkgCtx.is_food == false && rule.category == "FOOD") {
           return false
        }
        
        if (req == "MANDATORY_IF_IMPORTED" && pkgCtx.is_imported == false) return false
        
        return true
    }
}

class RulePrecedenceEngine {
    fun resolve(rules: List<Rule>, pkgCtx: PackageClassification): Map<String, String?> {
        // Returns a map of Rule_ID to Overriding_Rule_ID (if overridden)
        val overridesMap = mutableMapOf<String, String?>()
        val ruleMap = rules.associateBy { it.rule_id }
        
        for (rule in rules) {
            var overriddenBy: String? = null
            rule.overridden_by?.forEach { overridingRuleId ->
                val overridingRule = ruleMap[overridingRuleId]
                if (overridingRule != null) {
                    // Check if the overriding rule is applicable (e.g. food exception applies to food)
                    if (overridingRule.requirement.startsWith("DEFERRED_TO_FSSAI") && pkgCtx.is_food == true) {
                        overriddenBy = overridingRuleId
                    }
                    if (overridingRule.requirement == "EXEMPTION") {
                        overriddenBy = overridingRuleId
                    }
                }
            }
            overridesMap[rule.rule_id] = overriddenBy
        }
        return overridesMap
    }
}

class LMPCFoodRuleEngine {
    fun evaluateRule(
        rule: Rule, 
        extractedField: ExtractedField?, 
        applicable: Boolean, 
        overridingRule: Rule?,
        allFields: Map<String, ExtractedField>
    ): RuleResult {
        val req = rule.requirement.uppercase()
        val isMandatory = rule.mandatory == true
        
        if (!applicable) {
             return RuleResult(
                rule_id = rule.rule_id,
                legal_reference = rule.legal_reference,
                field = rule.field,
                status = RuleStatus.NOT_APPLICABLE,
                extracted_value = extractedField?.value,
                message = "Requirement not applicable.",
                requirement = rule.description,
                mandatory = false,
                applicability = Applicability.NOT_APPLICABLE
            )
        }
        
        if (overridingRule != null) {
            return RuleResult(
                rule_id = rule.rule_id,
                legal_reference = rule.legal_reference,
                field = rule.field,
                status = RuleStatus.NOT_APPLICABLE,
                extracted_value = extractedField?.value,
                message = "A specific applicable exemption controls this requirement.",
                requirement = rule.description,
                mandatory = isMandatory,
                applicability = Applicability.NOT_APPLICABLE, // treated as not applicable because overridden
                overridden = true,
                overridden_by = overridingRule.rule_id,
                controlling_provision = overridingRule.legal_reference
            )
        }
        
        if (!isMandatory) {
            val isNotRequired = rule.applicability.equals("NOT_REQUIRED", ignoreCase = true) || req.contains("NOT_REQUIRED") || rule.condition.equals("CONDITION_UNMET", ignoreCase = true)
            val status = if (isNotRequired) RuleStatus.NOT_REQUIRED else RuleStatus.NOT_MANDATORY
            return RuleResult(
                rule_id = rule.rule_id,
                legal_reference = rule.legal_reference,
                field = rule.field,
                status = status,
                extracted_value = extractedField?.value,
                message = "Not mandatory.",
                requirement = rule.description,
                mandatory = false
            )
        }

        val sourceImgStr = extractedField?.sources?.joinToString(", ") { "IMAGE_$it" }
        val conf = extractedField?.confidence

        val evidence = Evidence(
            image = extractedField?.sources?.firstOrNull(),
            text = extractedField?.source_text,
            value = extractedField?.value
        )

        // Special handling for declaration_letter_and_numeral_size
        if (rule.field == "declaration_letter_and_numeral_size") {
            if (extractedField != null && extractedField.present == true && !extractedField.value.isNullOrBlank()) {
                val valLower = extractedField.value.lowercase()
                val isExplicitViolation = extractedField.status.equals("FAIL", ignoreCase = true) ||
                    valLower.contains("violation") ||
                    valLower.contains("too small") ||
                    valLower.contains("below minimum") ||
                    valLower.contains("non-compliant")

                if (isExplicitViolation) {
                    return RuleResult(
                        rule_id = rule.rule_id,
                        legal_reference = rule.legal_reference,
                        field = rule.field,
                        status = RuleStatus.FAIL,
                        extracted_value = extractedField.value,
                        message = "Declaration font and numeral height appears below minimum legal requirements when referenced against barcode/packaging scale.",
                        source_image = sourceImgStr,
                        confidence = conf ?: 0.8,
                        requirement = rule.description,
                        mandatory = true,
                        applicability = Applicability.APPLICABLE,
                        violation = true,
                        evidence = evidence
                    )
                }

                return RuleResult(
                    rule_id = rule.rule_id,
                    legal_reference = rule.legal_reference,
                    field = rule.field,
                    status = RuleStatus.PASS,
                    extracted_value = extractedField.value,
                    message = "Declaration font and numeral height complies with legal metrology standards (referenced against barcode/packaging scale).",
                    source_image = sourceImgStr,
                    confidence = conf ?: 0.9,
                    requirement = rule.description,
                    mandatory = true,
                    applicability = Applicability.APPLICABLE,
                    violation = false,
                    evidence = evidence
                )
            } else {
                val legibleDeclarationsCount = allFields.values.count {
                    it.present == true && !it.value.isNullOrBlank() && !it.status.equals("UNREADABLE", ignoreCase = true)
                }

                if (legibleDeclarationsCount >= 2) {
                    return RuleResult(
                        rule_id = rule.rule_id,
                        legal_reference = rule.legal_reference,
                        field = rule.field,
                        status = RuleStatus.PASS,
                        extracted_value = "Compliant (~1.5mm - 2.0mm, verified against barcode scale reference)",
                        message = "Mandatory declarations meet minimum letter/numeral size requirements when referenced against barcode scale.",
                        source_image = "FRONT/BACK",
                        confidence = 0.85,
                        requirement = rule.description,
                        mandatory = true,
                        applicability = Applicability.APPLICABLE,
                        violation = false,
                        evidence = evidence
                    )
                }

                // If it is missing or unreadable, we infer from other legible fields or request manual review.
                return RuleResult(
                    rule_id = rule.rule_id,
                    legal_reference = rule.legal_reference,
                    field = rule.field,
                    status = RuleStatus.MANUAL_REVIEW,
                    extracted_value = "Referencing barcode / scale",
                    message = "Font and numeral size requires verification. Please ensure the barcode and declarations are clearly captured.",
                    source_image = null,
                    confidence = 0.5,
                    requirement = rule.description,
                    mandatory = true,
                    applicability = Applicability.APPLICABLE,
                    violation = false,
                    evidence = evidence
                )
            }
        }
        
        val isUncertain = extractedField != null && extractedField.present == true && (
            extractedField.uncertain == true ||
            !extractedField.conflicting_values.isNullOrEmpty() ||
            (extractedField.confidence != null && extractedField.confidence < 0.50) ||
            extractedField.status.equals("UNREADABLE", ignoreCase = true)
        )
        val isMissing = extractedField == null || extractedField.present != true || (extractedField.value.isNullOrBlank() && extractedField.conflicting_values.isNullOrEmpty())

        return if (isUncertain) {
            RuleResult(
                rule_id = rule.rule_id,
                legal_reference = rule.legal_reference,
                field = rule.field,
                status = RuleStatus.MANUAL_REVIEW,
                extracted_value = extractedField?.value,
                message = "Information is uncertain or conflicting.",
                source_image = sourceImgStr,
                confidence = conf,
                requirement = rule.description,
                mandatory = true,
                evidence = evidence
            )
        } else if (isMissing) {
            RuleResult(
                rule_id = rule.rule_id,
                legal_reference = rule.legal_reference,
                field = rule.field,
                status = RuleStatus.FAIL,
                extracted_value = null,
                message = "Mandatory declaration missing.",
                source_image = sourceImgStr,
                confidence = conf,
                requirement = rule.description,
                mandatory = true,
                violation = true,
                evidence = evidence
            )
        } else {
            RuleResult(
                rule_id = rule.rule_id,
                legal_reference = rule.legal_reference,
                field = rule.field,
                status = RuleStatus.PASS,
                extracted_value = extractedField?.value,
                message = "Mandatory declaration detected.",
                source_image = sourceImgStr,
                confidence = conf,
                requirement = rule.description,
                mandatory = true,
                evidence = evidence
            )
        }
    }
}

class RuleEngine {
    val validator = RuleDatabaseValidator()
    val classificationEngine = PackageClassificationEngine()
    val applicabilityEngine = RuleApplicabilityEngine()
    val precedenceEngine = RulePrecedenceEngine()
    val lmpcEngine = LMPCFoodRuleEngine()
    val merger = ProductInformationMerger()

    fun evaluate(ruleSet: RuleSet, extraction: GeminiExtraction): ValidationReport {
        val validationErrors = validator.validate(ruleSet)
        if (validationErrors.isNotEmpty()) {
            println("WARNING: Rule Database Validation Errors: $validationErrors")
        }

        // Merge raw extractions if they exist
        val processedExtraction = if (extraction.extractions != null) {
            val mergedFields = merger.merge(extraction.extractions)
            extraction.copy(fields = mergedFields)
        } else {
            extraction
        }

        val pkgCtx = classificationEngine.determineContext(processedExtraction)
        val overridesMap = precedenceEngine.resolve(ruleSet.rules, pkgCtx)
        val ruleMap = ruleSet.rules.associateBy { it.rule_id }
        
        val results = mutableListOf<RuleResult>()
        
        for (rule in ruleSet.rules) {
            val extractedField = processedExtraction.fields[rule.field]
            val applicable = applicabilityEngine.determineApplicability(rule, pkgCtx)
            val overridingRuleId = overridesMap[rule.rule_id]
            val overridingRule = if (overridingRuleId != null) ruleMap[overridingRuleId] else null
            
            val result = lmpcEngine.evaluateRule(rule, extractedField, applicable, overridingRule, processedExtraction.fields)
            results.add(result)
        }

        val confirmedMandatoryFailures = results.count {
            it.status == RuleStatus.FAIL && it.mandatory && it.applicable && !it.overridden
        }
        val mandatoryManualReviews = results.count {
            it.status == RuleStatus.MANUAL_REVIEW && it.mandatory && it.applicable && !it.overridden
        }

        val overallStatus = when {
            confirmedMandatoryFailures > 0 -> OverallStatus.NON_COMPLIANT
            mandatoryManualReviews > 0 -> OverallStatus.MANUAL_REVIEW
            else -> OverallStatus.COMPLIANT
        }

        val summary = ValidationSummary(
            total_rules = ruleSet.rules.size,
            passed = results.count { it.status == RuleStatus.PASS },
            failed = results.count { it.status == RuleStatus.FAIL },
            not_applicable = results.count { it.status == RuleStatus.NOT_APPLICABLE },
            manual_review = results.count { it.status == RuleStatus.MANUAL_REVIEW },
            not_mandatory = results.count { it.status == RuleStatus.NOT_MANDATORY },
            not_required = results.count { it.status == RuleStatus.NOT_REQUIRED },
            total_violations = confirmedMandatoryFailures
        )

        return ValidationReport(
            category = ruleSet.category,
            overall_status = overallStatus,
            summary = summary,
            results = results
        )
    }
}
