package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RuleSet(
    val dataset_name: String? = null,
    val version: String? = null,
    val commodity_category: String? = null,
    val rule_count: Int? = null,
    val exception_count: Int? = null,
    val note: String? = null,
    val rules: List<Rule> = emptyList(),
    val exceptions: List<GlobalException>? = null,
    val category: String? = null
) {
    // Adapter for backward compatibility or easier access
    val categoryName: String
        get() = commodity_category ?: category ?: "UNKNOWN"
}

@JsonClass(generateAdapter = true)
data class GlobalExceptionCondition(
    val net_quantity: String? = null,
    val consumer_type: String? = null,
    val package_context: String? = null,
    val commodity_category: String? = null,
    val requirement: String? = null,
    val product_type: String? = null
)

@JsonClass(generateAdapter = true)
data class GlobalException(
    val exception_id: String,
    val legal_reference: String? = null,
    val type: String? = null,
    val field: String? = null,
    val requirement: String? = null,
    val condition: GlobalExceptionCondition? = null,
    val effect: String? = null,
    val description: String? = null,
    val validation_type: String? = null
)

@JsonClass(generateAdapter = true)
data class RuleAppliesTo(
    val category: String? = null,
    val product_types: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class RuleExceptionCondition(
    val product_type: String? = null,
    val category: String? = null
)

@JsonClass(generateAdapter = true)
data class RuleException(
    val condition: RuleExceptionCondition? = null,
    val effect: String? = null
)

@JsonClass(generateAdapter = true)
data class Rule(
    val rule_id: String,
    val legal_reference: String? = null,
    val category: String? = null,
    val field: String,
    val requirement: String? = null,
    val condition: String? = null,
    val description: String? = null,
    val validation_type: String? = null,
    val mandatory: Boolean? = null,
    val applicability: String? = null,
    val applies_to: RuleAppliesTo? = null,
    val exceptions: List<String>? = null, // In new JSON, exceptions is List<String>
    val overrides: List<String>? = null,
    val overridden_by: List<String>? = null,
    val special_cases: List<String>? = null
)
