package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val age: Int,
    val profilePhotoUri: String? = null,
    val base64QrCode: String,
    val biometricEmbeddingHex: String,
    val registeredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "verification_logs")
data class VerificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val livenessPassed: Boolean,
    val livenessScore: Float,
    val similarityScore: Float,
    val isMatched: Boolean,
    val statusMessage: String
)
