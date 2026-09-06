package com.example.util

import android.content.Context
import android.net.Uri
import com.example.domain.model.ValidationReport
import java.io.File

object ReportExporter {

    suspend fun exportPdf(
        context: Context,
        report: ValidationReport,
        scanId: String,
        timestamp: String,
        productName: String? = null,
        imageUris: List<String> = emptyList()
    ): File {
        return PdfReportGenerator.generate(context, report, scanId, timestamp, productName, imageUris)
    }

    suspend fun exportDocx(
        context: Context,
        report: ValidationReport,
        scanId: String,
        timestamp: String,
        productName: String? = null,
        imageUris: List<String> = emptyList()
    ): File {
        return DocxReportGenerator.generate(context, report, scanId, timestamp, productName, imageUris)
    }

    suspend fun savePdfToDownloads(
        context: Context,
        report: ValidationReport,
        scanId: String,
        timestamp: String,
        productName: String? = null,
        imageUris: List<String> = emptyList()
    ): Pair<File, Uri?> {
        val file = exportPdf(context, report, scanId, timestamp, productName, imageUris)
        val cleanScanId = scanId.take(8).replace(Regex("[^a-zA-Z0-9]"), "_")
        val cleanDate = timestamp.take(10).replace(Regex("[^a-zA-Z0-9]"), "-")
        val fileName = "TRUscan_Report_${cleanScanId}_${cleanDate}.pdf"
        val uri = ReportFileSaver.saveToDownloads(context, file, fileName, "application/pdf")
        return Pair(file, uri)
    }

    suspend fun saveDocxToDownloads(
        context: Context,
        report: ValidationReport,
        scanId: String,
        timestamp: String,
        productName: String? = null,
        imageUris: List<String> = emptyList()
    ): Pair<File, Uri?> {
        val file = exportDocx(context, report, scanId, timestamp, productName, imageUris)
        val cleanScanId = scanId.take(8).replace(Regex("[^a-zA-Z0-9]"), "_")
        val cleanDate = timestamp.take(10).replace(Regex("[^a-zA-Z0-9]"), "-")
        val fileName = "TRUscan_Report_${cleanScanId}_${cleanDate}.docx"
        val uri = ReportFileSaver.saveToDownloads(context, file, fileName, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        return Pair(file, uri)
    }
}
