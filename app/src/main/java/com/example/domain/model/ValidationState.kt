package com.example.domain.model

enum class ValidationStatus {
    EMPTY,
    VALIDATING,
    VALID,
    INVALID,
    ERROR
}

data class ValidationState(
    val status: ValidationStatus = ValidationStatus.EMPTY,
    val reason: String? = null
)
