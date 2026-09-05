package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.domain.model.ValidationReport
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.util.ReportExporter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReportExportSection(
    report: ValidationReport,
    scanId: String,
    timestamp: String = Instant.now().toString()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    val formattedDate = try {
        DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault()).format(Instant.parse(timestamp))
    } catch (e: Exception) {
        "Unknown Date"
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    // It's tricky to pass the file content directly here since we don't know which format was picked.
                    // A better pattern is to generate the temp file, then when the user picks the dest URI, we copy it.
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Since we need to know the format when calling CreateDocument, it's better to just use standard intents for sharing,
    // or let the user choose the format which then triggers the launcher.
    // Actually, generating a temp file and sharing it via ACTION_SEND is often easiest and allows "Save to Drive" or "Files" app.
    
    fun shareFile(file: java.io.File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Report"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share report", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Export Report", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
        Spacer(modifier = Modifier.height(16.dp))

        if (isExporting) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(
                onClick = {
                    if (isExporting) return@OutlinedButton
                    isExporting = true
                    coroutineScope.launch {
                        try {
                            val file = ReportExporter.exportPdf(context, report, scanId, formattedDate)
                            shareFile(file, "application/pdf")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                        } finally {
                            isExporting = false
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("PDF", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    if (isExporting) return@OutlinedButton
                    isExporting = true
                    coroutineScope.launch {
                        try {
                            val file = ReportExporter.exportDocx(context, report, scanId, formattedDate)
                            shareFile(file, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to generate DOCX", Toast.LENGTH_SHORT).show()
                        } finally {
                            isExporting = false
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("DOCX", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    if (isExporting) return@OutlinedButton
                    isExporting = true
                    coroutineScope.launch {
                        try {
                            val file = ReportExporter.exportJson(context, report, scanId)
                            shareFile(file, "application/json")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to generate JSON", Toast.LENGTH_SHORT).show()
                        } finally {
                            isExporting = false
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("JSON", fontWeight = FontWeight.Bold)
            }
        }
    }
}
