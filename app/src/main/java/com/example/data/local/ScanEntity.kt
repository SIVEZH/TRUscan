package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey
    val scanId: String,
    val userId: String,
    val timestamp: String,
    val category: String,
    val productName: String?,
    val frontImageUri: String,
    val backImageUri: String,
    val overallStatus: String,
    val reportJson: String
)
