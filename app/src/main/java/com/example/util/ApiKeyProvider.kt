package com.example.util

import com.example.BuildConfig

object ApiKeyProvider {
    fun getApiKey(): String {
        // 1. Try reading the new CHANGEABLE_API_KEY
        val key = runCatching { BuildConfig.CHANGEABLE_API_KEY }.getOrDefault("")
        if (key.isNotBlank() && key != "DEFAULT_KEY") {
            return key.trim()
        }

        // 2. Optional fallback to GEMINI_API_KEY if CHANGEABLE_API_KEY is not set
        val fallback = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")
        return if (fallback.isNotBlank() && fallback != "DEFAULT_KEY") fallback.trim() else ""
    }
}