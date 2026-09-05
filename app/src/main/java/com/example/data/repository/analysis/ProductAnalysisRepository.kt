package com.example.data.repository.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.example.data.remote.*
import com.example.domain.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ProductAnalysisRepository(private val context: Context) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private fun loadRuleSet(category: String): RuleSet? {
        val fileName = when (category) {
            "FOOD" -> "rules/food_rules.json"
            "COSMETICS" -> "rules/cosmetics_rules.json"
            "MEDICINES" -> "rules/medicine_rules.json"
            else -> return null
        }
        
        return try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            moshi.adapter(RuleSet::class.java).fromJson(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun analyzeProduct(frontUri: Uri, backUri: Uri): Result<Pair<ValidationReport, GeminiExtraction>> = withContext(Dispatchers.IO) {
        try {
            val frontBase64 = uriToBase64(frontUri)
            val backBase64 = uriToBase64(backUri)

            if (frontBase64 == null || backBase64 == null) {
                return@withContext Result.failure(Exception("Unable to process images."))
            }

            // Step 1: Detect Category
            val category = detectCategory(frontBase64, backBase64)
            if (category == "UNKNOWN") {
                return@withContext Result.failure(Exception("Unable to determine whether this product is food, cosmetic, or medicine. Please provide clearer images."))
            }

            // Step 2: Load Rules
            val ruleSet = loadRuleSet(category)
                ?: return@withContext Result.failure(Exception("Unable to load the validation rules. Please try again later."))

            // Step 3: Extract Declarations
            val extraction = extractDeclarations(frontBase64, backBase64, category, ruleSet)
                ?: return@withContext Result.failure(Exception("Unable to read the declarations from the package. Please capture clearer front and back images."))

            // Step 4: Evaluate Rules
            val engine = com.example.domain.validation.RuleEngine()
            val report = engine.evaluate(ruleSet, extraction)

            Result.success(Pair(report, extraction))

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Unable to analyze the product. Please check your internet connection and try again."))
        }
    }

    private suspend fun detectCategory(frontBase64: String, backBase64: String): String {
        val prompt = """
            Analyze these front and back images of a product package.
            Determine the product category. It MUST be exactly one of: FOOD, COSMETICS, MEDICINES, or UNKNOWN.
            Return ONLY a JSON object matching this schema:
            {
              "category": "FOOD", 
              "confidence": 0.95,
              "evidence": ["Looks like a packaged snack"]
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData("image/jpeg", frontBase64)),
                        Part(inlineData = InlineData("image/jpeg", backBase64))
                    )
                )
            ),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        val response = RetrofitClient.service.generateContent(BuildConfig.spare2_TRUscan_API_KEY, request)
        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return "UNKNOWN"
        
        return try {
            val cleanJson = text.replace("```json", "").replace("```", "").trim()
            val result = moshi.adapter(CategoryDetectionResult::class.java).fromJson(cleanJson)
            result?.category ?: "UNKNOWN"
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    private suspend fun extractDeclarations(frontBase64: String, backBase64: String, category: String, ruleSet: RuleSet): GeminiExtraction? {
        val fieldsToExtract = ruleSet.rules.map { it.field }.distinct()
        
        val prompt = """
            Analyze these front and back images of a $category product package.
            Extract the following fields if visible: ${fieldsToExtract.joinToString(", ")}.
            For EACH field, determine if it is present, its value, the source image (FRONT or BACK), and your confidence.
            If a field is not found, set present: false.
            If it is found but unreadable, set present: true, value: null, and status: "UNREADABLE".
            Do NOT invent missing declarations.
            
            Return ONLY a JSON object matching this schema:
            {
              "category": "$category",
              "category_confidence": 0.95,
              "fields": {
                "field_name": {
                  "present": true,
                  "value": "extracted text",
                  "source": "FRONT",
                  "confidence": 0.9,
                  "status": null
                }
              }
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData("image/jpeg", frontBase64)),
                        Part(inlineData = InlineData("image/jpeg", backBase64))
                    )
                )
            ),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        val response = RetrofitClient.service.generateContent(BuildConfig.spare2_TRUscan_API_KEY, request)
        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return null
        
        return try {
            val cleanJson = text.replace("```json", "").replace("```", "").trim()
            moshi.adapter(GeminiExtraction::class.java).fromJson(cleanJson)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close() ?: return null

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
            base64Image
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
