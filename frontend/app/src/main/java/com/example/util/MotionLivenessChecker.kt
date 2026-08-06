package com.example.util

import android.graphics.Bitmap
import android.media.Image
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs

enum class MotionChallengeType(
    val title: String,
    val instruction: String,
    val iconEmoji: String
) {
    BLINK("Blink Eyes", "Blink both eyes clearly", "👁️"),
    SMILE("Smile Broadly", "Smile at the camera", "😊"),
    TURN_LEFT("Turn Head Left", "Turn your head slowly to the left", "⬅️"),
    TURN_RIGHT("Turn Head Right", "Turn your head slowly to the right", "➡️"),
    NOD_UP("Tilt Head Up", "Tilt your head slightly upwards", "⬆️"),
    NOD_DOWN("Tilt Head Down", "Tilt your head slightly downwards", "⬇️")
}

data class MotionChallengeStatus(
    val challenge: MotionChallengeType,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val progress: Float = 0.0f,
    val feedbackMessage: String = "Perform gesture to verify",
    val errorMessage: String? = null
)

data class MotionLivenessResult(
    val passed: Boolean,
    val totalScore: Float, // 0.0f to 100.0f
    val completedChallengesCount: Int,
    val totalRequiredChallengesCount: Int,
    val passiveResult: PassiveLivenessResult,
    val detailsMessage: String
)

