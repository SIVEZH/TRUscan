package com.example.domain.model

import android.net.Uri

enum class ImageSource {
    CAMERA,
    GALLERY
}

data class ProductImage(
    val id: String,
    val uri: Uri,
    val source: ImageSource = ImageSource.CAMERA,
    val timestamp: Long = System.currentTimeMillis()
)
