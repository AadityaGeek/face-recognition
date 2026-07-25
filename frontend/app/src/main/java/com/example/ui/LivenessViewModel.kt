package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel handling registration, liveness verification, and state.
 * Code implementation hidden for open-source repository preview.
 */
sealed interface RegistrationState {
    object Form : RegistrationState
    object PhotoCapture : RegistrationState
    data class Uploading(val progress: Float, val stage: String) : RegistrationState
    data class DuplicateError(val message: String) : RegistrationState
    data class Success(val userId: String, val base64QrCode: String) : RegistrationState
}

sealed interface VerificationState {
    object Idle : VerificationState
    data class FaceCapture(val userId: String) : VerificationState
    data class RecordingCountdown(val secondsLeft: Int, val progress: Float) : VerificationState
    data class Uploading(val progress: Float, val stage: String) : VerificationState
    data class LivenessFailed(val userId: String, val score: Float, val threshold: Float = 40.0f, val message: String) : VerificationState
    data class MatchResult(
        val userId: String,
        val userName: String,
        val userAge: String? = null,
        val isSuccess: Boolean,
        val similarityScore: Float,
        val thresholdPercent: Float = 60f,
        val livenessScore: Float,
        val message: String
    ) : VerificationState
}

class LivenessViewModel(application: Application) : AndroidViewModel(application) {
    val regName = MutableStateFlow("")
    val regAge = MutableStateFlow("")
    val regUserId = MutableStateFlow("")
    val verUserIdInput = MutableStateFlow("")

    fun submitRegistration() {
        TODO("Implementation hidden")
    }

    fun startVerificationFlow(userId: String) {
        TODO("Implementation hidden")
    }

    fun verifyCapturedFace(userId: String, facePhoto: Bitmap) {
        TODO("Implementation hidden")
    }
}
