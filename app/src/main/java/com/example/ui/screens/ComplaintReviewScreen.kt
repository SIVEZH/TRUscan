package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.local.ComplaintEntity
import com.example.data.local.ScanEntity
import com.example.ui.components.FrostedBackground
import com.example.ui.theme.Blue600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.ComplaintViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintReviewScreen(
    scanId: String,
    viewModel: ComplaintViewModel,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var scanEntity by remember { mutableStateOf<ScanEntity?>(null) }
    
    LaunchedEffect(scanId) {
        coroutineScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                scanEntity = db.scanDao().getScanById(scanId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a").withZone(ZoneId.systemDefault())
    val scanDateString = try {
        scanEntity?.timestamp?.let { formatter.format(Instant.parse(it)) } ?: "Unknown date"
    } catch (e: Exception) {
        "Unknown date"
    }

    FrostedBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Review Complaint", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Slate900,
                        navigationIconContentColor = Slate900
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text("Complainant Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Name: ${viewModel.name.value}", color = Slate700)
                Text("Email: ${viewModel.email.value} ✓", color = Slate700)
                Text("Phone: ${viewModel.phone.value} ✓", color = Slate700)
                Text("Address: ${viewModel.address.value}", color = Slate700)
                Text("Live Photo: Verified ✓", color = Slate700)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Product Purchase Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Purchased at: ${viewModel.purchaseLocation.value}", color = Slate700)
                if (viewModel.purchaseDate.value.isNotBlank()) {
                    Text("Date: ${viewModel.purchaseDate.value}", color = Slate700)
                }
                Text("Receipt: ${if (viewModel.receiptUri.value != null) "Attached" else "Not Attached"}", color = Slate700)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Associated Scan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (scanEntity != null) {
                    Text("Product: ${scanEntity!!.productName ?: "Unknown"}", color = Slate700)
                    Text("Category: ${scanEntity!!.category}", color = Slate700)
                    Text("Status: ${scanEntity!!.overallStatus}", color = Slate700)
                    Text("Date: $scanDateString", color = Slate700)
                } else {
                    Text("Loading scan details...", color = Slate700)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Edit", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = {
                            scanEntity?.let { scan ->
                                coroutineScope.launch {
                                    try {
                                        val db = AppDatabase.getDatabase(context)
                                        val complaint = ComplaintEntity(
                                            complaintId = "TRU-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
                                            userId = scan.userId,
                                            sourceScanId = scan.scanId,
                                            category = scan.category,
                                            productName = scan.productName,
                                            reportJson = scan.reportJson,
                                            complainantName = viewModel.name.value,
                                            complainantEmail = viewModel.email.value,
                                            complainantPhone = viewModel.phone.value,
                                            complainantAddress = viewModel.address.value,
                                            purchaseLocation = viewModel.purchaseLocation.value,
                                            purchaseDate = viewModel.purchaseDate.value.takeIf { it.isNotBlank() },
                                            receiptUri = viewModel.receiptUri.value,
                                            status = "NEW",
                                            authorityId = null,
                                            authorityAction = null,
                                            rejectionReason = null,
                                            actionTimestamp = null,
                                            createdAt = Instant.now().toString()
                                        )
                                        db.complaintDao().insertComplaint(complaint)
                                        onContinueClick()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue600)
                    ) {
                        Text("Submit Complaint", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
