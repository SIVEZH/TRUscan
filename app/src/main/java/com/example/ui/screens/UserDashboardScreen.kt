package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FrostedBackground
import com.example.ui.components.FrostedGlassButton
import com.example.ui.theme.*

@Composable
fun UserDashboardScreen(
    onScanProductClick: () -> Unit,
    onPreviousScansClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    FrostedBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .shadow(4.dp, RoundedCornerShape(10.dp), spotColor = Blue600)
                            .background(Blue600, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "TRUscan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        letterSpacing = (-0.5).sp
                    )
                }
                
                TextButton(onClick = onLogoutClick) {
                    Text("Logout", color = Slate500, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))
            
            // Title Area
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Welcome to TRUscan",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    lineHeight = 36.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Check packaged products easily and quickly.",
                    fontSize = 16.sp,
                    color = Slate500,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Buttons
            FrostedGlassButton(
                title = "Scan Product",
                subtitle = "Scan a packaged product to check its information.",
                icon = Icons.Filled.CameraAlt,
                iconBgColor = Blue100,
                iconColor = Blue700,
                onClick = onScanProductClick
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            FrostedGlassButton(
                title = "Previous Scans",
                subtitle = "View your previously scanned products.",
                icon = Icons.Filled.History,
                iconBgColor = Slate200,
                iconColor = Slate700,
                onClick = onPreviousScansClick
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
