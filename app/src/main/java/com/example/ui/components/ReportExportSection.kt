package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.domain.model.ValidationReport
import com.example.ui.theme.*
import com.example.util.DocxReportGenerator
import com.example.util.PdfReportGenerator
import com.example.util.ReportFileSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed class ExportState {
    object Idle : ExportState()
    data class Generating(val message: String) : ExportState()
    data class Success(val message: String, val file: File, val uri: Uri?, val mimeType: String, val format: String) : ExportState()
    data class Error(val message: String) : ExportState()
}

@Composable
fun ReportExportSection(
    report: ValidationReport,
    scanId: String,
    timestamp: String = Instant.now().toString(),
    productName: String? = null,
    imageUris: List<String> = emptyList()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var exportState by remember { mutableStateOf<ExportState>(ExportState.Idle) }

    // Last generated file reference for immediate sharing
    var lastGeneratedFile by remember { mutableStateOf<File?>(null) }
    var lastGeneratedMimeType by remember { mutableStateOf<String?>("application/pdf") }

    val formattedDate = remember(timestamp) {
        try {
            DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a")
                .withZone(ZoneId.systemDefault())
                .format(Instant.parse(timestamp))
        } catch (e: Exception) {
            timestamp.take(19)
        }
    }

    // Resolves image URIs and product name from database if not supplied directly
    suspend fun resolveScanData(): Pair<String?, List<String>> = withContext(Dispatchers.IO) {
        var resolvedImages = imageUris.filter { it.isNotBlank() }
        var resolvedName = productName

        if (resolvedImages.isEmpty() || resolvedName == null) {
            try {
                val db = AppDatabase.getDatabase(context)
                val scan = db.scanDao().getScanById(scanId)
                if (scan != null) {
                    if (resolvedName == null) {
                        resolvedName = scan.productName
                    }
                    if (resolvedImages.isEmpty()) {
                        val list = mutableListOf<String>()
                        if (scan.frontImageUri.isNotBlank()) list.add(scan.frontImageUri)
                        if (scan.backImageUri.isNotBlank()) list.add(scan.backImageUri)
                        resolvedImages = list.distinct()
                    }
                }
            } catch (e: Exception) {
                // Ignore DB error, proceed with provided parameters
            }
        }
        Pair(resolvedName, resolvedImages)
    }

    // Storage Access Framework (SAF) CreateDocument launcher for custom destination
    var pendingExportType by remember { mutableStateOf<String?>(null) }
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri == null) {
            exportState = ExportState.Error("Save location cancelled by user.")
            return@rememberLauncherForActivityResult
        }
        val type = pendingExportType ?: "pdf"
        val mime = if (type == "docx") {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        } else {
            "application/pdf"
        }
        exportState = ExportState.Generating(if (type == "docx") "Generating DOCX..." else "Generating PDF...")

        coroutineScope.launch {
            try {
                val (resolvedName, resolvedImgs) = resolveScanData()
                val file = if (type == "docx") {
                    DocxReportGenerator.generate(context, report, scanId, formattedDate, resolvedName, resolvedImgs)
                } else {
                    PdfReportGenerator.generate(context, report, scanId, formattedDate, resolvedName, resolvedImgs)
                }
                lastGeneratedFile = file
                lastGeneratedMimeType = mime

                val saved = ReportFileSaver.saveToUri(context, file, uri)
                if (saved) {
                    val label = if (type == "docx") "DOCX" else "PDF"
                    exportState = ExportState.Success(
                        message = "$label saved successfully.",
                        file = file,
                        uri = uri,
                        mimeType = mime,
                        format = label
                    )
                } else {
                    exportState = ExportState.Error("Failed to save report to chosen location.")
                }
            } catch (e: Exception) {
                exportState = ExportState.Error("Unable to save report. Please try again.")
            }
        }
    }

    fun handleDownloadPdf() {
        if (exportState is ExportState.Generating) return
        exportState = ExportState.Generating("Generating PDF...")

        coroutineScope.launch {
            try {
                val (resolvedName, resolvedImgs) = resolveScanData()
                val file = try {
                    PdfReportGenerator.generate(
                        context = context,
                        report = report,
                        scanId = scanId,
                        timestamp = formattedDate,
                        productName = resolvedName,
                        imageUris = resolvedImgs
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    exportState = ExportState.Error("Unable to generate the PDF. Please try again.")
                    return@launch
                }

                lastGeneratedFile = file
                lastGeneratedMimeType = "application/pdf"

                val cleanScanId = scanId.take(8).replace(Regex("[^a-zA-Z0-9]"), "_")
                val cleanDate = formattedDate.take(11).replace(Regex("[^a-zA-Z0-9]"), "-").trim('-')
                val fileName = "TRUscan_Report_${cleanScanId}_${cleanDate}.pdf"

                val savedUri = ReportFileSaver.saveToDownloads(
                    context = context,
                    sourceFile = file,
                    displayName = fileName,
                    mimeType = "application/pdf"
                )

                if (savedUri != null) {
                    exportState = ExportState.Success(
                        message = "PDF saved successfully.",
                        file = file,
                        uri = savedUri,
                        mimeType = "application/pdf",
                        format = "PDF"
                    )
                } else {
                    // Fallback to SAF picker if direct MediaStore save failed
                    pendingExportType = "pdf"
                    createDocLauncher.launch(fileName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                exportState = ExportState.Error("Unable to generate the PDF. Please try again.")
            }
        }
    }

    fun handleDownloadDocx() {
        if (exportState is ExportState.Generating) return
        exportState = ExportState.Generating("Generating DOCX...")

        coroutineScope.launch {
            try {
                val (resolvedName, resolvedImgs) = resolveScanData()
                val file = try {
                    DocxReportGenerator.generate(
                        context = context,
                        report = report,
                        scanId = scanId,
                        timestamp = formattedDate,
                        productName = resolvedName,
                        imageUris = resolvedImgs
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    exportState = ExportState.Error("Unable to generate the DOCX. Please try again.")
                    return@launch
                }

                val mime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                lastGeneratedFile = file
                lastGeneratedMimeType = mime

                val cleanScanId = scanId.take(8).replace(Regex("[^a-zA-Z0-9]"), "_")
                val cleanDate = formattedDate.take(11).replace(Regex("[^a-zA-Z0-9]"), "-").trim('-')
                val fileName = "TRUscan_Report_${cleanScanId}_${cleanDate}.docx"

                val savedUri = ReportFileSaver.saveToDownloads(
                    context = context,
                    sourceFile = file,
                    displayName = fileName,
                    mimeType = mime
                )

                if (savedUri != null) {
                    exportState = ExportState.Success(
                        message = "DOCX saved successfully.",
                        file = file,
                        uri = savedUri,
                        mimeType = mime,
                        format = "DOCX"
                    )
                } else {
                    // Fallback to SAF picker if direct MediaStore save failed
                    pendingExportType = "docx"
                    createDocLauncher.launch(fileName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                exportState = ExportState.Error("Unable to generate the DOCX. Please try again.")
            }
        }
    }

    fun handleShare() {
        coroutineScope.launch {
            try {
                val targetFile = lastGeneratedFile ?: run {
                    exportState = ExportState.Generating("Generating PDF for sharing...")
                    val (resolvedName, resolvedImgs) = resolveScanData()
                    val f = PdfReportGenerator.generate(
                        context = context,
                        report = report,
                        scanId = scanId,
                        timestamp = formattedDate,
                        productName = resolvedName,
                        imageUris = resolvedImgs
                    )
                    lastGeneratedFile = f
                    lastGeneratedMimeType = "application/pdf"
                    exportState = ExportState.Idle
                    f
                }
                val mime = lastGeneratedMimeType ?: "application/pdf"
                ReportFileSaver.shareFile(
                    context = context,
                    file = targetFile,
                    mimeType = mime,
                    title = "TRUscan Compliance Report ($scanId)"
                )
            } catch (e: Exception) {
                exportState = ExportState.Error("Unable to share report. Please try again.")
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Export Report",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Download or share the compliance inspection record",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // State Feedback (Generating / Success / Error)
            when (val state = exportState) {
                is ExportState.Generating -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = state.message,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                is ExportState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F5E9))
                            .border(1.dp, Color(0xFFA5D6A7), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.message,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Saved to device storage. Ready to view or inspect.",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                if (state.uri != null) {
                                    val opened = ReportFileSaver.openFile(context, state.uri, state.mimeType)
                                    if (!opened) {
                                        Toast.makeText(context, "No app found to open ${state.format} file", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1B5E20))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open ${state.format}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                is ExportState.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFEBEE))
                            .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.message,
                            fontSize = 13.sp,
                            color = Color(0xFFB71C1C)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                ExportState.Idle -> { /* Idle */ }
            }

            // The exact requested UI buttons:
            // [ Download PDF ]
            // [ Download DOCX ]
            // [ Share ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { handleDownloadPdf() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = exportState !is ExportState.Generating
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Download PDF",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { handleDownloadDocx() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    enabled = exportState !is ExportState.Generating
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Download DOCX",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { handleShare() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = exportState !is ExportState.Generating
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
