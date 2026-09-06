package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkUtils {

    /**
     * Checks if the device has an active internet connection.
     */
    fun isConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Parses exceptions into user-friendly, specific error descriptions.
     */
    fun parseApiError(throwable: Throwable, context: Context? = null): String {
        if (context != null && !isConnected(context)) {
            return "No internet connection detected. Please connect to Wi-Fi or mobile data and try again."
        }

        return when (throwable) {
            is UnknownHostException -> {
                "Unable to connect to Gemini API servers. Please check your internet connection."
            }
            is SocketTimeoutException -> {
                "Connection timed out. The server took too long to respond. Please try again."
            }
            is HttpException -> {
                val code = throwable.code()
                val rawBody = runCatching { throwable.response()?.errorBody()?.string() }.getOrNull().orEmpty()
                val detailedMessage = parseGeminiErrorMessage(rawBody)

                when (code) {
                    400 -> {
                        if (detailedMessage.contains("API_KEY_INVALID", ignoreCase = true) ||
                            detailedMessage.contains("API key not valid", ignoreCase = true) ||
                            detailedMessage.contains("key", ignoreCase = true)) {
                            "Invalid API Key (HTTP 400): The configured CHANGEABLE_API_KEY is not recognized. Please check your key in the AI Studio Secrets panel."
                        } else {
                            "Request Error (HTTP 400): $detailedMessage"
                        }
                    }
                    403 -> {
                        "Permission Denied (HTTP 403): The API key lacks permission or is expired. Details: $detailedMessage"
                    }
                    429 -> {
                        "Rate Limit / Quota Exceeded (HTTP 429): Your Gemini API quota limit has been reached, or the service is temporarily busy. Details: $detailedMessage"
                    }
                    500, 502, 503, 504 -> {
                        "Server Unavailable (HTTP $code): The Gemini service is currently overloaded or undergoing maintenance. Please retry in a few moments."
                    }
                    else -> {
                        "API Error (HTTP $code): $detailedMessage"
                    }
                }
            }
            is IOException -> {
                "Network Error: ${throwable.localizedMessage ?: "Connection interrupted. Please verify your internet connection."}"
            }
            else -> {
                throwable.localizedMessage ?: "Unexpected error: ${throwable.javaClass.simpleName}"
            }
        }
    }

    private fun parseGeminiErrorMessage(rawJson: String): String {
        if (rawJson.isBlank()) return "No additional error details."
        return try {
            val json = JSONObject(rawJson)
            if (json.has("error")) {
                val errorObj = json.getJSONObject("error")
                errorObj.optString("message", rawJson)
            } else {
                rawJson
            }
        } catch (_: Exception) {
            rawJson.take(200)
        }
    }
}
