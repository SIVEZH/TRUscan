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
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.GlassBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidationScreen(
    images: List<com.example.domain.model.ProductImage>,
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
            viewModel.analyzeProduct(images, userId)
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
                                "Reading declarations and evaluating legal rules.",
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
                                onClick = { viewModel.analyzeProduct(images, userId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue600)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                    is AnalysisState.Success -> {
                        val report = currentState.report
                        val violations = report.violations
                        val otherRules = report.results.filter { !it.violation }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                ReportSummaryHeader(report)
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            // 1. PRIORITIZED VIOLATIONS SECTION (Only Confirmed Mandatory Failures)
                            if (violations.isNotEmpty()) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Filled.Error,
                                            contentDescription = "Violations",
                                            tint = Color(0xFFC62828),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Confirmed Violations (${violations.size})",
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

                            // 2. OTHER DECLARATIONS & EVALUATIONS
                            item {
                                Text(
                                    "All Evaluated Declarations (${report.results.size})",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Includes compliant mandatory fields, manual reviews, and optional declarations.",
                                    fontSize = 13.sp,
                                    color = Slate400
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            items(if (violations.isEmpty()) report.results else otherRules) { rule ->
                                RuleCard(rule) { selectedRule = rule }
                                Spacer(modifier = Modifier.height(10.dp))
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
                                    scanId = (state as AnalysisState.Success).scanId,
                                    imageUris = images.map { it.uri.toString() }
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
    val list = remember(frontUri, backUri) {
        listOf(
            com.example.domain.model.ProductImage("1", frontUri, com.example.domain.model.ImageSource.CAMERA),
            com.example.domain.model.ProductImage("2", backUri, com.example.domain.model.ImageSource.CAMERA)
        )
    }
    ValidationScreen(
        images = list,
        userId = userId,
        onNavigateBack = onNavigateBack,
        onNavigateHome = onNavigateHome,
        onRaiseComplaint = onRaiseComplaint,
        viewModel = viewModel
    )
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

            Text("${report.summary.total_rules} Rules Evaluated", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("✓ ${report.summary.passed} Passed", color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                    Text("✗ ${report.summary.total_violations} Violations", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("— ${report.summary.not_required} Not Required", color = Slate400, fontWeight = FontWeight.Medium)
                    Text("⚠ ${report.summary.manual_review} Review", color = Color(0xFFEF6C00), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun RuleCard(rule: RuleResult, onClick: () -> Unit) {
    val (statusColor, statusIcon, bgColor, badgeText) = when (rule.status) {
        RuleStatus.PASS -> Quad(Color(0xFF2E7D32), Icons.Filled.CheckCircle, Color(0xFFE8F5E9), "PASS")
        RuleStatus.FAIL -> Quad(Color(0xFFC62828), Icons.Filled.Error, Color(0xFFFFEBEE), if (rule.violation) "VIOLATION" else "FAIL")
        RuleStatus.NOT_REQUIRED -> Quad(Slate400, Icons.Filled.Info, Color(0xFFF1F5F9), "NOT REQUIRED")
        RuleStatus.NOT_APPLICABLE -> Quad(Slate400, Icons.Filled.Info, Color(0xFFF1F5F9), "N/A")
        RuleStatus.MANUAL_REVIEW -> Quad(Color(0xFFEF6C00), Icons.Filled.Warning, Color(0xFFFFF3E0), "MANUAL REVIEW")
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
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        rule.field.replace("_", " ").replaceFirstChar { it.uppercase() },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

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
                        fontSize = 13.sp,
                        color = Slate700
                    )
                } else if (rule.status == RuleStatus.NOT_REQUIRED) {
                    Text(
                        "Optional declaration (not present)",
                        fontSize = 13.sp,
                        color = Slate400
                    )
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
                RuleStatus.FAIL -> (if (rule.violation) "VIOLATION (MANDATORY DECLARATION MISSING)" else "FAIL") to Color(0xFFC62828)
                RuleStatus.NOT_REQUIRED -> "NOT REQUIRED (OPTIONAL DECLARATION)" to Slate400
                RuleStatus.NOT_APPLICABLE -> "NOT APPLICABLE" to Slate400
                RuleStatus.MANUAL_REVIEW -> "MANUAL REVIEW" to Color(0xFFEF6C00)
            }

            Text(rule.legal_reference, fontSize = 14.sp, color = Slate400, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(rule.field.replace("_", " ").replaceFirstChar { it.uppercase() }, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Slate900)

            Spacer(modifier = Modifier.height(14.dp))

            Text("CLASSIFICATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
            Text(
                if (rule.mandatory) "MANDATORY DECLARATION" else "NON-MANDATORY / OPTIONAL",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (rule.mandatory) Slate900 else Slate400
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
            Text(statusText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = statusColor)

            Spacer(modifier = Modifier.height(14.dp))

            Text("DETECTED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
            Text(rule.extracted_value ?: "None", fontSize = 15.sp, color = Slate900)

            if (rule.source_image != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("SOURCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                Text(rule.source_image, fontSize = 14.sp, color = Slate900)
            }

            if (rule.confidence != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("CONFIDENCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                Text("${(rule.confidence * 100).toInt()}%", fontSize = 14.sp, color = Slate900)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("REQUIREMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
            Text(rule.requirement, fontSize = 13.sp, color = Slate700)

            Spacer(modifier = Modifier.height(14.dp))

            Text("EXPLANATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
            Text(rule.message, fontSize = 13.sp, color = Slate700)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
