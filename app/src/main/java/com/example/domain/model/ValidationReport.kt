package com.example.domain.model

enum class OverallStatus {
    COMPLIANT,
    NON_COMPLIANT,
    MANUAL_REVIEW
}

enum class RuleStatus {
    PASS,
    FAIL,
    NOT_MANDATORY,
    NOT_REQUIRED,
    NOT_APPLICABLE,
    MANUAL_REVIEW
}

enum class Applicability {
    APPLICABLE,
    NOT_APPLICABLE
}

data class RuleResult(
    val rule_id: String,
    val legal_reference: String,
    val field: String,
    val status: RuleStatus,
    val extracted_value: String?,
    val message: String,
    val source_image: String? = null,
    val confidence: Double? = null,
    val requirement: String,
    val mandatory: Boolean = false,
    val applicability: Applicability = Applicability.APPLICABLE,
    val violation: Boolean = false
) {
    val applicable: Boolean
        get() = applicability == Applicability.APPLICABLE
}

data class ValidationSummary(
    val total_rules: Int,
    val passed: Int,
    val failed: Int,
    val not_applicable: Int,
    val manual_review: Int,
    val not_mandatory: Int = 0,
    val not_required: Int = 0,
    val total_violations: Int = 0
)

data class ValidationReport(
    val category: String,
    val overall_status: OverallStatus,
    val summary: ValidationSummary,
    val results: List<RuleResult>
) {
    /**
     * Only applicable mandatory failures are considered true violations.
     */
    val violations: List<RuleResult>
        get() = results.filter { it.violation }

    /**
     * Single source of truth for complaint eligibility:
     * Available ONLY when the report contains at least one confirmed mandatory violation.
     */
    val hasConfirmedMandatoryViolation: Boolean
        get() = results.any {
            it.status == RuleStatus.FAIL && it.mandatory && it.applicable
        }

    val canRaiseComplaint: Boolean
        get() = hasConfirmedMandatoryViolation
}
