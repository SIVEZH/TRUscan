package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.domain.model.Applicability
import com.example.domain.model.OverallStatus
import com.example.domain.model.RuleResult
import com.example.domain.model.RuleStatus
import com.example.domain.model.ValidationReport
import com.example.ui.analysis.ReportSummaryHeader
import com.example.ui.analysis.RuleCard
import com.example.ui.analysis.RuleDetailsDialog
import com.example.ui.components.FrostedBackground
import com.example.ui.theme.Blue600
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
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
    onBackClick: () -> Unit,
    onRaiseComplaint: ((String) -> Unit)? = null
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
                val violations = report.results.filter { it.status == RuleStatus.FAIL && it.mandatory && it.applicable }
                val manualReviews = report.results.filter { it.status == RuleStatus.MANUAL_REVIEW }
                val notMandatoryRules = report.results.filter { it.status == RuleStatus.NOT_MANDATORY }
                val notRequiredRules = report.results.filter { it.status == RuleStatus.NOT_REQUIRED }
                val compliantRules = report.results.filter { it.status == RuleStatus.PASS }
                val notApplicableRules = report.results.filter { it.status == RuleStatus.NOT_APPLICABLE }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        ReportSummaryHeader(report)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 1. MANDATORY VIOLATIONS
                    if (violations.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.Error,
                                    contentDescription = "Mandatory Violations",
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Mandatory Violations (${violations.size})",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "The following legally mandatory declarations are missing or non-compliant:",
                                fontSize = 13.sp,
                                color = Slate400
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        items(violations) { rule ->
                            RuleCard(rule) { selectedRule = rule }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // 2. MANUAL REVIEW
                    if (manualReviews.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = "Manual Review",
                                    tint = Color(0xFFEF6C00),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Manual Review (${manualReviews.size})",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF6C00)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Mandatory declarations requiring manual inspection or verification:",
                                fontSize = 13.sp,
                                color = Slate400
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        items(manualReviews) { rule ->
                            RuleCard(rule) { selectedRule = rule }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // 3. NOT MANDATORY
                    if (notMandatoryRules.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Not Mandatory",
                                    tint = Slate700,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Not Mandatory (${notMandatoryRules.size})",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Declarations not mandatory for this product category:",
                                fontSize = 13.sp,
                                color = Slate400
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        items(notMandatoryRules) { rule ->
                            RuleCard(rule) { selectedRule = rule }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // 4. NOT REQUIRED
                    if (notRequiredRules.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Not Required",
                                    tint = Slate700,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Not Required (${notRequiredRules.size})",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Declarations not required under current packaging conditions:",
                                fontSize = 13.sp,
                                color = Slate400
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        items(notRequiredRules) { rule ->
                            RuleCard(rule) { selectedRule = rule }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // 5. COMPLIANT REQUIREMENTS
                    if (compliantRules.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Compliant Requirements",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Compliant Requirements (${compliantRules.size})",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Mandatory declarations verified and compliant with regulations:",
                                fontSize = 13.sp,
                                color = Slate400
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        items(compliantRules) { rule ->
                            RuleCard(rule) { selectedRule = rule }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // 5. NOT APPLICABLE
                    if (notApplicableRules.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = "Not Applicable",
                                    tint = Slate400,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Not Applicable (${notApplicableRules.size})",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate400
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Rules not applicable to this product category:",
                                fontSize = 13.sp,
                                color = Slate400
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        items(notApplicableRules) { rule ->
                            RuleCard(rule) { selectedRule = rule }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))

                        if (complaint != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Complaint Filed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Reference: ${complaint!!.complaintId}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Status: ${complaint!!.status}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

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
                        } else if (report.canRaiseComplaint && onRaiseComplaint != null) {
                            Button(
                                onClick = { onRaiseComplaint(scanId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue600)
                            ) {
                                Text("Raise a Complaint", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
