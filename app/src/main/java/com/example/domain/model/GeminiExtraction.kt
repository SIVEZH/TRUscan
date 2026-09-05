package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CategoryDetectionResult(
    val category: String, // FOOD, COSMETICS, MEDICINES, UNKNOWN
    val confidence: Double,
    val evidence: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ExtractedField(
    val present: Boolean,
    val value: String? = null,
    val source: String? = null, // FRONT, BACK, BOTH
    val confidence: Double? = null,
    val status: String? = null // UNREADABLE, etc.
)

@JsonClass(generateAdapter = true)
data class GeminiExtraction(
    val category: String,
    val category_confidence: Double,
    val fields: Map<String, ExtractedField>
)
