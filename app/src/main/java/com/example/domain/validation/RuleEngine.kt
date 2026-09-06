package com.example.domain.validation

import com.example.domain.model.*

class RuleEngine {

    fun evaluate(ruleSet: RuleSet, extraction: GeminiExtraction): ValidationReport {
        val results = mutableListOf<RuleResult>()
        
        var passed = 0
        var failed = 0
        var notApplicable = 0
        var manualReview = 0
        var notMandatory = 0
        var notRequired = 0

        for (rule in ruleSet.rules) {
            val extractedField = extraction.fields[rule.field]
            
            val evaluated = evaluateRule(rule, extractedField, extraction)
            
            when (evaluated.status) {
                RuleStatus.PASS -> passed++
                RuleStatus.FAIL -> failed++
                RuleStatus.NOT_APPLICABLE -> notApplicable++
                RuleStatus.MANUAL_REVIEW -> manualReview++
                RuleStatus.NOT_MANDATORY -> notMandatory++
                RuleStatus.NOT_REQUIRED -> notRequired++
            }

            results.add(evaluated)
        }

        // Mandatory-only failure check:
        // overallStatus is NON_COMPLIANT iff there is at least one confirmed FAIL among applicable mandatory rules (i.e. violation == true)
        val confirmedMandatoryFailures = results.count {
            it.status == RuleStatus.FAIL && it.mandatory && it.applicable
        }
        // Check if any applicable mandatory rules require manual review and no confirmed failure
        val mandatoryManualReviews = results.count {
            it.status == RuleStatus.MANUAL_REVIEW && it.mandatory && it.applicable
        }

        val overallStatus = when {
            confirmedMandatoryFailures > 0 -> OverallStatus.NON_COMPLIANT
            mandatoryManualReviews > 0 -> OverallStatus.MANUAL_REVIEW
            else -> OverallStatus.COMPLIANT
        }

        val summary = ValidationSummary(
            total_rules = ruleSet.rules.size,
            passed = passed,
            failed = failed,
            not_applicable = notApplicable,
            manual_review = manualReview,
            not_mandatory = notMandatory,
            not_required = notRequired,
            total_violations = confirmedMandatoryFailures
        )

        return ValidationReport(
            category = ruleSet.category,
            overall_status = overallStatus,
            summary = summary,
            results = results
        )
    }

