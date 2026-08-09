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
    private val onAllChallengesCompleted: (bestCapturedFrame: Bitmap?) -> Unit
) {
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)

    private var currentChallengeIndex = 0
    private var isBlinkEyeClosedState = false
    private var isProcessingFrame = false
    private var challengeStartTimeMs = System.currentTimeMillis()
    private var incorrectGestureFrameCount = 0

    private var initialTrackingId: Int? = null
    private var bestFrameBitmap: Bitmap? = null
    private var bestFrameScore: Float = -1.0f

    private val CHALLENGE_TIMEOUT_MS = 15000L // 15 seconds per challenge

    fun getBestCapturedFrame(): Bitmap? = bestFrameBitmap

    fun reset() {
        currentChallengeIndex = 0
        isBlinkEyeClosedState = false
        isProcessingFrame = false
        challengeStartTimeMs = System.currentTimeMillis()
        incorrectGestureFrameCount = 0
        initialTrackingId = null
        bestFrameBitmap = null
        bestFrameScore = -1.0f
    }

    fun getCurrentChallenge(): MotionChallengeType? {
        return requiredChallenges.getOrNull(currentChallengeIndex)
    }

    private fun rotateBitmapHelper(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    @OptIn(ExperimentalGetImage::class)
    fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isProcessingFrame) {
            imageProxy.close()
            return
        }

        isProcessingFrame = true
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.size > 1) {
                    failCurrentChallenge("Multiple faces detected in frame. Only one person is allowed during verification.")
                } else if (faces.isNotEmpty()) {
                    val face = faces.first()
                    evaluateFaceForChallenge(face, imageProxy, rotationDegrees)
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
                if (faces.size > 1) {
                    failCurrentChallenge("Multiple faces detected in frame. Only one person is allowed during verification.")
                } else if (faces.isNotEmpty()) {
                    val face = faces.first()
                    evaluateFaceForChallenge(face, sourceBitmap = bitmap)
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
            onAllChallengesCompleted(getBestCapturedFrame())
        }
    }

    private fun evaluateFaceForChallenge(
        face: Face,
        imageProxy: ImageProxy? = null,
        rotationDegrees: Int = 0,
        sourceBitmap: Bitmap? = null
    ) {
        val challenge = getCurrentChallenge() ?: return

        // 1. Enforce Face Identity Continuity
        val currentTrackingId = face.trackingId
        if (currentTrackingId != null) {
            if (initialTrackingId == null) {
                initialTrackingId = currentTrackingId
            } else if (currentTrackingId != initialTrackingId) {
                failCurrentChallenge("Face Identity Swapped: A different person entered the camera view during verification.")
                return
            }
        }

        // 2. Best Frame Selection Buffer Scoring
        val yaw = abs(face.headEulerAngleY)
        val pitch = abs(face.headEulerAngleX)
        val roll = abs(face.headEulerAngleZ)
        val leftOpen = face.leftEyeOpenProbability ?: 0.5f
        val rightOpen = face.rightEyeOpenProbability ?: 0.5f

        if (yaw < 12.0f && pitch < 12.0f && roll < 12.0f && leftOpen > 0.40f && rightOpen > 0.40f) {
            val qualityScore = (100.0f - (yaw * 3.0f + pitch * 3.0f + roll * 2.0f)) + (leftOpen + rightOpen) * 20.0f
            if (qualityScore > bestFrameScore) {
                if (sourceBitmap != null) {
                    bestFrameScore = qualityScore
                    bestFrameBitmap = sourceBitmap.copy(sourceBitmap.config ?: Bitmap.Config.ARGB_8888, true)
                } else if (imageProxy != null) {
                    try {
                        val raw = try { imageProxy.toBitmap() } catch (e: Exception) { null }
                        if (raw != null) {
                            val rotated = if (rotationDegrees != 0) rotateBitmapHelper(raw, rotationDegrees) else raw
                            bestFrameScore = qualityScore
                            bestFrameBitmap = rotated
                        }
                    } catch (e: Exception) {
                        // Ignore decode error
                    }
                }
            }
        }

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
