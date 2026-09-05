package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ImageQuality(
    val is_clear_enough: Boolean,
    val is_blurry: Boolean,
    val is_obstructed: Boolean
)

@JsonClass(generateAdapter = true)
data class PackageVisibility(
    val is_complete: Boolean,
    val is_cropped: Boolean,
    val edges_visible: Boolean,
    val partial_package: Boolean
)

@JsonClass(generateAdapter = true)
data class Authenticity(
    val classification: String,
    val confidence: Double
)

@JsonClass(generateAdapter = true)
data class Manipulation(
    val classification: String,
    val confidence: Double
)

@JsonClass(generateAdapter = true)
data class GeminiValidationResult(
    val is_packaged_product: Boolean,
    val image_quality: ImageQuality,
    val package_visibility: PackageVisibility,
    val authenticity: Authenticity,
    val manipulation: Manipulation,
    val overall_valid: Boolean,
    val reason: String
)
