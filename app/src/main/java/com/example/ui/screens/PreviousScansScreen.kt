package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ScanEntity
import com.example.domain.model.ValidationReport
import com.example.ui.components.FrostedBackground
import com.example.ui.theme.*
import com.example.ui.viewmodel.PreviousScansViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviousScansScreen(
    userId: String,
    onBackClick: () -> Unit,
    onScanProductClick: () -> Unit,
    onRaiseComplaint: ((String) -> Unit)? = null,
    viewModel: PreviousScansViewModel = viewModel()
) {
    val scans by viewModel.scans.collectAsState()
    var selectedScan by remember { mutableStateOf<ScanEntity?>(null) }
    var selectedReport by remember { mutableStateOf<ValidationReport?>(null) }
    
    val moshi = remember { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }

    LaunchedEffect(userId) {
        viewModel.loadScans(userId)
    }

    if (selectedReport != null && selectedScan != null) {
        ReportDetailScreen(
            report = selectedReport!!,
            scanId = selectedScan!!.scanId,
            timestamp = selectedScan!!.timestamp,
            onBackClick = { 
                selectedReport = null
                selectedScan = null
            },
            onRaiseComplaint = onRaiseComplaint
        )
        return
    }

    FrostedBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Previous Scans", fontWeight = FontWeight.Bold, color = Slate900) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate900)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            if (scans.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    EmptyScansState(onScanProductClick)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(scans) { scan ->
                        ScanCard(scan = scan) {
                            try {
                                val report = moshi.adapter(ValidationReport::class.java).fromJson(scan.reportJson)
                                if (report != null) {
                                    selectedScan = scan
                                    selectedReport = report
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyScansState(onScanProductClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = Color(0x1A000000),
                ambientColor = Color(0x0F000000)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Slate100, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = "History",
                    tint = Slate400,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "No previous scans",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Your scanned products will appear here.",
                fontSize = 14.sp,
                color = Slate500,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onScanProductClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Blue600),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue600)
            ) {
                Text(text = "Scan Product", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun ScanCard(scan: ScanEntity, onClick: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a")
        .withZone(ZoneId.systemDefault())
    
    val dateString = try {
        formatter.format(Instant.parse(scan.timestamp))
    } catch (e: Exception) {
        "Unknown date"
    }

    val (statusColor, statusIcon, bgColor) = when (scan.overallStatus) {
        "COMPLIANT" -> Triple(Color(0xFF2E7D32), Icons.Filled.CheckCircle, Color(0xFFE8F5E9))
        "NON_COMPLIANT" -> Triple(Color(0xFFC62828), Icons.Filled.Error, Color(0xFFFFEBEE))
        else -> Triple(Color(0xFFEF6C00), Icons.Filled.Warning, Color(0xFFFFF3E0))
    }
    
    val displayStatus = scan.overallStatus.replace("_", "-")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = scan.productName ?: "Unknown Product",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Text(
                text = scan.category,
                fontSize = 12.sp,
                color = Slate500,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(statusIcon, contentDescription = displayStatus, tint = statusColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        displayStatus,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                Text(
                    text = dateString,
                    fontSize = 12.sp,
                    color = Slate400
                )
            }
        }
    }
}
