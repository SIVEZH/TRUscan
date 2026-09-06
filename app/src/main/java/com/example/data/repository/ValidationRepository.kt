package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.util.ApiKeyProvider
import com.example.data.remote.*
import com.example.domain.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ValidationRepository(private val context: Context) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    
    suspend fun validateImage(imageUri: Uri): ValidationState = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap == null) {
                return@withContext ValidationState(ValidationStatus.ERROR, "Unable to read image.")
            }
            
            val maxDim = 1024
            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            val resizedBitmap = if (scale < 1) {
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else bitmap
            
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }
            
            val promptText = """
                Analyze this image as part of a product scanning pipeline.
                Determine if the image is a genuine photograph of a packaged commodity.
                Ensure the COMPLETE relevant side (front or back) of the package is visible, and it is NOT cropped.
                Assess if the image is an original photograph without AI generation or digital manipulation.
                
                Respond ONLY in strict JSON format matching the schema. Do NOT include markdown blocks like ```json.
                
                Expected JSON format:
                {
                  "is_packaged_product": true,
                  "image_quality": {
                    "is_clear_enough": true,
                    "is_blurry": false,
                    "is_obstructed": false
                  },
                  "package_visibility": {
                    "is_complete": true,
                    "is_cropped": false,
                    "edges_visible": true,
                    "partial_package": false
                  },
                  "authenticity": {
                    "classification": "REAL_PHOTOGRAPH",
                    "confidence": 0.95
                  },
                  "manipulation": {
                    "classification": "NO_OBVIOUS_MANIPULATION",
                    "confidence": 0.95
                  },
                  "overall_valid": true,
                  "reason": "Complete packaged product is visible..."
                }
            """.trimIndent()
            
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = promptText),
                            Part(inlineData = InlineData("image/jpeg", base64Image))
                        )
                    )
                ),
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )
            
            val apiKey = ApiKeyProvider.getApiKey()
            if (apiKey.isBlank()) {
                return@withContext ValidationState(
                    ValidationStatus.ERROR,
                    "Gemini API key is missing. Please configure your API key in AI Studio Secrets panel."
                )
            }

            val response = RetrofitClient.service.generateContent(apiKey, request)
            
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext ValidationState(ValidationStatus.ERROR, "Unable to verify the image. Please try again.")
            
            val cleanJson = responseText.replace("```json", "").replace("```", "").trim()
            Log.d("ValidationRepository", "Gemini Response: $cleanJson")
            
            val adapter = moshi.adapter(GeminiValidationResult::class.java)
            val result = adapter.fromJson(cleanJson)
                ?: return@withContext ValidationState(ValidationStatus.ERROR, "Invalid response from server.")
            
            val isValid = result.is_packaged_product &&
                          result.image_quality.is_clear_enough &&
                          result.package_visibility.is_complete &&
                          !result.package_visibility.is_cropped &&
                          result.authenticity.classification == "REAL_PHOTOGRAPH" &&
                          result.manipulation.classification == "NO_OBVIOUS_MANIPULATION"
            
            if (isValid) {
                ValidationState(ValidationStatus.VALID, "Image verified")
            } else {
                val userFriendlyReason = when {
                    !result.is_packaged_product -> "We couldn't identify a packaged product in this image. Please try again."
                    result.package_visibility.is_cropped -> "The product is partially outside the image. Please capture the complete package."
                    result.package_visibility.partial_package -> "Please capture the entire front/back of the package."
                    !result.image_quality.is_clear_enough -> "The image is too blurry to check. Please take a clearer photo."
                    result.authenticity.classification != "REAL_PHOTOGRAPH" -> "This image may not be an original photograph. Please take a new photo of the actual product."
                    result.manipulation.classification != "NO_OBVIOUS_MANIPULATION" -> "This image appears to have been edited. Please upload an original product photograph."
                    else -> "This image cannot be used. Please provide a clear photo of the complete product."
                }
                ValidationState(ValidationStatus.INVALID, userFriendlyReason)
            }
        } catch (e: retrofit2.HttpException) {
            e.printStackTrace()
            val errorBody = e.response()?.errorBody()?.string() ?: e.message()
            ValidationState(ValidationStatus.ERROR, "API Error: ${e.code()} - $errorBody")
        } catch (e: Exception) {
            e.printStackTrace()
            ValidationState(ValidationStatus.ERROR, "Unable to verify the image. Please check your internet connection and try again. Error: ${e.message}")
        }
    }
}
