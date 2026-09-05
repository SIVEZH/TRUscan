package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.RuleResult
import com.example.domain.model.ValidationReport
import com.example.ui.analysis.ReportSummaryHeader
import com.example.ui.analysis.RuleCard
import com.example.ui.analysis.RuleDetailsDialog
import com.example.ui.components.FrostedBackground
import com.example.ui.theme.Slate900
import com.example.data.local.AppDatabase
import com.example.data.local.ComplaintEntity
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    report: ValidationReport,
    scanId: String,
    timestamp: String,
    onBackClick: () -> Unit
) {
    var selectedRule by remember { mutableStateOf<RuleResult?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var complaint by remember { mutableStateOf<ComplaintEntity?>(null) }

    LaunchedEffect(scanId) {
        coroutineScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                complaint = db.complaintDao().getComplaintByScanId(scanId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    FrostedBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Compliance Report", fontWeight = FontWeight.Bold) },
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
                    .padding(horizontal = 24.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        ReportSummaryHeader(report)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Detailed Rules", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    items(report.results) { rule ->
                        RuleCard(rule) { selectedRule = rule }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (complaint != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Complaint Status", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Status: ${complaint!!.status}", fontWeight = FontWeight.Bold)
                                    
                                    if (complaint!!.status == "ADDED_TO_VIOLATION_LIST") {
                                        Text("This complaint was added to the violation list.", color = Color(0xFFC62828))
                                    } else if (complaint!!.status == "REJECTED" || complaint!!.status == "REJECTED_ACTION_REQUIRED") {
                                        Text("This complaint was rejected.", color = Color(0xFFC62828))
                                        if (complaint!!.rejectionReason != null) {
                                            Text("Reason: ${complaint!!.rejectionReason}", fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        com.example.ui.components.ReportExportSection(
                            report = report,
                            scanId = scanId,
                            timestamp = timestamp
                        )
                    }
                }
            }
        }
        
        if (selectedRule != null) {
            RuleDetailsDialog(
                rule = selectedRule!!,
                onDismiss = { selectedRule = null }
            )
        }
    }
}