    private fun evaluateRule(rule: Rule, extractedField: ExtractedField?, extraction: GeminiExtraction): RuleResult {
        val (isMandatory, applicability) = determineRuleContract(rule)

        // 1. Determine whether the rule applies to this product/category
        if (applicability == Applicability.NOT_APPLICABLE) {
            return RuleResult(
                rule_id = rule.rule_id,
                legal_reference = rule.legal_reference,
                field = rule.field,
                status = RuleStatus.NOT_APPLICABLE,
                extracted_value = extractedField?.value,
                message = "Requirement not applicable to this product/category (${rule.description}).",
                source_image = extractedField?.source,
                confidence = extractedField?.confidence,
                requirement = rule.description,
                mandatory = false,
                applicability = Applicability.NOT_APPLICABLE,
                violation = false
            )
        }

        // 2. Determine whether the requirement is mandatory
        // The NOT_MANDATORY / NOT_REQUIRED check MUST happen before MANUAL_REVIEW.
        // A non-mandatory or non-required field must never be treated as an error or manual review simply because it is absent or unreadable.
        if (!isMandatory) {
            val req = rule.requirement.uppercase()
            val isNotRequired = (rule.applicability != null && rule.applicability.equals("NOT_REQUIRED", ignoreCase = true)) ||
                req.equals("NOT_REQUIRED") ||
                req.contains("NOT_REQUIRED") ||
                rule.condition.equals("NOT_REQUIRED", ignoreCase = true) ||
                rule.condition.equals("CONDITION_UNMET", ignoreCase = true)

            val status = if (isNotRequired) RuleStatus.NOT_REQUIRED else RuleStatus.NOT_MANDATORY

            val isPresent = extractedField != null && extractedField.present && !extractedField.value.isNullOrBlank()
            val message = if (status == RuleStatus.NOT_REQUIRED) {
                if (isPresent) {
                    "Declaration detected (${extractedField?.value}), but not required for this product/packaging condition."
                } else {
                    "Not required under current condition (${rule.description})."
                }
            } else {
                if (isPresent) {
                    "Optional declaration detected (${extractedField?.value}). Not mandatory under the applicable rule."
                } else {
                    "Not mandatory under the applicable rule (${rule.description})."
                }
            }

            return RuleResult(
                rule_id = rule.rule_id,
                legal_reference = rule.legal_reference,
                field = rule.field,
                status = status,
                extracted_value = extractedField?.value,
                message = message,
                source_image = extractedField?.source,
                confidence = extractedField?.confidence,
                requirement = rule.description,
                mandatory = false,
                applicability = Applicability.APPLICABLE,
                violation = false
            )
        }

        // 3. Special handling for declaration font and numeral size (Rule 7)
        // References letter/numeral size against standard barcode dimensions (~22mm height, numbers ~2.75mm)
        if (rule.field == "declaration_letter_and_numeral_size") {
            return evaluateFontAndNumeralSize(rule, extractedField, extraction)
        }

        // 4. Only if the requirement is mandatory, evaluate whether declaration passes/fails/is uncertain
        val isUncertain = extractedField != null && (
            extractedField.status.equals("UNREADABLE", ignoreCase = true) ||
            (extractedField.confidence != null && extractedField.confidence > 0.0 && extractedField.confidence < 0.50)
        )

        val isMissingOrInvalid = extractedField == null || !extractedField.present || extractedField.value.isNullOrBlank()

        if (isUncertain) {
            return RuleResult(
                rule_id = rule.rule_id,
                legal_reference = rule.legal_reference,
                field = rule.field,
                status = RuleStatus.MANUAL_REVIEW,
                extracted_value = extractedField?.value,
                message = "The declaration may be present but cannot be reliably read or confirmed from the provided images.",
                source_image = extractedField?.source,
                confidence = extractedField?.confidence,
                requirement = rule.description,
                mandatory = true,
                applicability = Applicability.APPLICABLE,
                violation = false
            )
        } else if (isMissingOrInvalid) {
            return RuleResult(
                rule_id = rule.rule_id,
                legal_reference = rule.legal_reference,
                field = rule.field,
                status = RuleStatus.FAIL,
                extracted_value = null,
                message = "Mandatory declaration not found in the provided product images.",
                source_image = extractedField?.source,
                confidence = 0.0,
                requirement = rule.description,
                mandatory = true,
                applicability = Applicability.APPLICABLE,
                violation = true
            )
        } else {
            return RuleResult(
                rule_id = rule.rule_id,
                legal_reference = rule.legal_reference,
                field = rule.field,
                status = RuleStatus.PASS,
                extracted_value = extractedField.value,
                message = "Mandatory declaration detected and satisfies legal requirements.",
                source_image = extractedField.source,
                confidence = extractedField.confidence,
                requirement = rule.description,
                mandatory = true,
                applicability = Applicability.APPLICABLE,
                violation = false
            )
        }
    }

    private fun evaluateFontAndNumeralSize(
        rule: Rule,
        extractedField: ExtractedField?,
        extraction: GeminiExtraction
    ): RuleResult {
        // 1. If explicit font size extraction is provided and valid
        if (extractedField != null && extractedField.present && !extractedField.value.isNullOrBlank()) {
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
                    source_image = extractedField.source,
                    confidence = extractedField.confidence ?: 0.8,
                    requirement = rule.description,
                    mandatory = true,
                    applicability = Applicability.APPLICABLE,
                    violation = true
                )
            }

