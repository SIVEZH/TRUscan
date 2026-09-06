package com.example.domain.validation

import com.example.domain.model.*

class RuleEngine {

    fun evaluate(ruleSet: RuleSet, extraction: GeminiExtraction): ValidationReport {
        val results = mutableListOf<RuleResult>()
        
        var passed = 0
        var failed = 0
        var notApplicable = 0
        var manualReview = 0
        var notRequired = 0

        for (rule in ruleSet.rules) {
            val extractedField = extraction.fields[rule.field]
            
            val evaluated = evaluateRule(rule, extractedField)
            
            when (evaluated.status) {
                RuleStatus.PASS -> passed++
                RuleStatus.FAIL -> failed++
                RuleStatus.NOT_APPLICABLE -> notApplicable++
                RuleStatus.MANUAL_REVIEW -> manualReview++
                RuleStatus.NOT_REQUIRED -> notRequired++
            }

            results.add(evaluated)
        }

        // Mandatory-only failure check:
        // overallStatus is NON_COMPLIANT iff there is at least one confirmed FAIL among applicable mandatory rules (i.e. violation == true)
        val confirmedMandatoryFailures = results.count { it.violation }
        // Check if any applicable mandatory rules require manual review and no confirmed failure
        val mandatoryManualReviews = results.count { it.mandatory && it.applicability == Applicability.APPLICABLE && it.status == RuleStatus.MANUAL_REVIEW }

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

    private fun evaluateRule(rule: Rule, extractedField: ExtractedField?): RuleResult {
        val (isMandatory, applicability) = determineRuleContract(rule)

        // 1. If NOT_APPLICABLE
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
                mandatory = isMandatory,
                applicability = Applicability.NOT_APPLICABLE,
                violation = false
            )
        }

        // 2. Field not in extraction map
        if (extractedField == null) {
            return if (isMandatory) {
                RuleResult(
                    rule_id = rule.rule_id,
                    legal_reference = rule.legal_reference,
                    field = rule.field,
                    status = RuleStatus.FAIL,
                    extracted_value = null,
                    message = "Mandatory declaration not found in the provided product images.",
                    source_image = null,
                    confidence = 0.0,
                    requirement = rule.description,
                    mandatory = true,
                    applicability = Applicability.APPLICABLE,
                    violation = true
                )
            } else {
                RuleResult(
                    rule_id = rule.rule_id,
                    legal_reference = rule.legal_reference,
                    field = rule.field,
                    status = RuleStatus.NOT_REQUIRED,
                    extracted_value = null,
                    message = "Optional declaration not present (not required).",
                    source_image = null,
                    confidence = null,
                    requirement = rule.description,
                    mandatory = false,
                    applicability = Applicability.APPLICABLE,
                    violation = false
                )
            }
        }

        // 3. Field is not present in product images
        if (!extractedField.present) {
            return if (isMandatory) {
                // If it's a conditional mandatory requirement (e.g. MANDATORY_IF_IMPORTED, MANDATORY_IF_APPLICABLE),
                // we should NOT automatically flag as a confirmed failure unless the condition is definitely true.
                // It requires MANUAL_REVIEW to avoid false-positive violations.
                if (isConditionalMandatory(rule.requirement)) {
                    RuleResult(
                        rule_id = rule.rule_id,
                        legal_reference = rule.legal_reference,
                        field = rule.field,
                        status = RuleStatus.MANUAL_REVIEW,
                        extracted_value = null,
                        message = "Conditional mandatory declaration (${rule.requirement}) was not detected. Manual check recommended to determine if condition applies.",
                        source_image = extractedField.source,
                        confidence = extractedField.confidence,
                        requirement = rule.description,
                        mandatory = true,
                        applicability = Applicability.APPLICABLE,
                        violation = false
                    )
                } else {
                    RuleResult(
                        rule_id = rule.rule_id,
                        legal_reference = rule.legal_reference,
                        field = rule.field,
                        status = RuleStatus.FAIL,
                        extracted_value = null,
                        message = "Mandatory declaration not found in the provided product images.",
                        source_image = extractedField.source,
                        confidence = extractedField.confidence,
                        requirement = rule.description,
                        mandatory = true,
                        applicability = Applicability.APPLICABLE,
                        violation = true
                    )
                }
            } else {
                // NON-MANDATORY / OPTIONAL field missing -> NOT_REQUIRED, NO VIOLATION
                RuleResult(
                    rule_id = rule.rule_id,
                    legal_reference = rule.legal_reference,
                    field = rule.field,
                    status = RuleStatus.NOT_REQUIRED,
                    extracted_value = null,
                    message = "Optional declaration not present (not required).",
                    source_image = extractedField.source,
                    confidence = extractedField.confidence,
                    requirement = rule.description,
                    mandatory = false,
                    applicability = Applicability.APPLICABLE,
                    violation = false
                )
            }
        }

        // 4. Field is present, but UNREADABLE or low confidence
        if (extractedField.status == "UNREADABLE") {
            return RuleResult(
                rule_id = rule.rule_id,
                legal_reference = rule.legal_reference,
                field = rule.field,
                status = RuleStatus.MANUAL_REVIEW,
                extracted_value = extractedField.value,
                message = "The declaration may be present but cannot be reliably read from the provided images.",
                source_image = extractedField.source,
                confidence = extractedField.confidence,
                requirement = rule.description,
                mandatory = isMandatory,
                applicability = Applicability.APPLICABLE,
                violation = false
            )
        }

        // 5. Present and readable -> PASS
        return RuleResult(
            rule_id = rule.rule_id,
            legal_reference = rule.legal_reference,
            field = rule.field,
            status = RuleStatus.PASS,
            extracted_value = extractedField.value,
            message = "Declaration detected and satisfies the rule requirement.",
            source_image = extractedField.source,
            confidence = extractedField.confidence,
            requirement = rule.description,
            mandatory = isMandatory,
            applicability = Applicability.APPLICABLE,
            violation = false
        )
    }

    private fun determineRuleContract(rule: Rule): Pair<Boolean, Applicability> {
        // 1. Explicit applicability attribute in rule JSON
        if (rule.applicability.equals("NOT_APPLICABLE", ignoreCase = true)) {
            return (rule.mandatory ?: false) to Applicability.NOT_APPLICABLE
        }

        // 2. Check exemptions or exclusions in requirement definition
        val req = rule.requirement.uppercase()
        if (req == "EXEMPTION" || req == "CONDITIONAL_EXEMPTION" || req.startsWith("DEFERRED_TO")) {
            return false to Applicability.NOT_APPLICABLE
        }

        // 3. Explicit mandatory attribute in rule JSON
        if (rule.mandatory != null) {
            val app = if (rule.applicability.equals("NOT_APPLICABLE", ignoreCase = true)) {
                Applicability.NOT_APPLICABLE
            } else {
                Applicability.APPLICABLE
            }
            return rule.mandatory to app
        }

        // 4. Deterministic fallback from requirement string
        val isMandatory = req == "MANDATORY" || req.startsWith("MANDATORY_")
        return isMandatory to Applicability.APPLICABLE
    }

    private fun isConditionalMandatory(requirement: String): Boolean {
        val req = requirement.uppercase()
        return req.startsWith("MANDATORY_IF") || req.contains("CONDITIONAL")
    }
}
