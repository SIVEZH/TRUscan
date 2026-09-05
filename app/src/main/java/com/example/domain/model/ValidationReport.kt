package com.example.domain.model

enum class OverallStatus {
    COMPLIANT,
    NON_COMPLIANT,
    MANUAL_REVIEW
}

enum class RuleStatus {
    PASS,
    FAIL,
    NOT_APPLICABLE,
    MANUAL_REVIEW
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
    val requirement: String
)

data class ValidationSummary(
    val total_rules: Int,
    val passed: Int,
    val failed: Int,
    val not_applicable: Int,
    val manual_review: Int
)

data class ValidationReport(
    val category: String,
    val overall_status: OverallStatus,
    val summary: ValidationSummary,
    val results: List<RuleResult>
)
