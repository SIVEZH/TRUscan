package com.example.domain.model

data class ComplaintData(
    val complaintId: String = "",
    val sourceScanId: String = "",
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val address: String,
    val productImages: List<ProductImage> = emptyList(),
    val purchasePlace: String,
    val purchaseLocation: String,
    val purchaseDate: String? = null,
    val additionalDescription: String? = null,
    val livePhotoUri: String,
    val complaintCreatedAt: String
)
