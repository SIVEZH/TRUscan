package com.example.ui.analysis

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.OverallStatus
import com.example.domain.model.RuleResult
import com.example.domain.model.RuleStatus
import com.example.domain.model.ValidationReport
import com.example.ui.components.FrostedBackground
import com.example.ui.theme.Blue600
import com.example.ui.theme.Blue700
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.GlassBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidationScreen(
    frontUri: Uri,
    backUri: Uri,
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onRaiseComplaint: (String) -> Unit,
    viewModel: ValidationScreenViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    
    var selectedRule by remember { mutableStateOf<RuleResult?>(null) }

    LaunchedEffect(Unit) {
        if (state is AnalysisState.Idle) {
            viewModel.analyzeProduct(frontUri, backUri, userId)
        }
    }

    FrostedBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Validation Report", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                when (val currentState = state) {
                    is AnalysisState.Idle, is AnalysisState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Blue600)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Analyzing Product...",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate700
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Reading declarations and evaluating rules.",
                                fontSize = 14.sp,
                                color = Slate400
                            )
                        }
                    }
                    is AnalysisState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Error, contentDescription = "Error", tint = Color(0xFFC62828), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                currentState.message,
                                fontSize = 16.sp,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.analyzeProduct(frontUri, backUri, userId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue600)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                    is AnalysisState.Success -> {
                        val report = currentState.report
                        
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
                                Spacer(modifier = Modifier.height(32.dp))
                                if (report.overall_status != OverallStatus.COMPLIANT) {
                                    Button(
                                        onClick = { onRaiseComplaint((state as AnalysisState.Success).scanId) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Blue600),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Blue600)
                                    ) {
                                        Text("Raise a Complaint", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                com.example.ui.components.ReportExportSection(
                                    report = report,
                                    scanId = (state as AnalysisState.Success).scanId
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                OutlinedButton(
                                    onClick = onNavigateHome,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                }
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
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

@Composable
fun ReportSummaryHeader(report: ValidationReport) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Category: ${report.category}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Slate400
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val (statusText, statusColor, statusIcon) = when (report.overall_status) {
                OverallStatus.COMPLIANT -> Triple("COMPLIANT", Color(0xFF2E7D32), Icons.Filled.CheckCircle)
                OverallStatus.NON_COMPLIANT -> Triple("NON-COMPLIANT", Color(0xFFC62828), Icons.Filled.Error)
                OverallStatus.MANUAL_REVIEW -> Triple("MANUAL REVIEW", Color(0xFFEF6C00), Icons.Filled.Warning)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(statusIcon, contentDescription = statusText, tint = statusColor, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    statusText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("${report.summary.total_rules} Rules Checked", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("✓ ${report.summary.passed} Passed", color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                    Text("✗ ${report.summary.failed} Failed", color = Color(0xFFC62828), fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("— ${report.summary.not_applicable} N/A", color = Slate400, fontWeight = FontWeight.Medium)
                    Text("⚠ ${report.summary.manual_review} Review", color = Color(0xFFEF6C00), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun RuleCard(rule: RuleResult, onClick: () -> Unit) {
    val (statusColor, statusIcon, bgColor) = when (rule.status) {
        RuleStatus.PASS -> Triple(Color(0xFF2E7D32), Icons.Filled.CheckCircle, Color(0xFFE8F5E9))
        RuleStatus.FAIL -> Triple(Color(0xFFC62828), Icons.Filled.Error, Color(0xFFFFEBEE))
        RuleStatus.NOT_APPLICABLE -> Triple(Slate400, Icons.Filled.Info, Color(0xFFF1F5F9))
        RuleStatus.MANUAL_REVIEW -> Triple(Color(0xFFEF6C00), Icons.Filled.Warning, Color(0xFFFFF3E0))
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Slate400),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(statusIcon, contentDescription = rule.status.name, tint = statusColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    rule.field.replace("_", " ").capitalize(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    rule.legal_reference,
                    fontSize = 12.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                if (rule.status == RuleStatus.PASS || rule.status == RuleStatus.MANUAL_REVIEW || rule.status == RuleStatus.FAIL) {
                    Text(
                        "Detected: ${rule.extracted_value ?: "Not found"}",
                        fontSize = 14.sp,
                        color = Slate700
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleDetailsDialog(rule: RuleResult, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            val (statusText, statusColor) = when (rule.status) {
                RuleStatus.PASS -> "PASS" to Color(0xFF2E7D32)
                RuleStatus.FAIL -> "FAIL" to Color(0xFFC62828)
                RuleStatus.NOT_APPLICABLE -> "NOT APPLICABLE" to Slate400
                RuleStatus.MANUAL_REVIEW -> "MANUAL REVIEW" to Color(0xFFEF6C00)
            }
            
            Text(rule.legal_reference, fontSize = 14.sp, color = Slate400, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(rule.field.replace("_", " ").capitalize(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Slate900)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("STATUS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400)
            Text(statusText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = statusColor)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("DETECTED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400)
            Text(rule.extracted_value ?: "None", fontSize = 16.sp, color = Slate900)
            
            if (rule.source_image != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("SOURCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400)
                Text(rule.source_image, fontSize = 16.sp, color = Slate900)
            }
            
            if (rule.confidence != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("CONFIDENCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400)
                Text("${(rule.confidence * 100).toInt()}%", fontSize = 16.sp, color = Slate900)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("REQUIREMENT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400)
            Text(rule.requirement, fontSize = 14.sp, color = Slate700)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("EXPLANATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400)
            Text(rule.message, fontSize = 14.sp, color = Slate700)
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
