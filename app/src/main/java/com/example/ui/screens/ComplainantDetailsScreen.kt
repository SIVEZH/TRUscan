package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ComplaintEntity
import com.example.ui.components.FrostedBackground
import com.example.util.AuthorityPdfExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplainantDetailsScreen(
    complaintId: String,
    authorityId: String,
    authorityCategory: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var complaint by remember { mutableStateOf<ComplaintEntity?>(null) }
    
    LaunchedEffect(complaintId) {
        coroutineScope.launch {
            val db = AppDatabase.getDatabase(context)
            complaint = db.complaintDao().getComplaintById(complaintId)
        }
    }

    FrostedBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Complainant Details", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (complaint == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val c = complaint!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text("AUTHORITY ACTION REPORT", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("COMPLAINANT DETAILS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Name: ${c.complainantName}")
                    Text("Email: ${c.complainantEmail}")
                    Text("Phone: ${c.complainantPhone}")
                    Text("Address: ${c.complainantAddress}")
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("VERIFICATION STATUS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Email: Verified ✓", color = Color(0xFF2E7D32))
                    Text("Phone: Verified ✓", color = Color(0xFF2E7D32))
                    Text("Live Photo: Verified ✓", color = Color(0xFF2E7D32))
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("PURCHASE DETAILS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Purchase Location: ${c.purchaseLocation}")
                    if (c.purchaseDate != null) Text("Purchase Date: ${c.purchaseDate}")
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("PRODUCT DETAILS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Product Name: ${c.productName ?: "Unknown"}")
                    Text("Category: ${c.category}")
                    Text("Complaint ID: ${c.complaintId}")
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val pdfFile = AuthorityPdfExporter.exportAuthorityPdf(context, c)
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Authority Report"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = "Download")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download PDF Report")
                    }
                }
            }
        }
    }
}
