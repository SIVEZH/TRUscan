package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.UserEntity
import com.example.ui.components.FrostedBackground
import com.example.ui.theme.*
import com.example.ui.viewmodel.ComplaintViewModel
import com.example.ui.viewmodel.PhotoClassification
import com.example.ui.viewmodel.VerificationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintRegistrationScreen(
    scanId: String,
    user: UserEntity?,
    onBackClick: () -> Unit,
    onReviewComplaint: () -> Unit,
    viewModel: ComplaintViewModel = viewModel()
) {
    val name by viewModel.name.collectAsState()
    val email by viewModel.email.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val address by viewModel.address.collectAsState()
    val purchaseLocation by viewModel.purchaseLocation.collectAsState()
    val purchaseDate by viewModel.purchaseDate.collectAsState()
    val receiptUri by viewModel.receiptUri.collectAsState()
    
    val emailVerificationStatus by viewModel.emailVerificationStatus.collectAsState()
    val phoneVerificationStatus by viewModel.phoneVerificationStatus.collectAsState()
    val photoVerificationStatus by viewModel.photoVerificationStatus.collectAsState()
    val photoClassification by viewModel.photoClassification.collectAsState()
    val livePhotoBitmap by viewModel.livePhotoBitmap.collectAsState()
    
    val emailOtp by viewModel.emailOtp.collectAsState()
    val phoneOtp by viewModel.phoneOtp.collectAsState()

    LaunchedEffect(user) {
        if (user != null) {
            if (viewModel.name.value.isEmpty()) viewModel.name.value = user.name
            if (viewModel.email.value.isEmpty()) viewModel.email.value = user.email
            if (viewModel.phone.value.isEmpty()) viewModel.phone.value = user.phone ?: ""
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.verifyLivePhoto(bitmap)
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.receiptUri.value = uri.toString()
        }
    }

    val isFormValid = name.isNotBlank() &&
        email.isNotBlank() && emailVerificationStatus == VerificationStatus.VERIFIED &&
        phone.isNotBlank() && phoneVerificationStatus == VerificationStatus.VERIFIED &&
        address.isNotBlank() &&
        photoVerificationStatus == VerificationStatus.VERIFIED &&
        purchaseLocation.isNotBlank()

    FrostedBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Raise a Complaint", fontWeight = FontWeight.Bold) },
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
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    "Please provide your details so we can verify your complaint.",
                    fontSize = 16.sp,
                    color = Slate700
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.name.value = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    label = { Text("Email Address *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = emailVerificationStatus != VerificationStatus.VERIFIED
                )
                
                val emailError = viewModel.emailErrorMessage.collectAsState().value
                if (emailError != null) {
                    Text(emailError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                
                if (emailVerificationStatus == VerificationStatus.IDLE || emailVerificationStatus == VerificationStatus.FAILED || emailVerificationStatus == VerificationStatus.EXPIRED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.sendEmailOtp() }) {
                        Text("Send OTP")
                    }
                } else if (emailVerificationStatus == VerificationStatus.VERIFYING || emailVerificationStatus == VerificationStatus.OTP_SENT) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Verification code sent to your email.", color = Color(0xFF2E7D32), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emailOtp,
                        onValueChange = { viewModel.emailOtp.value = it },
                        label = { Text("Enter OTP") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(onClick = { viewModel.verifyEmailOtp() }) {
                            Text("Verify Email")
                        }
                        
                        val emailCooldown = viewModel.emailResendCooldown.collectAsState().value
                        if (emailCooldown > 0) {
                            Text("Resend available in ${emailCooldown}s", color = Slate500, modifier = Modifier.align(Alignment.CenterVertically))
                        } else {
                            OutlinedButton(onClick = { viewModel.sendEmailOtp() }) {
                                Text("Resend OTP")
                            }
                        }
                    }
                } else if (emailVerificationStatus == VerificationStatus.VERIFIED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Email verified", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { viewModel.onPhoneChanged(it) },
                    label = { Text("Phone Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = phoneVerificationStatus != VerificationStatus.VERIFIED
                )
                
                val phoneError = viewModel.phoneErrorMessage.collectAsState().value
                if (phoneError != null) {
                    Text(phoneError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                
                if (phoneVerificationStatus == VerificationStatus.IDLE || phoneVerificationStatus == VerificationStatus.FAILED || phoneVerificationStatus == VerificationStatus.EXPIRED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.sendPhoneOtp() }) {
                        Text("Send OTP")
                    }
                } else if (phoneVerificationStatus == VerificationStatus.VERIFYING || phoneVerificationStatus == VerificationStatus.OTP_SENT) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Verification code sent to your phone.", color = Color(0xFF2E7D32), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneOtp,
                        onValueChange = { viewModel.phoneOtp.value = it },
                        label = { Text("Enter OTP") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(onClick = { viewModel.verifyPhoneOtp() }) {
                            Text("Verify Phone")
                        }
                        
                        val phoneCooldown = viewModel.phoneResendCooldown.collectAsState().value
                        if (phoneCooldown > 0) {
                            Text("Resend available in ${phoneCooldown}s", color = Slate500, modifier = Modifier.align(Alignment.CenterVertically))
                        } else {
                            OutlinedButton(onClick = { viewModel.sendPhoneOtp() }) {
                                Text("Resend OTP")
                            }
                        }
                    }
                } else if (phoneVerificationStatus == VerificationStatus.VERIFIED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Phone number verified", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { viewModel.address.value = it },
                    label = { Text("Address *") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Live Photo
                Text("Live Photo Verification *", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (livePhotoBitmap != null) {
                    Image(
                        bitmap = livePhotoBitmap!!.asImageBitmap(),
                        contentDescription = "Live Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (photoVerificationStatus == VerificationStatus.IDLE || photoVerificationStatus == VerificationStatus.FAILED) {
                    if (photoVerificationStatus == VerificationStatus.FAILED) {
                        val msg = when (photoClassification) {
                            PhotoClassification.NO_PERSON -> "We could not verify a person in this photo."
                            PhotoClassification.UNCERTAIN -> "We could not confidently verify the photo. Please retake it."
                            else -> "We could not verify the photo. Please retake it."
                        }
                        Text(msg, color = Color(0xFFC62828), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(onClick = {
                        viewModel.retakeLivePhoto()
                        cameraLauncher.launch(null) 
                    }) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = "Take Photo")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (livePhotoBitmap == null) "Take Live Photo" else "Retake Photo")
                    }
                } else if (photoVerificationStatus == VerificationStatus.VERIFYING) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Verifying photo...", color = Slate700)
                } else if (photoVerificationStatus == VerificationStatus.VERIFIED) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("✓ Photo verified", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { 
                            viewModel.retakeLivePhoto()
                            cameraLauncher.launch(null) 
                        }) {
                            Text("Retake")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Purchase Location
                OutlinedTextField(
                    value = purchaseLocation,
                    onValueChange = { viewModel.purchaseLocation.value = it },
                    label = { Text("Where did you buy the product? *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Purchase Date
                OutlinedTextField(
                    value = purchaseDate,
                    onValueChange = { viewModel.purchaseDate.value = it },
                    label = { Text("When did you buy the product? (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Receipt
                Text("Attach Purchase Bill / Receipt (Optional)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text(if (receiptUri == null) "Select Receipt" else "Change Receipt")
                }
                if (receiptUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Receipt attached.", color = Slate700)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onReviewComplaint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isFormValid,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue600)
                ) {
                    Text("Review Complaint", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