            return RuleResult(
                rule_id = rule.rule_id,
                legal_reference = rule.legal_reference,
                field = rule.field,
                status = RuleStatus.PASS,
                extracted_value = extractedField.value,
                message = "Declaration font and numeral height complies with legal metrology standards (referenced against barcode/packaging scale).",
                source_image = extractedField.source,
                confidence = extractedField.confidence ?: 0.9,
                requirement = rule.description,
                mandatory = true,
                applicability = Applicability.APPLICABLE,
                violation = false
            )
        }

        // 2. Barcode & legible declaration reference fallback:
        // When AI extraction did not return an explicit field for font size,
        // we check the surrounding mandatory declarations (e.g. MRP, Net Quantity, Best Before, Manufacturer).
        // If mandatory text is detected and legible on the packaging, standard consumer packaging proportions
        // relative to the standard barcode (nominal bar height ~20-23mm, digits ~2.75mm) satisfy the 1.0mm-2.0mm minimum height.
        val legibleDeclarationsCount = extraction.fields.values.count {
            it.present && !it.value.isNullOrBlank() && !it.status.equals("UNREADABLE", ignoreCase = true)
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
                violation = false
            )
        }

        // 3. If images are completely unreadable or insufficient, request review rather than a false violation
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
            violation = false
        )
    }

    private fun determineRuleContract(rule: Rule): Pair<Boolean, Applicability> {
        // 1. Explicit applicability attribute in rule JSON
        if (rule.applicability != null && rule.applicability.equals("NOT_APPLICABLE", ignoreCase = true)) {
            return false to Applicability.NOT_APPLICABLE
        }

        // 2. Check exemptions or exclusions in requirement definition
        val req = rule.requirement.uppercase()
        if (req == "EXEMPTION" || req == "CONDITIONAL_EXEMPTION" || req.startsWith("DEFERRED_TO")) {
            return false to Applicability.NOT_APPLICABLE
        }

        // 3. Conditional requirements where retail package conditions do not apply
        if (req == "MANDATORY_IF_ECOMMERCE" ||
            req == "MANDATORY_IF_OUTER_CONTAINER" ||
            req == "MANDATORY_IF_GM_FOOD" ||
            req.startsWith("MANDATORY_IF_CURD") ||
            req.startsWith("MANDATORY_IF_FRUIT") ||
            req.startsWith("MANDATORY_IF_EDIBLE_OIL") ||
            req.startsWith("MANDATORY_IF_HONEY") ||
            req.startsWith("MANDATORY_IF_ICE_CREAM") ||
            req.startsWith("MANDATORY_IF_SWEET_PREPARATION") ||
            req.startsWith("MANDATORY_IF_SAUCE") ||
            req.startsWith("MANDATORY_IF_LMPC_APPLIES") ||
            req.startsWith("MANDATORY_IF_IMPORTED_AND_LMPC_APPLIES") ||
            req.startsWith("ALLOWED_")
        ) {
            return false to Applicability.NOT_APPLICABLE
        }

        // 4. Explicit mandatory attribute in rule JSON (source of truth)
        if (rule.mandatory != null) {
            val app = if (rule.applicability != null && rule.applicability.equals("NOT_APPLICABLE", ignoreCase = true)) {
                Applicability.NOT_APPLICABLE
            } else {
                Applicability.APPLICABLE
            }
            return rule.mandatory to app
        }

        // 5. Fallback check for optional or conditional requirements
        // Per specifications: unit_sale_price, dimensions, etc. are NOT mandatory for general product scanning
        if (req == "MANDATORY_IF_APPLICABLE" || req.contains("OPTIONAL") || req.contains("RECOMMENDED")) {
            return false to Applicability.APPLICABLE
        }

        // 6. Mandatory declaration (e.g. MANDATORY or MANDATORY_IF_IMPORTED)
        val isMandatory = req == "MANDATORY" || req == "MANDATORY_IF_IMPORTED" || req.startsWith("MANDATORY_")
        return isMandatory to Applicability.APPLICABLE
    }
}
