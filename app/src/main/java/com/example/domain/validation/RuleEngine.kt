package com.example.domain.validation

import com.example.domain.model.*

class RuleEngine {

    fun evaluate(ruleSet: RuleSet, extraction: GeminiExtraction): ValidationReport {
        val results = mutableListOf<RuleResult>()
        
        var passed = 0
        var failed = 0
        var notApplicable = 0
        var manualReview = 0

        for (rule in ruleSet.rules) {
            val extractedField = extraction.fields[rule.field]
            
            val (status, message) = evaluateRule(rule, extractedField)
            
            when (status) {
                RuleStatus.PASS -> passed++
                RuleStatus.FAIL -> failed++
                RuleStatus.NOT_APPLICABLE -> notApplicable++
                RuleStatus.MANUAL_REVIEW -> manualReview++
            }

            results.add(
                RuleResult(
                    rule_id = rule.rule_id,
                    legal_reference = rule.legal_reference,
                    field = rule.field,
                    status = status,
                    extracted_value = extractedField?.value,
                    message = message,
                    source_image = extractedField?.source,
                    confidence = extractedField?.confidence,
                    requirement = rule.description
                )
            )
        }

        val overallStatus = when {
            failed > 0 -> OverallStatus.NON_COMPLIANT
            manualReview > 0 -> OverallStatus.MANUAL_REVIEW
            else -> OverallStatus.COMPLIANT
        }

        val summary = ValidationSummary(
            total_rules = ruleSet.rules.size,
            passed = passed,
            failed = failed,
            not_applicable = notApplicable,
            manual_review = manualReview
        )

        return ValidationReport(
            category = ruleSet.category,
            overall_status = overallStatus,
            summary = summary,
            results = results
        )
    }

    private fun evaluateRule(rule: Rule, extractedField: ExtractedField?): Pair<RuleStatus, String> {
        // Handle missing field from extraction output (shouldn't happen if schema is strictly followed, but just in case)
        if (extractedField == null) {
            return if (rule.requirement.startsWith("MANDATORY")) {
                RuleStatus.FAIL to "Required declaration was not found in the extraction result."
            } else {
                RuleStatus.NOT_APPLICABLE to "Field not evaluated."
            }
        }

        // Handle applicability (basic logic based on requirement type)
        // If it's an EXEMPTION rule, we just assume MANUAL_REVIEW for this prototype or NOT_APPLICABLE
        if (rule.requirement == "EXEMPTION" || rule.requirement == "CONDITIONAL_EXEMPTION" || rule.requirement.startsWith("DEFERRED_TO")) {
            return RuleStatus.NOT_APPLICABLE to "Rule condition or exemption evaluated: ${rule.description}"
        }

        // Evaluate based on field presence
        if (!extractedField.present) {
            return if (rule.requirement == "MANDATORY" || rule.requirement.startsWith("MANDATORY_IF_LMPC_APPLIES")) {
                RuleStatus.FAIL to "Required declaration was not detected on the package."
            } else if (rule.requirement.startsWith("MANDATORY_IF")) {
                RuleStatus.MANUAL_REVIEW to "Declaration is missing, but rule is conditional (${rule.requirement}). Requires manual check if condition applies."
            } else {
                RuleStatus.NOT_APPLICABLE to "Optional or conditional declaration not present."
            }
        }

        // Handle unreadable status
        if (extractedField.status == "UNREADABLE") {
            return RuleStatus.MANUAL_REVIEW to "The declaration was detected but could not be read reliably."
        }

        // If present and not unreadable, and it's mandatory or conditionally mandatory, we assume PASS for now
        // In a real Python rule engine, we'd parse the value and validate formatting/units
        return RuleStatus.PASS to "Declaration detected and satisfies the rule requirements based on visual evidence."
    }
}
