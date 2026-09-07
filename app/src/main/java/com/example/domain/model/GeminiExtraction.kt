package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CategoryDetectionResult(
    val category: String,
    val confidence: Double,
    val evidence: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class PackageClassification(
    val category: String? = null,
    val product_type: String? = null,
    val sub_type: String? = null,
    val is_food: Boolean? = null,
    val is_prepackaged: Boolean? = null,
    val is_retail: Boolean? = null,
    val is_wholesale: Boolean? = null,
    val is_imported: Boolean? = null,
    val is_industrial: Boolean? = null,
    val is_institutional: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class RawExtraction(
    val field: String,
    val present: Boolean,
    val value: String? = null,
    val source_image: Int? = null,
    val source_text: String? = null,
    val confidence: Double? = null,
    val uncertain: Boolean? = null,
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class RawExtractionResult(
    val extractions: List<RawExtraction>,
    val package_classification: PackageClassification? = null,
    val category: String? = null,
    val category_confidence: Double? = null
)

@JsonClass(generateAdapter = true)
data class ExtractedField(
    val present: Boolean? = null,
    val value: String? = null,
    val sources: List<Int>? = null,
    val source_text: String? = null,
    val confidence: Double? = null,
    val uncertain: Boolean? = null,
    val conflicting_values: List<String>? = null,
    val source: String? = null,
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiExtraction(
    val category: String,
    val category_confidence: Double,
    val fields: Map<String, ExtractedField>,
    val package_classification: PackageClassification? = null,
    val extractions: List<RawExtraction>? = null
)
