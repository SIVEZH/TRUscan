package com.example.ui.screens.complaint

import android.app.DatePickerDialog
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.Image
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.domain.model.ProductImage
import com.example.ui.components.FrostedBackground
import com.example.ui.viewmodel.ComplaintViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ProductPurchaseDetailsScreen(
    viewModel: ComplaintViewModel,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    val context = LocalContext.current
    val productImages by viewModel.productImages.collectAsState()
    val productName by viewModel.productName.collectAsState()
    val purchasePlace by viewModel.purchasePlace.collectAsState()
    val purchaseLocation by viewModel.purchaseLocation.collectAsState()
    val purchaseDate by viewModel.purchaseDate.collectAsState()
    val additionalDescription by viewModel.additionalDescription.collectAsState()

    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var touched by remember { mutableStateOf(false) }
    var selectedImageForPreview by remember { mutableStateOf<Uri?>(null) }

    fun validate(): Boolean {
        touched = true
        val (valid, errorMap) = viewModel.validateProductDetails()
        errors = errorMap
        return valid
    }

    // Date picker setup
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                viewModel.setPurchaseDate(format.format(cal.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    FrostedBackground {
        Scaffold(
            topBar = {
                ComplaintFlowHeader(
                    step = 2,
                    totalSteps = 4,
                    stepTitle = "Product & Purchase",
                    stepDescription = "Verify the scanned product images and enter details about where and when the item was purchased.",
                    onBackClick = onBackClick
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                // Section 1: Attached Product Images
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Product Images (${productImages.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (productName != null) {
                                Text(
                                    text = productName!!,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }

                        Text(
                            text = "Preserved from your compliance scan. Tap any image to preview.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        if (productImages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "No images associated with scan",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(productImages) { index, img ->
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedImageForPreview = img.uri }
                                    ) {
                                        AsyncImage(
                                            model = img.uri,
                                            contentDescription = "Product image ${index + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "#${index + 1}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (errors.containsKey("productImages")) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errors["productImages"]!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 2: Purchase Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // 6. Where did you buy the product?
                        Column {
                            Text(
                                text = "Where did you buy the product? *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = purchasePlace,
                                onValueChange = {
                                    viewModel.purchasePlace.value = it
                                    if (touched) validate()
                                },
                                placeholder = { Text("Shop / Store / Market name", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Store,
                                        contentDescription = "Shop Name",
                                        tint = if (errors.containsKey("purchasePlace")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                },
                                isError = errors.containsKey("purchasePlace"),
                                supportingText = {
                                    if (errors.containsKey("purchasePlace")) {
                                        Text(errors["purchasePlace"]!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    } else {
                                        Text("Name of vendor, retail shop, or supermarket", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("complaint_purchase_place_input")
                            )
                        }

                        // 7. Purchase location/address
                        Column {
                            Text(
                                text = "Purchase location / address *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = purchaseLocation,
                                onValueChange = {
                                    viewModel.purchaseLocation.value = it
                                    if (touched) validate()
                                },
                                placeholder = { Text("Street, market area, village or city", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Purchase Location",
                                        tint = if (errors.containsKey("purchaseLocation")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                },
                                isError = errors.containsKey("purchaseLocation"),
                                supportingText = {
                                    if (errors.containsKey("purchaseLocation")) {
                                        Text(errors["purchaseLocation"]!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    } else {
                                        Text("Where the seller is located for regulatory inspection", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                minLines = 2,
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("complaint_purchase_location_input")
                            )
                        }

                        // 8. Purchase date (Optional)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Purchase date (Optional)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!purchaseDate.isNullOrBlank()) {
                                    TextButton(
                                        onClick = { viewModel.setPurchaseDate(null) },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear Date", fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedCard(
                                onClick = { datePickerDialog.show() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Calendar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (!purchaseDate.isNullOrBlank()) purchaseDate!! else "Select purchase date (tap to choose)",
                                        fontSize = 15.sp,
                                        color = if (!purchaseDate.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Text(
                                text = "You can skip this if you don't recall the exact date",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }

                        // 9. Additional complaint description (Optional)
                        Column {
                            Text(
                                text = "Additional description (Optional)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = additionalDescription,
                                onValueChange = { viewModel.additionalDescription.value = it },
                                placeholder = { Text("Describe anything else you want to report (e.g., missing MRP, overcharging, missing manufacturer address, tampering).", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "Description",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                minLines = 3,
                                maxLines = 6,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("complaint_additional_description_input")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (validate()) {
                            onContinueClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("continue_to_live_photo_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Continue to Live Photo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
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
