package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey val complaintId: String,
    val userId: String,
    val sourceScanId: String,
    
    val category: String,
    val productName: String?,
    val reportJson: String,
    
    // Complainant Details
    val complainantName: String,
    val complainantEmail: String,
    val complainantPhone: String,
    val complainantAddress: String,
    
    // Purchase Details
    val purchaseLocation: String,
    val purchaseDate: String?,
    val receiptUri: String?,
    val purchasePlace: String? = null,
    val additionalDescription: String? = null,
    val livePhotoUri: String? = null,
    val productImagesJson: String? = null,
    
    // Status and Authority
    val status: String, // NEW, UNDER_REVIEW, ADDED_TO_VIOLATION_LIST, REJECTED, REJECTED_ACTION_REQUIRED
    val authorityId: String?,
    val authorityAction: String?, // ADD_TO_VIOLATION_LIST, REJECT_NO_ACTION, REJECT_TAKE_ACTION
    val rejectionReason: String?,
    val actionTimestamp: String?,
    
    val createdAt: String
)
