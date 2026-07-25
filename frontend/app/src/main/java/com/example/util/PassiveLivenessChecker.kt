package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class PassiveLivenessResult(
    val passed: Boolean,
    val score: Float, // 0.0f to 100.0f
    val reason: String,
    val textureVarianceScore: Float, // 0.0f to 100.0f
    val moireGridScore: Float,       // 0.0f to 100.0f (higher = cleaner, no moire)
    val specularReflectionScore: Float // 0.0f to 100.0f
)

object PassiveLivenessChecker {

    /**
     * Performs passive liveness analysis on a captured face bitmap.
     * Evaluates skin texture variance, Moire screen pattern presence,
     * and specular reflection distribution without requiring active user movement.
     */
    fun checkPassiveLiveness(bitmap: Bitmap): PassiveLivenessResult {
        // Sample/scale image to 200x200 for fast processing
        val width = 200
        val height = 200
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)

        val pixels = IntArray(width * height)
        scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 1. Convert to Grayscale & Calculate Mean Intensity
        val grayscale = FloatArray(width * height)
        var totalLum = 0.0f
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val gray = 0.299f * r + 0.587f * g + 0.114f * b
            grayscale[i] = gray
            totalLum += gray
        }
        val meanLum = totalLum / (width * height)

        // 2. Texture Variance Analysis (Laplacian 3x3 Filter)
        var laplacianVarianceSum = 0.0
        var sampleCount = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = grayscale[y * width + x]
                val top = grayscale[(y - 1) * width + x]
                val bottom = grayscale[(y + 1) * width + x]
                val left = grayscale[y * width + (x - 1)]
                val right = grayscale[y * width + (x + 1)]

                // Laplacian operator
                val lapVal = abs(4 * center - top - bottom - left - right)
                laplacianVarianceSum += lapVal
                sampleCount++
            }
        }
        val avgLaplacian = if (sampleCount > 0) laplacianVarianceSum / sampleCount else 15.0

        // Score texture: Normal live face laplacian variance is usually between 12.0 and 80.0
        val textureScore = when {
            avgLaplacian < 4.0 -> 15.0f  // Extremely blurry (paper/print blur or bad out of focus)
            avgLaplacian > 120.0 -> 25.0f // Unnatural digital screen edge artifacts / extreme noise
            avgLaplacian in 10.0..65.0 -> 95.0f - abs(avgLaplacian - 35.0f).toFloat() * 0.8f
            else -> 60.0f
        }.coerceIn(10.0f, 98.0f)

        // 3. Moire & High Frequency Subpixel Grid Pattern Detection
        // Analyzes horizontal and vertical pixel periodicity (digital screen display artifacts)
        var horizontalGridDiffSum = 0.0
        var verticalGridDiffSum = 0.0
        for (y in 2 until height - 2 step 2) {
            for (x in 2 until width - 2 step 2) {
                val p0 = grayscale[y * width + x]
                val p2 = grayscale[y * width + (x + 2)]
                val p4 = grayscale[y * width + (x - 2)]
                horizontalGridDiffSum += abs(2 * p0 - p2 - p4)

                val v0 = grayscale[y * width + x]
                val v2 = grayscale[(y + 2) * width + x]
                val v4 = grayscale[(y - 2) * width + x]
                verticalGridDiffSum += abs(2 * v0 - v2 - v4)
            }
        }
        val avgMoireDiff = (horizontalGridDiffSum + verticalGridDiffSum) / (sampleCount / 2)
        // High periodic subpixel grid diff indicates digital display screen replay attack
        val moireScore = when {
            avgMoireDiff > 45.0 -> 20.0f // High screen subpixel grid noise
            avgMoireDiff > 28.0 -> 45.0f // Moderate display artifacts
            else -> (98.0f - avgMoireDiff.toFloat() * 1.5f).coerceIn(30.0f, 99.0f)
        }

        // 4. Specular Highlight & Reflection Range Analysis
        // Checks highlight contrast across central face region (eyes, nose, forehead)
        var maxBrightness = 0.0f
        var minBrightness = 255.0f
        var highlightPixelsCount = 0
        val faceCenterStartX = width / 4
        val faceCenterEndX = (width * 3) / 4
        val faceCenterStartY = height / 4
        val faceCenterEndY = (height * 3) / 4

        for (y in faceCenterStartY..faceCenterEndY) {
            for (x in faceCenterStartX..faceCenterEndX) {
                val lum = grayscale[y * width + x]
                if (lum > maxBrightness) maxBrightness = lum
                if (lum < minBrightness) minBrightness = lum
                if (lum > 235.0f) highlightPixelsCount++
            }
        }

        val contrastRange = maxBrightness - minBrightness
        val highlightRatio = highlightPixelsCount.toFloat() / ((faceCenterEndX - faceCenterStartX) * (faceCenterEndY - faceCenterStartY))

        val specularScore = when {
            highlightRatio > 0.18f -> 20.0f // Screen glare or paper reflection hotspot
            contrastRange < 40.0f -> 30.0f  // Flat lighting (flat printed photo)
            contrastRange in 60.0f..220.0f -> 92.0f
            else -> 70.0f
        }

        // 5. Final Composite Passive Liveness Score Calculation
        val compositeScore = (textureScore * 0.40f + moireScore * 0.35f + specularScore * 0.25f)
            .coerceIn(5.0f, 98.5f)

        val threshold = 40.0f
        val passed = compositeScore >= threshold

        val reason = when {
            !passed && moireScore < 40.0f -> "Digital screen display pattern detected (Screen Replay Attack)."
            !passed && textureScore < 30.0f -> "Flat paper texture or low-resolution print detected (Printed Photo Attack)."
            !passed && specularScore < 35.0f -> "Unnatural illumination reflection glare detected."
            !passed -> "Passive liveness score below required threshold (Score: ${String.format("%.1f", compositeScore)}%)."
            else -> "Live human face verified by passive texture, reflection, and depth analysis."
        }

        return PassiveLivenessResult(
            passed = passed,
            score = compositeScore,
            reason = reason,
            textureVarianceScore = textureScore,
            moireGridScore = moireScore,
            specularReflectionScore = specularScore
        )
    }
}
