package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val age: Int,
    val profilePhotoUri: String? = null,
    val base64QrCode: String, // The base64 representation of the generated QR code
    val biometricEmbeddingHex: String, // Simulating unique face descriptors
    val registeredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "verification_logs")
data class VerificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val livenessPassed: Boolean,
    val livenessScore: Float, // Frame diff delta score (e.g., 12.5)
    val similarityScore: Float, // Cosine Similarity percentage (e.g., 94.2)
    val isMatched: Boolean,
    val statusMessage: String
)