class MotionLivenessDetector(
    private val requiredChallenges: List<MotionChallengeType>,
    private val onChallengeUpdated: (currentChallengeIndex: Int, status: MotionChallengeStatus) -> Unit,
    private val onAllChallengesCompleted: () -> Unit
) {
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(options)

    private var currentChallengeIndex = 0
    private var isBlinkEyeClosedState = false
    private var isProcessingFrame = false
    private var challengeStartTimeMs = System.currentTimeMillis()
    private var incorrectGestureFrameCount = 0

    private val CHALLENGE_TIMEOUT_MS = 15000L // 15 seconds per challenge

    fun reset() {
        currentChallengeIndex = 0
        isBlinkEyeClosedState = false
        isProcessingFrame = false
        challengeStartTimeMs = System.currentTimeMillis()
        incorrectGestureFrameCount = 0
    }

    fun getCurrentChallenge(): MotionChallengeType? {
        return requiredChallenges.getOrNull(currentChallengeIndex)
    }

    @OptIn(ExperimentalGetImage::class)
    fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isProcessingFrame) {
            imageProxy.close()
            return
        }

        isProcessingFrame = true
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces.first()
                    evaluateFaceForChallenge(face)
                } else {
                    checkTimeout()
                }
            }
            .addOnCompleteListener {
                isProcessingFrame = false
                imageProxy.close()
            }
    }

    fun processBitmap(bitmap: Bitmap) {
        if (isProcessingFrame) return
        isProcessingFrame = true
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces.first()
                    evaluateFaceForChallenge(face)
                } else {
                    checkTimeout()
                }
            }
            .addOnCompleteListener {
                isProcessingFrame = false
            }
    }

    private fun checkTimeout() {
        val challenge = getCurrentChallenge() ?: return
        val elapsed = System.currentTimeMillis() - challengeStartTimeMs
        if (elapsed > CHALLENGE_TIMEOUT_MS) {
            failCurrentChallenge("Motion Challenge Timed Out: Failed to perform ${challenge.title} within 15s limit.")
        }
    }

    fun failCurrentChallenge(reason: String) {
        val challenge = getCurrentChallenge() ?: return
        onChallengeUpdated(
            currentChallengeIndex,
            MotionChallengeStatus(
                challenge = challenge,
                isCompleted = false,
                isFailed = true,
                progress = 0.0f,
                feedbackMessage = "❌ Challenge Failed",
                errorMessage = reason
            )
        )
    }

    fun simulateCurrentChallengeSuccess() {
        val challenge = getCurrentChallenge() ?: return
        onChallengeUpdated(
            currentChallengeIndex,
            MotionChallengeStatus(
                challenge = challenge,
                isCompleted = true,
                isFailed = false,
                progress = 1.0f,
                feedbackMessage = "Gesture Verified!"
            )
        )

        currentChallengeIndex++
        isBlinkEyeClosedState = false
        challengeStartTimeMs = System.currentTimeMillis()
        incorrectGestureFrameCount = 0

        if (currentChallengeIndex >= requiredChallenges.size) {
            onAllChallengesCompleted()
        }
    }

    private fun evaluateFaceForChallenge(face: Face) {
        val challenge = getCurrentChallenge() ?: return

        val elapsed = System.currentTimeMillis() - challengeStartTimeMs
        if (elapsed > CHALLENGE_TIMEOUT_MS) {
            failCurrentChallenge("Motion Verification Timed Out: 15s limit exceeded for ${challenge.title}.")
            return
        }

        when (challenge) {
            MotionChallengeType.BLINK -> {
                val leftOpen = face.leftEyeOpenProbability ?: -1f
                val rightOpen = face.rightEyeOpenProbability ?: -1f
                val yaw = face.headEulerAngleY
                val pitch = face.headEulerAngleX

                if (abs(yaw) > 15.0f || abs(pitch) > 15.0f) {
                    incorrectGestureFrameCount++
                    onChallengeUpdated(
                        currentChallengeIndex,
                        MotionChallengeStatus(
                            challenge = challenge,
                            isCompleted = false,
                            progress = 0f,
                            feedbackMessage = "⚠️ Incorrect gesture! Keep face centered while blinking"
                        )
                    )
                } else if (leftOpen in 0.0f..0.40f && rightOpen in 0.0f..0.40f && leftOpen >= 0f && rightOpen >= 0f) {
                    isBlinkEyeClosedState = true
                    incorrectGestureFrameCount = 0
                    onChallengeUpdated(
                        currentChallengeIndex,
                        MotionChallengeStatus(
                            challenge = challenge,
                            isCompleted = false,
                            progress = 0.5f,
                            feedbackMessage = "Eyes closed detected... now open eyes"
                        )
                    )
                } else if (isBlinkEyeClosedState && leftOpen > 0.55f && rightOpen > 0.55f) {
                    // Blink complete!
                    simulateCurrentChallengeSuccess()
                } else {
                    onChallengeUpdated(
                        currentChallengeIndex,
                        MotionChallengeStatus(
                            challenge = challenge,
                            isCompleted = false,
                            progress = if (isBlinkEyeClosedState) 0.5f else 0.1f,
                            feedbackMessage = "Blink both eyes clearly"
                        )
                    )
                }
            }

            MotionChallengeType.SMILE -> {
                val smileProb = face.smilingProbability ?: -1f
                val yaw = face.headEulerAngleY
                val targetThreshold = 0.45f
                val progress = if (smileProb >= 0) (smileProb / targetThreshold).coerceIn(0.0f, 1.0f) else 0f

                if (abs(yaw) > 15.0f) {
                    incorrectGestureFrameCount++
                    onChallengeUpdated(
                        currentChallengeIndex,
                        MotionChallengeStatus(
                            challenge = challenge,
                            isCompleted = false,
                            progress = 0f,
                            feedbackMessage = "⚠️ Incorrect gesture! Face camera directly and smile"
                        )
                    )
                } else if (smileProb > targetThreshold) {
                    simulateCurrentChallengeSuccess()
                } else {
                    onChallengeUpdated(
                        currentChallengeIndex,
                        MotionChallengeStatus(
                            challenge = challenge,
                            isCompleted = false,
                            progress = progress,
                            feedbackMessage = if (smileProb > 0.25f) "Almost! Smile a bit wider" else "Smile at the camera"
                        )
                    )
                }
            }

            MotionChallengeType.TURN_LEFT -> {
                val yaw = face.headEulerAngleY // Yaw: >0 is left, <0 is right
                val targetThreshold = 12.0f

                if (yaw > targetThreshold) {
                    simulateCurrentChallengeSuccess()
                } else if (yaw < -10.0f) {
                    // User turned head RIGHT instead of LEFT!
                    incorrectGestureFrameCount++
                    onChallengeUpdated(
                        currentChallengeIndex,
                        MotionChallengeStatus(
                            challenge = challenge,
                            isCompleted = false,
                            progress = 0f,
                            feedbackMessage = "⚠️ Incorrect gesture! Turned head RIGHT instead of LEFT"
                        )
                    )
                } else {
                    val progress = (yaw.coerceAtLeast(0f) / targetThreshold).coerceIn(0.0f, 1.0f)
                    onChallengeUpdated(
                        currentChallengeIndex,
                        MotionChallengeStatus(
                            challenge = challenge,
                            isCompleted = false,
                            progress = progress,
                            feedbackMessage = "Turn head slowly to the left"
                        )
                    )
                }
            }

            MotionChallengeType.TURN_RIGHT -> {
                val yaw = face.headEulerAngleY // Yaw: <0 is right, >0 is left
                val targetThreshold = 12.0f

                if (yaw < -targetThreshold) {
                    simulateCurrentChallengeSuccess()
                } else if (yaw > 10.0f) {
                    // User turned head LEFT instead of RIGHT!
                    incorrectGestureFrameCount++
                    onChallengeUpdated(
                        currentChallengeIndex,
                        MotionChallengeStatus(
                            challenge = challenge,
                            isCompleted = false,
                            progress = 0f,
                            feedbackMessage = "⚠️ Incorrect gesture! Turned head LEFT instead of RIGHT"
                        )
                    )
                } else {
                    val progress = (abs(yaw.coerceAtMost(0f)) / targetThreshold).coerceIn(0.0f, 1.0f)
                    onChallengeUpdated(
                        currentChallengeIndex,
                        MotionChallengeStatus(
                            challenge = challenge,
                            isCompleted = false,
                            progress = progress,
                            feedbackMessage = "Turn head slowly to the right"
                        )
                    )
                }
            }

            MotionChallengeType.NOD_UP -> {
                val pitch = face.headEulerAngleX // Pitch: >0 is up, <0 is down
                val targetThreshold = 10.0f

                if (pitch > targetThreshold) {
                    simulateCurrentChallengeSuccess()
                } else if (pitch < -8.0f) {
                    // User tilted head DOWN instead of UP!
                    incorrectGestureFrameCount++
                    onChallengeUpdated(
                        currentChallengeIndex,
                        MotionChallengeStatus(
                            challenge = challenge,
                            isCompleted = false,
                            progress = 0f,
                            feedbackMessage = "⚠️ Incorrect gesture! Tilted head DOWN instead of UP"
                        )
                    )
                } else {
                    val progress = (pitch.coerceAtLeast(0f) / targetThreshold).coerceIn(0.0f, 1.0f)
                    onChallengeUpdated(
                        currentChallengeIndex,
                        MotionChallengeStatus(
                            challenge = challenge,
                            isCompleted = false,
                            progress = progress,
                            feedbackMessage = "Tilt head slightly upwards"
                        )
                    )
                }
            }

            MotionChallengeType.NOD_DOWN -> {
                val pitch = face.headEulerAngleX // Pitch: <0 is down, >0 is up
                val targetThreshold = 10.0f

                if (pitch < -targetThreshold) {
                    simulateCurrentChallengeSuccess()
                } else if (pitch > 8.0f) {
                    // User tilted head UP instead of DOWN!
                    incorrectGestureFrameCount++
                    onChallengeUpdated(
                        currentChallengeIndex,
                        MotionChallengeStatus(
                            challenge = challenge,
                            isCompleted = false,
                            progress = 0f,
                            feedbackMessage = "⚠️ Incorrect gesture! Tilted head UP instead of DOWN"
                        )
                    )
                } else {
                    val progress = (abs(pitch.coerceAtMost(0f)) / targetThreshold).coerceIn(0.0f, 1.0f)
                    onChallengeUpdated(
                        currentChallengeIndex,
                        MotionChallengeStatus(
                            challenge = challenge,
                            isCompleted = false,
                            progress = progress,
                            feedbackMessage = "Tilt head slightly downwards"
                        )
                    )
                }
            }
        }

        if (incorrectGestureFrameCount > 35) {
            failCurrentChallenge("Motion Challenge Failed: Repeatedly performed incorrect gesture for ${challenge.title}.")
        }
    }

    fun close() {
        try {
            detector.close()
        } catch (e: Exception) {
            // Ignored
        }
    }
}
