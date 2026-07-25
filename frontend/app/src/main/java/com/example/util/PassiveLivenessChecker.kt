package com.example.util

import android.graphics.Bitmap

data class PassiveLivenessResult(
    val passed: Boolean,
    val score: Float,
    val reason: String,
    val textureVarianceScore: Float,
    val moireGridScore: Float,
    val specularReflectionScore: Float
)

/**
 * Utility for passive liveness and anti-spoofing checks.
 * Code implementation hidden for open-source repository preview.
 */
object PassiveLivenessChecker {
    fun checkPassiveLiveness(bitmap: Bitmap): PassiveLivenessResult {
        TODO("Implementation hidden")
    }
}
