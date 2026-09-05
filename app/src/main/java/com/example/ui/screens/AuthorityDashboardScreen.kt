package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.local.ComplaintEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorityDashboardScreen(
    category: String?,
    onLogoutClick: () -> Unit,
    onComplaintClick: (String) -> Unit
) {
    val context = LocalContext.current
    var complaints by remember { mutableStateOf<List<ComplaintEntity>>(emptyList()) }
    
    val displayCategory = category?.uppercase() ?: "UNKNOWN"

    LaunchedEffect(displayCategory) {
        if (displayCategory != "UNKNOWN") {
            val db = AppDatabase.getDatabase(context)
            db.complaintDao().getComplaintsByCategory(displayCategory).collectLatest { list ->
                complaints = list
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$displayCategory Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        if (complaints.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No complaints found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(complaints) { complaint ->
                    ComplaintCard(complaint = complaint, onClick = { onComplaintClick(complaint.complaintId) })
                }
            }
        }
    }
}

@Composable
fun ComplaintCard(complaint: ComplaintEntity, onClick: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault())
    val dateStr = try {
        formatter.format(Instant.parse(complaint.createdAt))
    } catch (e: Exception) {
        "Unknown Date"
    }

    // Since reportJson is complex, we just show simple fields or default values for violations count
    val violationsCount = complaint.reportJson.split("FAIL").size - 1

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Complaint #${complaint.complaintId}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(complaint.status, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (complaint.status == "NEW") Color(0xFF1565C0) else Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Product: ${complaint.productName ?: "Unknown"}", fontWeight = FontWeight.Medium)
            Text("Category: ${complaint.category}", fontSize = 14.sp)
            Text("Violations: $violationsCount", fontSize = 14.sp, color = Color(0xFFC62828))
            Text("Location: ${complaint.purchaseLocation}", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Submitted: $dateStr", fontSize = 12.sp, color = Color.Gray)
        }
    }
}
