package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.components.FrostedBackground
import com.example.ui.components.FrostedGlassButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.ScanViewModel
import com.example.domain.model.ValidationState
import com.example.domain.model.ValidationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanProductScreen(
    viewModel: ScanViewModel,
    onBackClick: () -> Unit,
    onUseCameraClick: (isFront: Boolean) -> Unit,
    onContinueClick: () -> Unit
) {
    val frontImageUri by viewModel.frontImageUri.collectAsState()
    val backImageUri by viewModel.backImageUri.collectAsState()
    val frontValidationState by viewModel.frontValidationState.collectAsState()
    val backValidationState by viewModel.backValidationState.collectAsState()
    val context = LocalContext.current

    var isGalleryMode by remember { mutableStateOf(false) }

    val frontGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> 
            if (uri != null) viewModel.setFrontImage(uri) 
        }
    )
    
    val backGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> 
            if (uri != null) viewModel.setBackImage(uri) 
        }
    )

    FrostedBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Scan Product", fontWeight = FontWeight.Bold, color = Slate900) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Slate900)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Scan both sides of the product",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Provide clear images of the front and back of the package.",
                    fontSize = 14.sp,
                    color = Slate500,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (!isGalleryMode && frontImageUri == null && backImageUri == null) {
                    // Initial Selection State
                    FrostedGlassButton(
                        title = "Upload from Gallery",
                        subtitle = "Select the front and back images from your phone.",
                        icon = Icons.Filled.PhotoLibrary,
                        iconBgColor = Slate200,
                        iconColor = Slate700,
                        onClick = { isGalleryMode = true }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    FrostedGlassButton(
                        title = "Use Camera",
                        subtitle = "Take photos of the front and back of the package.",
                        icon = Icons.Filled.CameraAlt,
                        iconBgColor = Blue100,
                        iconColor = Blue700,
                        onClick = { onUseCameraClick(true) }
                    )
                } else {
                    // Placeholders / Preview State
                    ImageUploadCard(
                        title = "Front of Product",
                        imageUri = frontImageUri,
                        validationState = frontValidationState,
                        onGalleryClick = { frontGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onCameraClick = { onUseCameraClick(true) },
                        buttonText = if (frontImageUri == null) "Add Front Image" else "Replace Front",
                        isGalleryMode = isGalleryMode
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    ImageUploadCard(
                        title = "Back of Product",
                        imageUri = backImageUri,
                        validationState = backValidationState,
                        onGalleryClick = { backGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onCameraClick = { onUseCameraClick(false) },
                        buttonText = if (backImageUri == null) "Add Back Image" else "Replace Back",
                        isGalleryMode = isGalleryMode
                    )

                    Spacer(modifier = Modifier.height(48.dp))
                    
                    val isContinueEnabled = frontImageUri != null && backImageUri != null && 
                                            frontValidationState.status == ValidationStatus.VALID && 
                                            backValidationState.status == ValidationStatus.VALID
                    
                    Button(
                        onClick = { 
                            if (isContinueEnabled) {
                                onContinueClick()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = if (isContinueEnabled) Blue600 else Slate400),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isContinueEnabled) Blue600 else Slate400,
                            disabledContainerColor = Slate400
                        ),
                        enabled = isContinueEnabled
                    ) {
                        Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ImageUploadCard(
    title: String,
    imageUri: Uri?,
    validationState: ValidationState,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    buttonText: String,
    isGalleryMode: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Slate900,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = Color(0x1A000000),
                    ambientColor = Color(0x0F000000)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(GlassWhite)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Validation Status View
                    if (validationState.status != ValidationStatus.EMPTY) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    when (validationState.status) {
                                        ValidationStatus.VALID -> Color(0xFFE8F5E9)
                                        ValidationStatus.INVALID -> Color(0xFFFFEBEE)
                                        ValidationStatus.ERROR -> Color(0xFFFFF3E0)
                                        ValidationStatus.VALIDATING -> Color(0xFFE3F2FD)
                                        else -> Color.Transparent
                                    },
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            when (validationState.status) {
                                ValidationStatus.VALID -> Icon(Icons.Filled.CheckCircle, contentDescription = "Valid", tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                ValidationStatus.INVALID -> Icon(Icons.Filled.Warning, contentDescription = "Invalid", tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                                ValidationStatus.ERROR -> Icon(Icons.Filled.Error, contentDescription = "Error", tint = Color(0xFFEF6C00), modifier = Modifier.size(20.dp))
                                ValidationStatus.VALIDATING -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Blue600, strokeWidth = 2.dp)
                                else -> {}
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = validationState.reason ?: "Checking image...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = when (validationState.status) {
                                    ValidationStatus.VALID -> Color(0xFF2E7D32)
                                    ValidationStatus.INVALID -> Color(0xFFC62828)
                                    ValidationStatus.ERROR -> Color(0xFFEF6C00)
                                    ValidationStatus.VALIDATING -> Blue700
                                    else -> Slate700
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Slate100, RoundedCornerShape(16.dp))
                            .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                            .clickable { if (isGalleryMode) onGalleryClick() else onCameraClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add",
                                tint = Slate500,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = buttonText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate500
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Show replace/action buttons below
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onGalleryClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate100,
                            contentColor = Slate900
                        )
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = "Gallery", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (imageUri == null) "Gallery" else "Replace", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = onCameraClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue100,
                            contentColor = Blue700
                        )
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Camera", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
