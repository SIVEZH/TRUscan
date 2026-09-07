package com.example.data.repository.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.data.remote.RetrofitClient
import com.example.data.remote.Content
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.InlineData
import com.example.data.remote.Part
import com.example.domain.model.ValidationReport
import com.example.util.ApiKeyProvider
import com.example.util.NetworkUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ProductAnalysisRepository(private val context: Context) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private suspend fun loadRuleSetJson(categoryName: String): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "rules/${categoryName.lowercase()}_rules.json"
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun analyzeProduct(
        images: List<com.example.domain.model.ProductImage>,
        category: com.example.domain.model.ProductCategory?
    ): Result<ValidationReport> = withContext(Dispatchers.IO) {
        try {
            if (ApiKeyProvider.getApiKey().isEmpty()) {
                return@withContext Result.failure(Exception("API key is not configured. Please add CHANGEABLE_API_KEY in the AI Studio Secrets panel."))
            }
            if (images.isEmpty()) {
                return@withContext Result.failure(Exception("Please provide at least one product image."))
            }
            if (category == null) {
                return@withContext Result.failure(Exception("Product category was not selected."))
            }
            val base64List = images.mapNotNull { uriToBase64(it.uri) }
            if (base64List.isEmpty()) {
                return@withContext Result.failure(Exception("Unable to process images."))
            }

            // Step 1: Load Rules JSON string directly
            val rulesJsonStr = loadRuleSetJson(category.name)
                ?: return@withContext Result.failure(Exception("Unable to load the validation rules for ${category.name}."))

            // Step 2: Evaluate Rules via Gemini
            val report = evaluateWithGemini(base64List, category.name, rulesJsonStr)
                ?: return@withContext Result.failure(Exception("Unable to read the declarations from the package. Please capture clearer images of all labels."))

            Result.success(report)
        } catch (e: Exception) {
            e.printStackTrace()
            val specificError = NetworkUtils.parseApiError(e, context)
            Result.failure(Exception(specificError))
        }
    }

    private suspend fun evaluateWithGemini(base64Images: List<String>, categoryName: String, rulesJson: String): ValidationReport? {
        val prompt = """
            You are an expert compliance auditor for $categoryName products.
            Analyze these ${base64Images.size} images of a product package. You must process all images together as different views of the SAME package.
            
            Based on the following JSON ruleset, evaluate the compliance of this product.
            Determine if the product is pre-packaged, retail/wholesale, imported, etc., to figure out which rules are applicable.
            For each rule in the ruleset, evaluate if it PASSES, FAILS, or is NOT_APPLICABLE/NOT_MANDATORY based on its criteria and what is visible in the images.
            Only report a FAIL if a MANDATORY and APPLICABLE rule is not satisfied.

            Ruleset:
            $rulesJson

            Return ONLY a JSON object matching this schema exactly (adapt the exact fields to the existing Android data model if necessary, but preserve the semantics):
            {
              "category": "$categoryName",
              "overall_status": "COMPLIANT" | "NON_COMPLIANT" | "MANUAL_REVIEW",
              "summary": {
                "total_rules": 10,
                "passed": 8,
                "failed": 0,
                "not_applicable": 1,
                "manual_review": 1,
                "not_mandatory": 0,
                "not_required": 0,
                "total_violations": 0,
                "excluded_rules": 0
              },
              "results": [
                {
                  "rule_id": "FOOD_LMPC_6_1_A",
                  "legal_reference": "Rule 6(1)(a)",
                  "field": "manufacturer_packer_importer_name_address",
                  "status": "PASS" | "FAIL" | "NOT_MANDATORY" | "NOT_REQUIRED" | "NOT_APPLICABLE" | "MANUAL_REVIEW",
                  "extracted_value": "Acme Corp...",
                  "message": "Found manufacturer details",
                  "requirement": "Name and address of...",
                  "mandatory": true,
                  "applicability": "APPLICABLE" | "NOT_APPLICABLE",
                  "violation": false,
                  "evidence": { "value": "Acme Corp..." }
                }
              ]
            }
        """.trimIndent()
        val parts = mutableListOf<Part>()
        parts.add(Part(text = prompt))
        base64Images.forEach { base64 ->
            parts.add(Part(inlineData = InlineData("image/jpeg", base64)))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        val response = RetrofitClient.service.generateContent(ApiKeyProvider.getApiKey(), request)
        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return null
        
        return try {
            val cleanJson = text.replace("```json", "").replace("```", "").trim()
            moshi.adapter(ValidationReport::class.java).fromJson(cleanJson)
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
