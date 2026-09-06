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
import com.example.domain.model.RuleStatus
import com.example.domain.model.ValidationReport
import com.example.ui.components.FrostedBackground
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorityComplaintDetailScreen(
    complaintId: String,
    authorityId: String,
    authorityCategory: String,
    onBackClick: () -> Unit,
    onNavigateToComplainant: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var complaint by remember { mutableStateOf<ComplaintEntity?>(null) }
    var report by remember { mutableStateOf<ValidationReport?>(null) }
    
    var showViolationDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var actionAgainstUser by remember { mutableStateOf(false) }
    var showTakeActionConfirmation by remember { mutableStateOf(false) }

    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val reportAdapter = moshi.adapter(ValidationReport::class.java)

    LaunchedEffect(complaintId) {
        coroutineScope.launch {
            val db = AppDatabase.getDatabase(context)
            val c = db.complaintDao().getComplaintById(complaintId)
            complaint = c
            if (c != null) {
                try {
                    report = reportAdapter.fromJson(c.reportJson)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    FrostedBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Complaint Details", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (complaint == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
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
                    Text("Complaint Information", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Complaint ID: ${c.complaintId}")
                    Text("Date: ${c.createdAt}")
                    Text("Status: ${c.status}", fontWeight = FontWeight.Bold)
                    Text("Source Scan ID: ${c.sourceScanId}")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Product Information", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Product Name: ${c.productName ?: "Unknown"}")
                    Text("Category: ${c.category}")
                    Text("Purchase Location: ${c.purchaseLocation}")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Compliance Report", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (report != null) {
                        val currentReport = report!!
                        Text("Overall Result:", fontWeight = FontWeight.Bold)
                        val statusColor = when (currentReport.overall_status.name) {
                            "COMPLIANT" -> Color(0xFF2E7D32)
                            "NON_COMPLIANT" -> Color(0xFFC62828)
                            else -> Color(0xFFEF6C00)
                        }
                        Text(currentReport.overall_status.name.replace("_", " "), color = statusColor, fontWeight = FontWeight.Bold)
                        
                        val violations = currentReport.violations
                        if (violations.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Confirmed Mandatory Violations (${violations.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFC62828))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            violations.forEach { rule ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Rule: ${rule.rule_id} (${rule.legal_reference})", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                        Text("Field: ${rule.field}", fontSize = 14.sp)
                                        Text("Extracted: ${rule.extracted_value ?: "Not found"}", fontSize = 14.sp)
                                        Text("Requirement: ${rule.requirement}", fontSize = 13.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Violation: Mandatory declaration missing / non-compliant", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                        Text("Details: ${rule.message}", color = Color(0xFFC62828), fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("All Evaluated Rules (${currentReport.results.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        currentReport.results.forEach { rule ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val ruleTag = if (rule.mandatory) "MANDATORY" else "OPTIONAL"
                                    Text("Rule: ${rule.rule_id} • $ruleTag", fontWeight = FontWeight.Bold)
                                    Text("Field: ${rule.field}", fontSize = 14.sp)
                                    Text("Extracted: ${rule.extracted_value ?: "N/A"}", fontSize = 14.sp)
                                    Text("Required: ${rule.requirement}", fontSize = 13.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    val itemColor = when (rule.status) {
                                        RuleStatus.PASS -> Color(0xFF2E7D32)
                                        RuleStatus.FAIL -> Color(0xFFC62828)
                                        RuleStatus.MANUAL_REVIEW -> Color(0xFFEF6C00)
                                        else -> Color.Gray
                                    }
                                    Text("Status: ${rule.status.name}", fontWeight = FontWeight.Bold, color = itemColor)
                                    if (rule.status != RuleStatus.PASS) {
                                        Text("Explanation: ${rule.message}", color = itemColor, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Report data unavailable.", color = Color.Gray)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    if (c.status == "NEW" || c.status == "UNDER_REVIEW") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            OutlinedButton(onClick = { showRejectDialog = true }, modifier = Modifier.weight(1f).height(50.dp)) {
                                Text("Reject Complaint")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(onClick = { showViolationDialog = true }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))) {
                                Text("Add to Violation List", color = Color.White)
                            }
                        }
                    } else if (c.status == "REJECTED_ACTION_REQUIRED") {
                        Button(onClick = { onNavigateToComplainant(c.complaintId) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                            Text("View Complainant Details")
                        }
                    }
                }
                
                // Dialogs
                if (showViolationDialog) {
                    AlertDialog(
                        onDismissRequest = { showViolationDialog = false },
                        title = { Text("Add this complaint to the violation list?") },
                        text = { Text("This will mark the complaint as an accepted violation.") },
                        confirmButton = {
                            Button(onClick = {
                                coroutineScope.launch {
                                    val db = AppDatabase.getDatabase(context)
                                    db.complaintDao().updateStatus(c.complaintId, "ADDED_TO_VIOLATION_LIST")
                                    complaint = c.copy(status = "ADDED_TO_VIOLATION_LIST")
                                    showViolationDialog = false
                                }
                            }) { Text("Confirm") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showViolationDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                if (showRejectDialog) {
                    AlertDialog(
                        onDismissRequest = { showRejectDialog = false },
                        title = { Text("Reject Complaint") },
                        text = {
                            Column {
                                Text("Provide a reason for rejection:")
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = rejectReason,
                                    onValueChange = { rejectReason = it },
                                    label = { Text("Reason") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = actionAgainstUser,
                                        onCheckedChange = { actionAgainstUser = it }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Action against user (Flag complaint)", fontSize = 14.sp)
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (actionAgainstUser) {
                                    showRejectDialog = false
                                    showTakeActionConfirmation = true
                                } else {
                                    coroutineScope.launch {
                                        val db = AppDatabase.getDatabase(context)
                                        val now = Instant.now().toString()
                                        db.complaintDao().updateRejection(c.complaintId, "REJECTED", rejectReason, "REJECT_NO_ACTION", now)
                                        complaint = c.copy(status = "REJECTED", rejectionReason = rejectReason, authorityAction = "REJECT_NO_ACTION", actionTimestamp = now)
                                        showRejectDialog = false
                                    }
                                }
                            }) { Text("Reject") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRejectDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                if (showTakeActionConfirmation) {
                    AlertDialog(
                        onDismissRequest = { showTakeActionConfirmation = false },
                        title = { Text("Confirm Action Against User") },
                        text = { Text("Are you sure you want to flag this user and take legal action for fraudulent submission?") },
                        confirmButton = {
                            Button(onClick = {
                                coroutineScope.launch {
                                    val db = AppDatabase.getDatabase(context)
                                    val now = Instant.now().toString()
                                    db.complaintDao().updateRejection(c.complaintId, "REJECTED_ACTION_REQUIRED", rejectReason, "REJECT_TAKE_ACTION", now)
                                    complaint = c.copy(status = "REJECTED_ACTION_REQUIRED", rejectionReason = rejectReason, authorityAction = "REJECT_TAKE_ACTION", actionTimestamp = now)
                                    showTakeActionConfirmation = false
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))) { Text("Confirm & Flag User") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTakeActionConfirmation = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }
}
