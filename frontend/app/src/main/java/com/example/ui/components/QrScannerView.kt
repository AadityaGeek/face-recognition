package com.example.ui.components

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.UserEntity
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@Composable
fun AndroidQrCameraPreview(
    modifier: Modifier = Modifier,
    onQrDecoded: (String) -> Unit,
    onPreviewError: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                } else {
                    cameraProviderRef?.unbindAll()
                }
            } catch (e: Exception) {
                Log.e("QrScannerPreview", "Error unbinding camera on dispose", e)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProviderRef = cameraProvider
                    
                    val preview = Preview.Builder().build().apply {
                        surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                        .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val scanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        if (rawValue != null) {
                                            onQrDecoded(rawValue)
                                            break
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("QrScanner", "Barcode scanning failed", e)
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("QrScannerPreview", "Camera binding failed", e)
                    onPreviewError()
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerView(
    modifier: Modifier = Modifier,
    registeredUsers: List<UserEntity> = emptyList(),
    onQrDecoded: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "qr_scanner")

    // Vertical sweep laser position
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_sweep"
    )

    // Scanner frame pulse intensity
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInBack),
            repeatMode = RepeatMode.Reverse
        ),
        label = "frame_pulse"
    )

    var useSimulatedScanner by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    // Trigger permission request if they use real scanner
    LaunchedEffect(useSimulatedScanner) {
        if (!useSimulatedScanner && !cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    val activeCameraMode = !useSimulatedScanner && cameraPermissionState.status.isGranted

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Text Instructions
        Text(
            text = "SCAN IDENTITY QR CODE",
            style = MaterialTheme.fontFamilyPairBold(16),
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Align the credential QR code within the focus reticle below to decode the unique biometric User ID.",
            style = MaterialTheme.fontFamilyPairMedium(13),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Scanner Viewport Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (activeCameraMode) {
                // Real back camera QR scanning preview
                AndroidQrCameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onQrDecoded = onQrDecoded,
                    onPreviewError = {
                        useSimulatedScanner = true
                    }
                )
            }

            // Central QR code finder box and sweeping holographic green laser overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw technical grid lines
                val cols = 10
                val colW = width / cols
                val rows = 10
                val rowH = height / rows

                for (i in 0..cols) {
                    drawLine(
                        color = Color(0xFF0EA5E9).copy(alpha = 0.04f),
                        start = Offset(i * colW, 0f),
                        end = Offset(i * colW, height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                for (j in 0..rows) {
                    drawLine(
                        color = Color(0xFF0EA5E9).copy(alpha = 0.04f),
                        start = Offset(0f, j * rowH),
                        end = Offset(width, j * rowH),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw central QR code finder box
                val boxSize = Math.min(width, height) * 0.6f * pulseScale
                val boxLeft = (width - boxSize) / 2f
                val boxTop = (height - boxSize) / 2f

                // 1. Draw outer viewport border with 24dp rounded corners to ensure visibility
                val outerStrokeW = 2.dp.toPx()
                drawRoundRect(
                    color = Color(0xFF0EA5E9),
                    topLeft = Offset(outerStrokeW / 2f, outerStrokeW / 2f),
                    size = Size(width - outerStrokeW, height - outerStrokeW),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                    style = Stroke(width = outerStrokeW)
                )

                // 2. Define the path for the translucent background (everything except the inner focus cutout)
                val outerRect = androidx.compose.ui.geometry.RoundRect(
                    left = 0f,
                    top = 0f,
                    right = width,
                    bottom = height,
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
                val innerRect = androidx.compose.ui.geometry.RoundRect(
                    left = boxLeft,
                    top = boxTop,
                    right = boxLeft + boxSize,
                    bottom = boxTop + boxSize,
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
                
                val cutoutPath = androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(outerRect)
                    addRoundRect(innerRect)
                    fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                }

                // Draw the translucent background overlay
                drawPath(
                    path = cutoutPath,
                    color = Color.Black.copy(alpha = if (activeCameraMode) 0.35f else 0.65f)
                )

                // 3. Re-draw outer outline border for QR focus zone
                drawRoundRect(
                    color = Color(0xFF0EA5E9).copy(alpha = 0.4f),
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxSize, boxSize),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Draw high-visibility bracket corners
                val cornerLen = 24.dp.toPx()
                val strokeW = 4.dp.toPx()
                val cColor = Color(0xFF0EA5E9)

                // Top-Left corner
                drawLine(cColor, Offset(boxLeft, boxTop), Offset(boxLeft + cornerLen, boxTop), strokeW)
                drawLine(cColor, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + cornerLen), strokeW)

                // Top-Right corner
                drawLine(cColor, Offset(boxLeft + boxSize, boxTop), Offset(boxLeft + boxSize - cornerLen, boxTop), strokeW)
                drawLine(cColor, Offset(boxLeft + boxSize, boxTop), Offset(boxLeft + boxSize, boxTop + cornerLen), strokeW)

                // Bottom-Left corner
                drawLine(cColor, Offset(boxLeft, boxTop + boxSize), Offset(boxLeft + cornerLen, boxTop + boxSize), strokeW)
                drawLine(cColor, Offset(boxLeft, boxTop + boxSize), Offset(boxLeft, boxTop + boxSize - cornerLen), strokeW)

                // Bottom-Right corner
                drawLine(cColor, Offset(boxLeft + boxSize, boxTop + boxSize), Offset(boxLeft + boxSize - cornerLen, boxTop + boxSize), strokeW)
                drawLine(cColor, Offset(boxLeft + boxSize, boxTop + boxSize), Offset(boxLeft + boxSize, boxTop + boxSize - cornerLen), strokeW)

                // Sweeping holographic green laser
                val laserYPos = boxTop + (boxSize * laserY)
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF22C55E),
                            Color.Transparent
                        )
                    ),
                    start = Offset(boxLeft, laserYPos - 4.dp.toPx()),
                    end = Offset(boxLeft + boxSize, laserYPos + 4.dp.toPx()),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Tech icon and scanner overlay UI
            if (!activeCameraMode) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scanning...",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "ALIGN QR CODE",
                        style = MaterialTheme.fontFamilyPairBold(11),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            // Camera mode toggle floating button removed to prevent clutter and keep camera direct
        }

    }
}
