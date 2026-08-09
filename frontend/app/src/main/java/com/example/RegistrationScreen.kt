package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.LivenessViewModel
import com.example.ui.RegistrationState
import com.example.ui.components.CameraView
import com.example.ui.theme.fontFamilyPairBold
import com.example.ui.theme.fontFamilyPairMedium
import com.example.util.QrCodeGenerator
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BiometricScanAnimation(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BiometricScan")
    
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserY"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )

    Box(
        modifier = modifier
            .size(150.dp)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = (1.25f - pulseScale).coerceIn(0f, 1f) * 0.35f
                }
                .background(color.copy(alpha = 0.15f), shape = CircleShape)
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeWidth = 3.dp.toPx()
            val bracketLength = 24.dp.toPx()

            drawPath(
                path = Path().apply {
                    moveTo(0f, bracketLength)
                    lineTo(0f, 0f)
                    lineTo(bracketLength, 0f)
                },
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawPath(
                path = Path().apply {
                    moveTo(w - bracketLength, 0f)
                    lineTo(w, 0f)
                    lineTo(w, bracketLength)
                },
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawPath(
                path = Path().apply {
                    moveTo(0f, h - bracketLength)
                    lineTo(0f, h)
                    lineTo(bracketLength, h)
                },
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawPath(
                path = Path().apply {
                    moveTo(w - bracketLength, h)
                    lineTo(w, h)
                    lineTo(w, h - bracketLength)
                },
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            val currentLaserY = h * laserY
            
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        color.copy(alpha = 0.12f),
                        color.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    startY = (currentLaserY - 24.dp.toPx()).coerceAtLeast(0f),
                    endY = (currentLaserY + 24.dp.toPx()).coerceAtMost(h)
                ),
                topLeft = Offset(0f, (currentLaserY - 24.dp.toPx()).coerceAtLeast(0f)),
                size = Size(w, 48.dp.toPx())
            )

            drawLine(
                color = color,
                start = Offset(0f, currentLaserY),
                end = Offset(w, currentLaserY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = "Real-time Scanner",
            tint = color.copy(alpha = alphaPulse),
            modifier = Modifier.size(80.dp)
        )
    }
}


@Composable
fun RegistrationScreen(
    viewModel: LivenessViewModel,
    onGoToVerify: (String) -> Unit
) {
    DisposableEffect(Unit) {
        viewModel.setRegistrationActive(true)
        onDispose {
            viewModel.setRegistrationActive(false)
        }
    }

    val registrationState by viewModel.registrationState.collectAsState()
    val name by viewModel.regName.collectAsState()

    BackHandler(enabled = registrationState !is RegistrationState.Form) {
        if (registrationState is RegistrationState.PhotoCapture) {
            viewModel.cancelPhotoCapture()
        } else {
            viewModel.resetRegistrationForm()
        }
    }
    val age by viewModel.regAge.collectAsState()
    val userId by viewModel.regUserId.collectAsState()
    val regUserIdExists by viewModel.regUserIdExists.collectAsState()
    val photo = viewModel.regCapturedPhoto.collectAsState().value
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            
            // 1. Check file size
            var fileSize: Long = -1
            var displayName: String? = null
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                try {
                    contentResolver.openAssetFileDescriptor(uri, "r")?.use { fd ->
                        fileSize = fd.length
                    }
                } catch (ex: Exception) {
                    // ignore
                }
            }

            // 2. Check mime type and extension
            val mimeType = contentResolver.getType(uri) ?: ""
            val extension = displayName?.substringAfterLast('.', "")?.lowercase(Locale.ROOT) ?: ""

            val isValidType = mimeType.startsWith("image/jpeg") || 
                              mimeType.startsWith("image/png") || 
                              mimeType.startsWith("image/webp") ||
                              extension == "jpg" || 
                              extension == "jpeg" || 
                              extension == "png" || 
                              extension == "webp"

            if (!isValidType) {
                Toast.makeText(context, "Error: Invalid file type. Please upload a JPEG, PNG, or WEBP image.", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            // Limit is 5.00 MB
            val maxBytes = 5 * 1024 * 1024
            if (fileSize > maxBytes) {
                val formattedSize = String.format(Locale.ROOT, "%.2f", fileSize.toDouble() / (1024 * 1024))
                Toast.makeText(context, "Error: File too large ($formattedSize MB). Limit is 5.00 MB.", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            // 3. Load Bitmap safely
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }

                if (bitmap != null) {
                    viewModel.setRegistrationPhoto(bitmap)
                    Toast.makeText(context, "Photo uploaded successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error: Could not decode image file.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error decoding image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Crossfade(targetState = registrationState, label = "reg_screens") { state ->
        when (state) {
            is RegistrationState.Form -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Header Illustration Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "CREATE USER PROFILE",
                                    style = MaterialTheme.fontFamilyPairBold(15),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Fill in your profile details and capture a face photo to complete registration.",
                                    style = MaterialTheme.fontFamilyPairMedium(12),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val cleanName = name.trim().replace(Regex("\\s+"), " ")
                    val isNameAlphabetOnly = name.isNotEmpty() && name.all { it.isLetter() || it.isWhitespace() }
                    val isNameValid = isNameAlphabetOnly && cleanName.length in 2..50
                    val ageInt = age.trim().toIntOrNull()
                    val isAgeValid = ageInt != null && ageInt in 1..120
                    val cleanId = userId.trim()
                    val isUserIdFormatValid = cleanId.length in 3..30 && cleanId.matches(Regex("^[a-zA-Z0-9_\\-]+$"))
                    val isFormValid = isNameValid && isAgeValid && isUserIdFormatValid && !regUserIdExists && photo != null

                    // Text Input Fields
                    OutlinedTextField(
                        value = name,
                        onValueChange = { input ->
                            // Filter input so only alphabets and spaces are permitted
                            viewModel.regName.value = input.filter { it.isLetter() || it.isWhitespace() }
                        },
                        label = { Text("Full Name") },
                        placeholder = { Text("e.g. John Doe") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text
                        ),
                        isError = name.isNotEmpty() && !isNameValid,
                        supportingText = {
                            if (name.isNotEmpty()) {
                                if (!isNameAlphabetOnly) {
                                    Text("Name can only contain alphabets and spaces", color = MaterialTheme.colorScheme.error)
                                } else if (cleanName.length < 2 || cleanName.length > 50) {
                                    Text("Name must be between 2 and 50 characters", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        trailingIcon = {
                            if (name.isNotEmpty()) {
                                IconButton(onClick = { viewModel.regName.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear name")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = age,
                        onValueChange = { viewModel.regAge.value = it },
                        label = { Text("Age") },
                        placeholder = { Text("e.g. 28") },
                        singleLine = true,
                        isError = age.isNotEmpty() && !isAgeValid,
                        supportingText = {
                            if (age.isNotEmpty() && !isAgeValid) {
                                Text("Age must be a valid number between 1 and 120", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        trailingIcon = {
                            if (age.isNotEmpty()) {
                                IconButton(onClick = { viewModel.regAge.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear age")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = userId,
                        onValueChange = { viewModel.regUserId.value = it },
                        label = { Text("Unique User ID") },
                        isError = (userId.isNotEmpty() && !isUserIdFormatValid) || regUserIdExists,
                        supportingText = {
                            if (userId.isNotEmpty() && !isUserIdFormatValid) {
                                Text("3–30 characters (letters, numbers, hyphens, underscores)", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = {
                                viewModel.regUserId.value = "USR-${(10000..99999).random()}"
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Generate ID")
                            }
                        }
                    )

                    if (cleanId.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (regUserIdExists || !isUserIdFormatValid) Icons.Default.Cancel else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (regUserIdExists || !isUserIdFormatValid) MaterialTheme.colorScheme.error else Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (regUserIdExists) "User ID already registered! Choose another." else if (!isUserIdFormatValid) "Invalid User ID format." else "User ID is available for registration",
                                style = MaterialTheme.fontFamilyPairMedium(11),
                                color = if (regUserIdExists || !isUserIdFormatValid) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                            )
                        }
                    }

                    // Biometric Image Capture Area
                    Text(
                        text = "BIOMETRIC PHOTO CAPTURE",
                        style = MaterialTheme.fontFamilyPairBold(11),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    if (photo == null) {
                        // Empty Capture Area
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(130.dp)
                                    .clickable { viewModel.startRegistrationPhotoCapture() },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Capture via webcam stream",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Webcam Stream",
                                        style = MaterialTheme.fontFamilyPairBold(13),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Take JPEG Frame",
                                        style = MaterialTheme.fontFamilyPairMedium(11),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(130.dp)
                                    .clickable { filePickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = "Upload Photo file",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Upload File",
                                        style = MaterialTheme.fontFamilyPairBold(13),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "JPEG / PNG",
                                        style = MaterialTheme.fontFamilyPairMedium(11),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        // Display captured photo
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Image(
                                    bitmap = photo.asImageBitmap(),
                                    contentDescription = "Captured Biometric Frame",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { viewModel.regCapturedPhoto.value = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove photo",
                                        tint = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(vertical = 4.dp, horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = "Ready for registration submission",
                                        style = MaterialTheme.fontFamilyPairMedium(12),
                                        color = Color.White
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.startRegistrationPhotoCapture() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retake", style = MaterialTheme.fontFamilyPairBold(12))
                                }

                                OutlinedButton(
                                    onClick = { filePickerLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Upload New", style = MaterialTheme.fontFamilyPairBold(12))
                                }
                            }
                        }
                    }

                    // Register Submit Button
                    Button(
                        onClick = { viewModel.submitRegistration() },
                        enabled = isFormValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Complete Registration",
                            style = MaterialTheme.fontFamilyPairBold(14)
                        )
                    }
                }
            }
            is RegistrationState.PhotoCapture -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WEBCAM LIVENESS FEED",
                            style = MaterialTheme.fontFamilyPairBold(14),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { viewModel.cancelPhotoCapture() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Camera")
                        }
                    }

                    // Interactive camera viewfinder
                    CameraView(
                        modifier = Modifier.weight(1f),
                        showCaptureButton = true,
                        onPhotoCaptured = { bitmap ->
                            viewModel.setRegistrationPhoto(bitmap)
                        }
                    )
                }
            }
            is RegistrationState.Uploading -> {
                val animatedProgress by animateFloatAsState(
                    targetValue = state.progress,
                    animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
                    label = "RegistrationProgress"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0284C7))
                                )
                                Text(
                                    text = "MULTIPART TRANSMISSION",
                                    style = MaterialTheme.fontFamilyPairBold(11),
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            }

                            BiometricScanAnimation(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(160.dp)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${(animatedProgress * 100).toInt()}% Complete",
                                    style = MaterialTheme.fontFamilyPairBold(16),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = state.stage,
                                    style = MaterialTheme.fontFamilyPairBold(14),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Payload Type", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("multipart/form-data", style = MaterialTheme.fontFamilyPairBold(11), color = MaterialTheme.colorScheme.onSurface)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("File Parameter", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("face_image (JPEG)", style = MaterialTheme.fontFamilyPairBold(11), color = MaterialTheme.colorScheme.onSurface)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Connection Status", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("UPLINK STREAMING", style = MaterialTheme.fontFamilyPairBold(11), color = Color(0xFF0284C7))
                                }
                            }
                        }
                    }
                }
            }
            is RegistrationState.DuplicateError -> {
                val isConnectionError = state.message.contains("502") || 
                                       state.message.contains("503") || 
                                       state.message.contains("Connection error") || 
                                       state.message.contains("timeout") || 
                                       state.message.contains("ConnectException") ||
                                       state.message.contains("failed to respond") ||
                                       state.message.contains("Unable to reach")

                val isRealDuplicate = state.message.lowercase().contains("duplicate") || 
                                     state.message.lowercase().contains("exists") || 
                                     state.message.lowercase().contains("already") ||
                                     state.message.lowercase().contains("conflict")

                val amberColor = Color(0xFFF59E0B)
                val redColor = Color(0xFFEF4444)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isConnectionError) {
                        // Dedicated Backend Connection Issue Card
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.5.dp, amberColor.copy(alpha = 0.6f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(amberColor.copy(alpha = 0.12f))
                                        .border(2.dp, amberColor.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = amberColor,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = amberColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, amberColor.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "SERVER CONNECTION ISSUE",
                                        style = MaterialTheme.fontFamilyPairBold(12),
                                        color = amberColor,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }

                                Text(
                                    text = "We were unable to reach the biometric verification server. Please make sure the backend server is running and your device is connected to the internet.",
                                    style = MaterialTheme.fontFamilyPairMedium(13),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = amberColor.copy(alpha = 0.05f)),
                                    border = BorderStroke(1.dp, amberColor.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("• Check that your device has active Wi-Fi or Mobile Data.", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurface)
                                        Text("• Ensure the server address in App Settings is correct.", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.resetRegistrationForm() },
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Dismiss", style = MaterialTheme.fontFamilyPairBold(13))
                                    }
                                    Button(
                                        onClick = { viewModel.startRegistrationPhotoCapture() },
                                        colors = ButtonDefaults.buttonColors(containerColor = amberColor),
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry Connection", style = MaterialTheme.fontFamilyPairBold(13), color = Color.White)
                                    }
                                }
                            }
                        }
                    } else {
                        // Dedicated Registration Failure / Duplicate Card
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.5.dp, redColor.copy(alpha = 0.6f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    IconButton(
                                        onClick = { viewModel.resetRegistrationForm() },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(redColor.copy(alpha = 0.12f))
                                        .border(2.dp, redColor.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isRealDuplicate) Icons.Default.Warning else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = redColor,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = redColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, redColor.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = if (isRealDuplicate) "DUPLICATE BIOMETRIC PROFILE" else "REGISTRATION UNSUCCESSFUL",
                                        style = MaterialTheme.fontFamilyPairBold(12),
                                        color = redColor,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }

                                Text(
                                    text = if (isRealDuplicate) 
                                        "This face profile is already registered under a different User ID. Each person can only be registered once."
                                    else 
                                        "We could not complete your registration. Please ensure your face is clearly visible and retake the photo.",
                                    style = MaterialTheme.fontFamilyPairMedium(13),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = redColor.copy(alpha = 0.05f)),
                                    border = BorderStroke(1.dp, redColor.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("• Make sure your face is well-lit and directly facing the camera.", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurface)
                                        Text("• Avoid dark shadows, hats, or sunglasses covering your features.", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.resetRegistrationForm() },
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cancel", style = MaterialTheme.fontFamilyPairBold(13))
                                    }

                                    Button(
                                        onClick = { viewModel.startRegistrationPhotoCapture() },
                                        colors = ButtonDefaults.buttonColors(containerColor = redColor),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retake", style = MaterialTheme.fontFamilyPairBold(13))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is RegistrationState.Success -> {
                val greenColor = Color(0xFF10B981)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.5.dp, greenColor.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                IconButton(
                                    onClick = { viewModel.resetRegistrationForm() },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(greenColor.copy(alpha = 0.12f))
                                    .border(2.dp, greenColor.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = greenColor,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = greenColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, greenColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "✓ REGISTRATION COMPLETED",
                                    style = MaterialTheme.fontFamilyPairBold(12),
                                    color = greenColor,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }

                            Text(
                                text = "Profile registered successfully! Here is your identity QR Code.",
                                style = MaterialTheme.fontFamilyPairMedium(13),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "IDENTITY QR CODE",
                                    style = MaterialTheme.fontFamilyPairBold(12),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // QR Render Card
                            val qrBitmap = remember(state.base64QrCode) {
                                QrCodeGenerator.base64ToBitmap(state.base64QrCode)
                            }

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(240.dp)
                                    .border(
                                        1.5.dp,
                                        greenColor.copy(alpha = 0.4f),
                                        RoundedCornerShape(20.dp)
                                    ),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (qrBitmap != null) {
                                        Image(
                                            bitmap = qrBitmap.asImageBitmap(),
                                            contentDescription = "Decoded QR credential",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }

                            // DOMINANT USER IDENTITY PROFILE CARD
                            val nameValue = name.trim()
                            val ageValue = age.trim()
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (nameValue.isNotEmpty()) nameValue else "Enrolled User",
                                        style = MaterialTheme.fontFamilyPairBold(20),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Badge,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "ID: ${state.userId}",
                                                    style = MaterialTheme.fontFamilyPairBold(12),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Cake,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Age: ${if (ageValue.isNotEmpty()) ageValue else "N/A"}",
                                                    style = MaterialTheme.fontFamilyPairBold(12),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Registration Profile Details Breakdown
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, greenColor.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "REGISTRATION RECORD DETAILS",
                                        style = MaterialTheme.fontFamilyPairBold(11),
                                        color = greenColor,
                                        letterSpacing = 0.5.sp
                                    )
                                    HorizontalDivider(color = greenColor.copy(alpha = 0.15f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("User ID", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(state.userId, style = MaterialTheme.fontFamilyPairBold(12), color = MaterialTheme.colorScheme.onSurface)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Full Name", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(if (nameValue.isNotEmpty()) nameValue else "N/A", style = MaterialTheme.fontFamilyPairBold(12), color = MaterialTheme.colorScheme.onSurface)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Age", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(if (ageValue.isNotEmpty()) ageValue else "N/A", style = MaterialTheme.fontFamilyPairBold(12), color = MaterialTheme.colorScheme.onSurface)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Anti-Spoof Check", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("PASSED", style = MaterialTheme.fontFamilyPairBold(12), color = greenColor)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Registered At", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        val formattedTime = remember(Unit) {
                                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                                        }
                                        Text(formattedTime, style = MaterialTheme.fontFamilyPairBold(12), color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // Action buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        shareQrCodeImage(context, qrBitmap, state.userId)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share QR Code", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share QR", style = MaterialTheme.fontFamilyPairBold(13))
                                }

                                Button(
                                    onClick = { onGoToVerify(state.userId) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Verify Identity", style = MaterialTheme.fontFamilyPairBold(13))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetRegistrationForm() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Register Another Profile", style = MaterialTheme.fontFamilyPairBold(13))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun shareQrCodeImage(context: Context, qrBitmap: Bitmap?, userId: String) {
    if (qrBitmap == null) {
        Toast.makeText(context, "QR code image not available", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val cacheDir = File(context.cacheDir, "qr_codes")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val cleanUserId = userId.trim().ifEmpty { "qr_code" }
        // Save QR code image file using the user ID as filename: <userId>.png
        val file = File(cacheDir, "$cleanUserId.png")
        val stream = FileOutputStream(file)
        qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "QR Code Credential for $cleanUserId")
            putExtra(Intent.EXTRA_TEXT, "Identity QR Code for User ID: $cleanUserId")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share QR Code ($cleanUserId.png)")
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to share QR code: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
