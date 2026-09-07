package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val phone: String?,
    val passwordHash: String,
    val role: String,
    val authorityCategory: String?,
    val createdAt: Long = System.currentTimeMillis()
)
