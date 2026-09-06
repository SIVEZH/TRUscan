package com.example.util

import com.example.BuildConfig

object ApiKeyProvider {
    fun getApiKey(): String {
        val key = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")
        return if (key.isNotBlank() && key != "DEFAULT_KEY") key.trim() else ""
    }
}
