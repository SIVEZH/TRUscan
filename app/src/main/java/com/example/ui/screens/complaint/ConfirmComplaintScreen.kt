package com.example.ui.screens.complaint

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ui.components.FrostedBackground
import com.example.ui.viewmodel.ComplaintViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmComplaintScreen(
    viewModel: ComplaintViewModel,
    onBackClick: () -> Unit,
    onEditUserDetails: () -> Unit,
    onEditProductDetails: () -> Unit,
    onRetakeLivePhoto: () -> Unit,
    onSubmitSuccess: (complaintId: String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // State from ViewModel
    val fullName by viewModel.fullName.collectAsState()
    val email by viewModel.email.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val address by viewModel.address.collectAsState()

    val productImages by viewModel.productImages.collectAsState()
    val purchasePlace by viewModel.purchasePlace.collectAsState()
    val purchaseLocation by viewModel.purchaseLocation.collectAsState()
    val purchaseDate by viewModel.purchaseDate.collectAsState()
    val additionalDescription by viewModel.additionalDescription.collectAsState()

    val livePhotoUri by viewModel.livePhotoUri.collectAsState()
    val isConfirmed by viewModel.isConfirmed.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val submissionError by viewModel.submissionError.collectAsState()

    var selectedImageForPreview by remember { mutableStateOf<Uri?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    FrostedBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Confirm Complaint",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Subtitle below the app bar
                Text(
                    text = "Review your details before submitting the complaint.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // ================= CARD 1 — YOUR DETAILS =================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_your_details"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Details",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = onEditUserDetails,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("edit_user_details_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit details",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        DetailRow(label = "Full Name", value = fullName.ifBlank { "Not provided" })
                        Spacer(modifier = Modifier.height(10.dp))
                        DetailRow(label = "Email Address", value = email.ifBlank { "Not provided" })
                        Spacer(modifier = Modifier.height(10.dp))
                        DetailRow(label = "Phone Number", value = phoneNumber.ifBlank { "Not provided" })
                        Spacer(modifier = Modifier.height(10.dp))
                        DetailRow(label = "Address", value = address.ifBlank { "Not provided" })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ================= CARD 2 — PRODUCT DETAILS =================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_product_details"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Product Details",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = onEditProductDetails,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("edit_product_details_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit product details",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Product Images Thumbnail Row
                        Text(
                            text = "Product Images (${productImages.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (productImages.isEmpty()) {
                            Text(
                                text = "No product images attached",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            val maxVisible = 4
                            val visibleImages = productImages.take(maxVisible)
                            val remainingCount = productImages.size - maxVisible

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(visibleImages) { _, img ->
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedImageForPreview = img.uri }
                                    ) {
                                        AsyncImage(
                                            model = img.uri,
                                            contentDescription = "Product thumbnail",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                if (remainingCount > 0) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { selectedImageForPreview = productImages[maxVisible].uri },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+$remainingCount",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        DetailRow(label = "Bought From", value = purchasePlace.ifBlank { "Not provided" })
                        Spacer(modifier = Modifier.height(10.dp))
                        DetailRow(label = "Purchase Location", value = purchaseLocation.ifBlank { "Not provided" })
                        Spacer(modifier = Modifier.height(10.dp))
                        DetailRow(label = "Purchase Date", value = if (purchaseDate.isNullOrBlank()) "Not provided" else purchaseDate!!)
                        Spacer(modifier = Modifier.height(10.dp))
                        DetailRow(label = "Additional Description", value = additionalDescription.ifBlank { "Not provided" })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ================= CARD 3 — LIVE PHOTO =================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_live_photo"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Photo",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = onRetakeLivePhoto,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("retake_live_photo_from_confirm")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retake photo",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retake", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        if (livePhotoUri.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = 1.5.dp,
                                        color = Color(0xFF16A34A).copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = livePhotoUri,
                                    contentDescription = "Live photo preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Strictly "Photo attached" - ZERO AI claims!
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFDCFCE7),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Photo attached",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No live photo attached. Please take a photo to continue.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ================= CONFIRMATION CHECKBOX =================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.isConfirmed.value = !isConfirmed },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConfirmed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isConfirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isConfirmed,
                            onCheckedChange = { viewModel.isConfirmed.value = it },
                            modifier = Modifier.testTag("confirm_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I confirm that the information provided is correct.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    }
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (submissionError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = submissionError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ================= SUBMIT COMPLAINT BUTTON =================
                Button(
                    onClick = {
                        if (isSubmitting) return@Button

                        val (isValid, err) = viewModel.validateAll()
                        if (!isValid) {
                            validationError = err
                            Toast.makeText(context, err ?: "Please complete all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        validationError = null
                        viewModel.submitComplaint(
                            context = context,
                            onSuccess = { complaintId ->
                                onSubmitSuccess(complaintId)
                            },
                            onError = { errMessage ->
                                Toast.makeText(context, errMessage, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = isConfirmed && !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("submit_complaint_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Submitting Complaint...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Submit Complaint",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConfirmed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }

    // Full screen image preview dialog
    if (selectedImageForPreview != null) {
        Dialog(onDismissRequest = { selectedImageForPreview = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = selectedImageForPreview,
                            contentDescription = "Full image preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { selectedImageForPreview = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Preview")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal,
            lineHeight = 20.sp
        )
    }
}
