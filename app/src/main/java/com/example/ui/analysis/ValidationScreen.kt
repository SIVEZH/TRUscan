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
import androidx.compose.material.icons.filled.Check
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
import com.example.domain.model.Applicability
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
    category: com.example.domain.model.ProductCategory?,
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
            viewModel.analyzeProduct(images, category, userId)
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
                                onClick = { viewModel.analyzeProduct(images, category, userId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue600)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                    is AnalysisState.Success -> {
                        val report = currentState.report
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
                                        "Declarations not mandatory under applicable rules:",
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

                            // 6. NOT APPLICABLE
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
                                Spacer(modifier = Modifier.height(32.dp))
                                // Raise a Complaint is ONLY shown when there is at least one confirmed mandatory violation
                                if (report.canRaiseComplaint) {
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
        category = null,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Compliance Summary",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp
                )
                Text(
                    report.category,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Blue600
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            val (statusText, statusColor, statusIcon, statusDesc) = when (report.overall_status) {
                OverallStatus.COMPLIANT -> Quad(
                    "COMPLIANT",
                    Color(0xFF2E7D32),
                    Icons.Filled.CheckCircle,
                    "✓ Product Compliant (No mandatory violations found)"
                )
                OverallStatus.NON_COMPLIANT -> Quad(
                    "NON-COMPLIANT",
                    Color(0xFFC62828),
                    Icons.Filled.Error,
                    "${report.summary.total_violations} Mandatory Violation${if (report.summary.total_violations > 1) "s" else ""} Found"
                )
                OverallStatus.MANUAL_REVIEW -> Quad(
                    "MANUAL REVIEW",
                    Color(0xFFEF6C00),
                    Icons.Filled.Warning,
                    "Some mandatory declarations require manual verification."
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(statusIcon, contentDescription = statusText, tint = statusColor, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        statusText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        statusDesc,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate700
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            Text("${report.summary.total_rules} Total Rules", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.6f)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("✓ ${report.summary.passed} Passed", color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                        Text(
                            "✕ ${report.summary.total_violations} Violations",
                            color = if (report.summary.total_violations > 0) Color(0xFFC62828) else Slate400,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("✓ ${report.summary.not_mandatory} Not Mandatory", color = Slate700, fontWeight = FontWeight.Medium)
                        if (report.summary.not_required > 0) {
                            Text("✓ ${report.summary.not_required} Not Required", color = Slate700, fontWeight = FontWeight.Medium)
                        } else if (report.summary.not_applicable > 0) {
                            Text("— ${report.summary.not_applicable} Not Applicable", color = Slate400, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (report.summary.not_required > 0 && report.summary.not_applicable > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("— ${report.summary.not_applicable} Not Applicable", color = Slate400, fontWeight = FontWeight.Medium)
                            if (report.summary.manual_review > 0) {
                                Text("⚠ ${report.summary.manual_review} Review", color = Color(0xFFEF6C00), fontWeight = FontWeight.Medium)
                            }
                        }
                    } else if (report.summary.manual_review > 0) {
                        Text("⚠ ${report.summary.manual_review} Review Required", color = Color(0xFFEF6C00), fontWeight = FontWeight.Medium)
                    }
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
        RuleStatus.NOT_MANDATORY -> Quad(Slate700, Icons.Filled.Check, Color(0xFFF1F5F9), "NOT MANDATORY")
        RuleStatus.NOT_REQUIRED -> Quad(Slate700, Icons.Filled.Check, Color(0xFFF1F5F9), "NOT REQUIRED")
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
                } else if (rule.status == RuleStatus.NOT_MANDATORY) {
                    Text(
                        if (!rule.extracted_value.isNullOrBlank()) "Detected: ${rule.extracted_value} (not mandatory)" else "Not mandatory under applicable rule",
                        fontSize = 13.sp,
                        color = Slate400
                    )
                } else if (rule.status == RuleStatus.NOT_REQUIRED) {
                    Text(
                        if (!rule.extracted_value.isNullOrBlank()) "Detected: ${rule.extracted_value} (not required)" else "Not required for current packaging context",
                        fontSize = 13.sp,
                        color = Slate400
                    )
                } else if (rule.status == RuleStatus.NOT_APPLICABLE) {
                    Text(
                        "Not applicable to this product category",
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
                RuleStatus.NOT_MANDATORY -> "NOT MANDATORY (NON-MANDATORY DECLARATION)" to Slate400
                RuleStatus.NOT_REQUIRED -> "NOT REQUIRED (CONDITION NOT APPLICABLE)" to Slate400
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
