package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.FrostedBackground
import com.example.ui.components.FrostedGlassButton
import com.example.ui.theme.*

@Composable
fun LoginSelectionScreen(
    onUserLoginClick: () -> Unit,
    onAuthorityLoginClick: () -> Unit
) {
    FrostedBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Logo & Title
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color(0x3336D399))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF11181B)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "TRUscan Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "TRUscan",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                letterSpacing = (-0.5).sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Smart Packaged Commodity Compliance",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Slate500,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Buttons
            FrostedGlassButton(
                title = "USER LOGIN",
                subtitle = "Access verification tools & reports",
                icon = Icons.Filled.Person,
                iconBgColor = Blue100,
                iconColor = Blue700,
                onClick = onUserLoginClick
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FrostedGlassButton(
                title = "AUTHORITY LOGIN",
                subtitle = "Regulatory dashboard & case review",
                icon = Icons.Filled.AdminPanelSettings,
                iconBgColor = Amber100,
                iconColor = Amber700,
                onClick = onAuthorityLoginClick
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Supported Categories banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x80EFF6FF), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0x80DBEAFE), RoundedCornerShape(24.dp))
                    .padding(vertical = 20.dp, horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SUPPORTED CATEGORIES",
                        color = Color(0xCC2563EB),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FOOD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900.copy(alpha = 0.6f),
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Blue300, CircleShape)
                        )
                        Text(
                            text = "COSMETICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900.copy(alpha = 0.6f),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            // Footer
            Text(
                text = "Secure Government Compliance Interface",
                color = Slate400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("TERMS", color = Blue600.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("PRIVACY", color = Blue600.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("HELP", color = Blue600.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
