package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.ScanEntity
import com.example.domain.model.ValidationReport
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

class ScanStorageRepository(private val context: Context) {
    private val scanDao = AppDatabase.getDatabase(context).scanDao()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    suspend fun saveScan(
        userId: String,
        frontUri: String,
        backUri: String,
        report: ValidationReport
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val reportJson = moshi.adapter(ValidationReport::class.java).toJson(report)
            
            val productName = report.results.find { it.field == "common_or_generic_name" }?.extracted_value
            
            val scanId = UUID.randomUUID().toString()
            val entity = ScanEntity(
                scanId = scanId,
                userId = userId,
                timestamp = Instant.now().toString(),
                category = report.category,
                productName = productName,
                frontImageUri = frontUri,
                backImageUri = backUri,
                overallStatus = report.overall_status.name,
                reportJson = reportJson
            )
            
            scanDao.insertScan(entity)
            Result.success(scanId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getScans(userId: String): Flow<List<ScanEntity>> {
        return scanDao.getScansForUser(userId)
    }
}
