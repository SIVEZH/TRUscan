package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.ImageSource
import com.example.domain.model.ProductImage
import com.example.ui.components.FrostedBackground
import com.example.ui.components.FrostedGlassButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanProductScreen(
    viewModel: ScanViewModel,
    onBackClick: () -> Unit,
    onUseCameraClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    val productImages by viewModel.productImages.collectAsState()

    // Multiple selection Photo Picker
    val multipleGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                viewModel.addImages(uris, ImageSource.GALLERY)
            }
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate900)
                        }
                    },
                    actions = {
                        if (productImages.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearImages() }) {
                                Icon(
                                    Icons.Filled.DeleteSweep,
                                    contentDescription = "Clear All",
                                    tint = Slate500
                                )
                            }
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
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Add Product Images",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Capture or select as many images as needed (labels, ingredients, dates, packaging).",
                    fontSize = 13.sp,
                    color = Slate500,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (productImages.isEmpty()) {
                    // Initial Empty State: Clean Choices
                    Spacer(modifier = Modifier.height(24.dp))

                    FrostedGlassButton(
                        title = "Take Photos with Camera",
                        subtitle = "Continuous CameraX capture for multiple sides and labels.",
                        icon = Icons.Filled.CameraAlt,
                        iconBgColor = Blue100,
                        iconColor = Blue700,
                        onClick = onUseCameraClick
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    FrostedGlassButton(
                        title = "Select from Gallery",
                        subtitle = "Choose one or multiple photos from your device library.",
                        icon = Icons.Filled.PhotoLibrary,
                        iconBgColor = Slate200,
                        iconColor = Slate700,
                        onClick = {
                            multipleGalleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Add at least one product image to continue.",
                        fontSize = 13.sp,
                        color = Slate400,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                } else {
                    // Unified Image Collection Grid View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Collected Images (${productImages.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )

                        Text(
                            text = "Tap ✕ to remove",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Grid of images + Add Tile
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(productImages) { index, item ->
                            ProductImageTile(
                                item = item,
                                index = index,
                                onRemove = { viewModel.removeImage(item.id) }
                            )
                        }

                        // Add from Camera Tile
                        item {
                            AddTile(
                                icon = Icons.Filled.CameraAlt,
                                label = "Camera",
                                onClick = onUseCameraClick
                            )
                        }

                        // Add from Gallery Tile
                        item {
                            AddTile(
                                icon = Icons.Filled.PhotoLibrary,
                                label = "Gallery",
                                onClick = {
                                    multipleGalleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )
                        }
                    }

                    // Bottom Action Panel
                    Surface(
                        color = GlassWhite,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onUseCameraClick,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue700)
                                ) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Camera", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        multipleGalleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700)
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Gallery", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onContinueClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Blue600),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                                enabled = productImages.isNotEmpty()
                            ) {
                                Text(
                                    text = "Continue with ${productImages.size} ${if (productImages.size == 1) "Image" else "Images"}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductImageTile(
    item: ProductImage,
    index: Int,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Slate100)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = "Product Image ${index + 1}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Top tag with Image # and Source
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(topEnd = 8.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "#${index + 1} • ${item.source.name.lowercase().replaceFirstChar { it.uppercase() }}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Close / Remove Button
        Box(
            modifier = Modifier
                .size(26.dp)
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun AddTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(GlassWhite)
            .border(1.5.dp, Slate200, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Blue100, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Blue700,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "+ $label",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate700
            )
        }
    }
}
