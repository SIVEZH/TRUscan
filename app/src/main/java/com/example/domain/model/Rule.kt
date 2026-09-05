package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RuleSet(
    val schema_version: String,
    val dataset_type: String,
    val category: String,
    val title: String,
    val scope: String,
    val source_note: String,
    val rules: List<Rule>
)

@JsonClass(generateAdapter = true)
data class Rule(
    val rule_id: String,
    val legal_reference: String,
    val category: String,
    val field: String,
    val requirement: String,
    val condition: String,
    val description: String,
    val validation_type: String
)
