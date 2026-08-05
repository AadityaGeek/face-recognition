package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.UserEntity
import com.example.ui.*
import com.example.ui.components.CameraView
import com.example.ui.components.QrScannerView
import com.example.ui.components.generateStylizedFaceBitmap
import com.google.accompanist.permissions.isGranted
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.util.QrCodeGenerator
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import android.graphics.ImageDecoder
import android.provider.MediaStore
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.data.ApiConfig.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val systemTheme = isSystemInDarkTheme()
            var darkTheme by remember { mutableStateOf(systemTheme) }
            MyApplicationTheme(darkTheme = darkTheme) {
                MainAppPortal(
                    isDarkTheme = darkTheme,
                    onThemeToggle = { darkTheme = !darkTheme }
                )
            }
        }
    }
}

enum class NavigationTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    REGISTER("Register", Icons.Default.PersonAdd),
    VERIFY("Verify", Icons.Default.Security),
    MORE("More", Icons.Default.MoreHoriz)
}

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val iconAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.65f) }
    val textAlpha = remember { Animatable(0f) }
    val devAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val job1 = launch { iconAlpha.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing)) }
        val job2 = launch { iconScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        val job3 = launch { textAlpha.animateTo(1f, animationSpec = tween(500)) }

        // Wait for logo & title entrance animations to finish loading
        job1.join()
        job2.join()
        job3.join()

        // Developer name quick blink animation at the bottom
        kotlinx.coroutines.delay(150)
        devAlpha.animateTo(1f, animationSpec = tween(250)) // Blink in
        kotlinx.coroutines.delay(600) // Brief highlight hold
        devAlpha.animateTo(0f, animationSpec = tween(250)) // Blink out
        kotlinx.coroutines.delay(150)

        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center Content: App Badge Icon & Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.graphicsLayer {
                alpha = iconAlpha.value
                scaleX = iconScale.value
                scaleY = iconScale.value
            }
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "App Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(60.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.graphicsLayer { alpha = textAlpha.value }
            ) {
                Text(
                    text = "Liveness Shield",
                    style = MaterialTheme.fontFamilyPairBold(24),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Biometric Registration & Identity Verification",
                    style = MaterialTheme.fontFamilyPairMedium(13),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Developer Attribution Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .graphicsLayer { alpha = devAlpha.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Developer: Aaditya",
                style = MaterialTheme.fontFamilyPairBold(14),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun MainAppPortal(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val viewModel: LivenessViewModel = viewModel()
    var showSplashScreen by remember { mutableStateOf(true) }
    var showLandingPage by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(NavigationTab.REGISTER) }
    val registeredUsers by viewModel.allUsers.collectAsState()
    val context = LocalContext.current

    val screenState = when {
        showSplashScreen -> "SPLASH"
        showLandingPage -> "LANDING"
        else -> "APP"
    }

    if (!showLandingPage && !showSplashScreen) {
        BackHandler {
            showLandingPage = true
        }
    }

    Crossfade(
        targetState = screenState,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "app_navigation_crossfade"
    ) { state ->
        when (state) {
            "SPLASH" -> {
                SplashScreen(
                    onSplashFinished = { showSplashScreen = false }
                )
            }
            "LANDING" -> {
                LandingPageScreen(
                    onRegisterClick = {
                        currentTab = NavigationTab.REGISTER
                        showLandingPage = false
                    },
                    onVerifyClick = {
                        currentTab = NavigationTab.VERIFY
                        showLandingPage = false
                    },
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle,
                    onMoreClick = {
                        currentTab = NavigationTab.MORE
                        showLandingPage = false
                    }
                )
            }
            else -> {
                Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = when (currentTab) {
                                    NavigationTab.REGISTER -> Icons.Default.PersonAdd
                                    NavigationTab.VERIFY -> Icons.Default.Security
                                    NavigationTab.MORE -> Icons.Default.MoreHoriz
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = when (currentTab) {
                                    NavigationTab.REGISTER -> "Biometric Registration"
                                    NavigationTab.VERIFY -> "Liveness Verification"
                                    NavigationTab.MORE -> "Services & Utilities"
                                },
                                style = MaterialTheme.fontFamilyPairBold(16),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val topThemeRotation by animateFloatAsState(
                                targetValue = if (isDarkTheme) 180f else 0f,
                                animationSpec = tween(350, easing = FastOutSlowInEasing),
                                label = "TopThemeRotation"
                            )
                            IconButton(
                                onClick = onThemeToggle,
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = topThemeRotation }
                                )
                            }

                            IconButton(
                                onClick = { showLandingPage = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Return to Landing Page",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationTab.values().forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                                label = { Text(text = tab.label, style = MaterialTheme.fontFamilyPairMedium(11)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                            (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> direction * (width / 5) } +
                                    fadeIn(animationSpec = tween(220)))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing)) { width -> -direction * (width / 5) } +
                                            fadeOut(animationSpec = tween(180))
                                )
                        },
                        label = "tab_transitions"
                    ) { tab ->
                        when (tab) {
                            NavigationTab.REGISTER -> {
                                RegistrationScreen(
                                    viewModel = viewModel,
                                    onGoToVerify = { scannedId ->
                                        viewModel.startVerificationFlow(scannedId)
                                        currentTab = NavigationTab.VERIFY
                                    }
                                )
                            }
                            NavigationTab.VERIFY -> {
                                VerificationScreen(
                                    viewModel = viewModel,
                                    onRegisterTrigger = {
                                        currentTab = NavigationTab.REGISTER
                                    }
                                )
                            }
                            NavigationTab.MORE -> {
                                MoreServicesScreen(
                                    onBackToHome = {
                                        showLandingPage = true
                                    },
                                    isDarkTheme = isDarkTheme,
                                    onThemeToggle = onThemeToggle
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun LandingPageScreen(
    onRegisterClick: () -> Unit,
    onVerifyClick: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onMoreClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val maxWidth = this.maxWidth
        val isWideScreen = maxWidth > 600.dp
        val contentPaddingHorizontal = if (isWideScreen) 32.dp else 20.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = contentPaddingHorizontal, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Theme & More Services Action Buttons at the Top Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onMoreClick,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "More Services & Utilities",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        val landingThemeRotation by animateFloatAsState(
                            targetValue = if (isDarkTheme) 180f else 0f,
                            animationSpec = tween(350, easing = FastOutSlowInEasing),
                            label = "LandingThemeRotation"
                        )
                        IconButton(
                            onClick = onThemeToggle,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer { rotationZ = landingThemeRotation }
                            )
                        }
                    }

                    // Brand Logo and Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = "Liveness Shield",
                            style = MaterialTheme.fontFamilyPairBold(26),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = "Biometric Identity Registration & Server-Verified Anti-Spoofing",
                        style = MaterialTheme.fontFamilyPairMedium(13),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Scanning Ring Illustration
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        BiometricScanningRing()
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(68.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Register Action Card
                    Card(
                        onClick = onRegisterClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "1. Register Identity Profile",
                                    style = MaterialTheme.fontFamilyPairBold(16),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Create secure digital credentials. Enroll your face reference with anti-spoofing verification and generate QR profile.",
                                    style = MaterialTheme.fontFamilyPairMedium(11),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Verify Action Card
                    Card(
                        onClick = onVerifyClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "2. Verify Liveness Scan",
                                    style = MaterialTheme.fontFamilyPairBold(16),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Conduct live camera spoof checks. Scan QR code or enter User ID to map biometric similarities instantly.",
                                    style = MaterialTheme.fontFamilyPairMedium(11),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Security / Stats Footnote
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Encrypted Transmission & Server Biometric Verification",
                            style = MaterialTheme.fontFamilyPairMedium(11),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoreServicesScreen(
    onBackToHome: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    var showBackendUrlDialog by remember { mutableStateOf(false) }
    var showFullAboutPage by remember { mutableStateOf(false) }

    BackHandler {
        if (showBackendUrlDialog) {
            showBackendUrlDialog = false
        } else if (showFullAboutPage) {
            showFullAboutPage = false
        } else {
            onBackToHome()
        }
    }

    AnimatedContent(
        targetState = showFullAboutPage,
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> width / 4 } +
                        fadeIn(animationSpec = tween(220)))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing)) { width -> -width / 4 } +
                                fadeOut(animationSpec = tween(180))
                    )
            } else {
                (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> -width / 4 } +
                        fadeIn(animationSpec = tween(220)))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing)) { width -> width / 4 } +
                                fadeOut(animationSpec = tween(180))
                    )
            }
        },
        label = "more_services_subpage_transition"
    ) { isAboutPage ->
        if (isAboutPage) {
            FullAboutPageScreen(onBack = { showFullAboutPage = false })
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val maxWidth = this.maxWidth
                val isWideScreen = maxWidth > 600.dp
                val contentPaddingHorizontal = if (isWideScreen) 32.dp else 18.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = contentPaddingHorizontal, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreHoriz,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Services & Utilities",
                                            style = MaterialTheme.fontFamilyPairBold(20),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Configure API endpoints, view app identity, and manage theme options",
                                            style = MaterialTheme.fontFamilyPairMedium(12),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Section Title
                        Text(
                            text = "AVAILABLE OPTIONS",
                            style = MaterialTheme.fontFamilyPairBold(12),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )

                        // Option List (Simple List Container)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column {
                                // Option 1: Backend Server URL
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showBackendUrlDialog = true }
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Dns,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "Backend Server URL",
                                        style = MaterialTheme.fontFamilyPairBold(14),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )

                                // Option 2: About Us & Application Details
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showFullAboutPage = true }
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "About Us & Application Details",
                                        style = MaterialTheme.fontFamilyPairBold(14),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )

                                // Option 3: Dark Theme Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "Dark Theme",
                                        style = MaterialTheme.fontFamilyPairBold(14),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = isDarkTheme,
                                        onCheckedChange = { onThemeToggle() }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = onBackToHome,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Home, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Back to Landing Screen", style = MaterialTheme.fontFamilyPairBold(14))
                        }
                    }
                }
            }
        }
    }
}

    // Dialogs
    if (showBackendUrlDialog) {
        BackendUrlConfigDialog(onDismiss = { showBackendUrlDialog = false })
    }
}



@Composable
fun FullAboutPageScreen(
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var isDevExpanded by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val maxWidth = this.maxWidth
        val isWideScreen = maxWidth > 600.dp
        val contentPaddingHorizontal = if (isWideScreen) 32.dp else 18.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = contentPaddingHorizontal, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Bar Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "About Liveness Shield",
                                style = MaterialTheme.fontFamilyPairBold(20),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Full application overview, team Sneekers, and developer profile",
                                style = MaterialTheme.fontFamilyPairMedium(12),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 1. App Identity Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Liveness Shield Biometrics",
                                        style = MaterialTheme.fontFamilyPairBold(16),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Version ${com.example.BuildConfig.VERSION_NAME} • Production Build",
                                        style = MaterialTheme.fontFamilyPairMedium(12),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Text(
                                text = "Liveness Shield is an enterprise-grade mobile identity application delivering real-time anti-spoofing facial verification, secure profile enrollment, and instant identity mapping.",
                                style = MaterialTheme.fontFamilyPairMedium(12),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Text(
                                text = "CORE APP CAPABILITIES",
                                style = MaterialTheme.fontFamilyPairBold(12),
                                color = MaterialTheme.colorScheme.primary
                            )

                            AboutBulletPoint(
                                title = "Digital Profile Registration",
                                description = "Streamlined user enrollment paired with instant QR credential generation."
                            )
                            AboutBulletPoint(
                                title = "Live Biometric Scan",
                                description = "Real-time face verification and blink detection protecting user identity endpoints."
                            )
                            AboutBulletPoint(
                                title = "Modern Compose Experience",
                                description = "Built with Jetpack Compose following Material 3 design principles with dynamic light and dark theme support."
                            )
                        }
                    }

                    // 2. Team Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Solution Sneekers",
                                        style = MaterialTheme.fontFamilyPairBold(16),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Mobile Engineering & Design Collective",
                                        style = MaterialTheme.fontFamilyPairMedium(12),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Text(
                                text = "Solution Sneekers is a software development group dedicated to crafting high-performance, user-centric mobile applications built with native Kotlin and Jetpack Compose.",
                                style = MaterialTheme.fontFamilyPairMedium(12),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )

                            AboutBulletPoint(
                                title = "User Privacy & Trust",
                                description = "Architecting applications with data protection and local encryption in mind."
                            )
                            AboutBulletPoint(
                                title = "Design & Usability",
                                description = "Prioritizing clean visuals, generous spacing, and accessibility across all screen sizes."
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        try { uriHandler.openUri("https://solutionsneekers.github.io") } catch (e: Exception) {}
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Lab Web: solutionsneekers.github.io",
                                    style = MaterialTheme.fontFamilyPairBold(13),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    // 3. Developer Profile Card (SLIGHTLY HIDDEN VIA EXPANDABLE ACCORDION)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { isDevExpanded = !isDevExpanded },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Lead Developer Profile",
                                        style = MaterialTheme.fontFamilyPairBold(16),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isDevExpanded) "Lead Developer Aaditya details" else "Tap to reveal developer credentials & links",
                                        style = MaterialTheme.fontFamilyPairMedium(12),
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Icon(
                                    imageVector = if (isDevExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle Developer Profile",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            if (isDevExpanded) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                Text(
                                    text = "Lead Developer: Aaditya",
                                    style = MaterialTheme.fontFamilyPairBold(14),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "Aaditya is a mobile software engineer specializing in modern Android architecture, Jetpack Compose UI systems, and biometric identity security.",
                                    style = MaterialTheme.fontFamilyPairMedium(12),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            try { uriHandler.openUri("https://github.com/AadityaGeek") } catch (e: Exception) {}
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = "GitHub",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "GitHub: @AadityaGeek",
                                        style = MaterialTheme.fontFamilyPairBold(13),
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            try { uriHandler.openUri("https://linkedin.com/in/aadityakr") } catch (e: Exception) {}
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Work,
                                        contentDescription = "LinkedIn",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "LinkedIn: linkedin.com/in/aadityakr",
                                        style = MaterialTheme.fontFamilyPairBold(13),
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back to Services & Utilities", style = MaterialTheme.fontFamilyPairBold(14))
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutBulletPoint(title: String, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.fontFamilyPairBold(12),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.fontFamilyPairMedium(11),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun DeveloperBadge(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.fontFamilyPairBold(10),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BiometricScanningRing() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = primaryColor.copy(alpha = 0.08f),
            radius = size.minDimension / 2f
        )
        drawCircle(
            color = primaryColor,
            radius = size.minDimension / 2f - 10f,
            style = Stroke(
                width = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            )
        )
        drawCircle(
            color = secondaryColor.copy(alpha = 0.3f),
            radius = size.minDimension / 2f - 25f,
            style = Stroke(width = 1.5f)
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
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
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
                val isRealDuplicate = state.message.lowercase().contains("duplicate") || 
                                     state.message.lowercase().contains("exists") || 
                                     state.message.lowercase().contains("already") ||
                                     state.message.lowercase().contains("conflict") ||
                                     state.message.lowercase().contains("multiple")

                val isImg1PathError = state.message.contains("img1_path", ignoreCase = true) || 
                                     state.message.contains("Face could not be detected", ignoreCase = true)
                val isOpenCvError = state.message.contains("CascadeClassifier", ignoreCase = true) || 
                                    state.message.contains("cv2", ignoreCase = true) || 
                                    state.message.contains("no attribute", ignoreCase = true)

                val friendlyMsg = if (isImg1PathError) {
                    "Backend Server Error: Face could not be detected in the profile photo by the server face recognition model."
                } else if (isOpenCvError) {
                    "Backend Server Error: OpenCV 'CascadeClassifier' module is missing or corrupt on the Python backend server."
                } else if (state.message.contains("Application failed to respond") || state.message.contains("502") || state.message.contains("503")) {
                    "The biometrics backend server is temporarily unavailable or offline. Please check your connection or retry."
                } else if (state.message.contains("Connection error") || state.message.contains("timeout") || state.message.contains("ConnectException")) {
                    "Unable to reach the biometrics backend. Please check your network connection."
                } else {
                    state.message
                }

                val redColor = Color(0xFFEF4444)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.5.dp, redColor.copy(alpha = 0.6f)),
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
                                    text = if (isRealDuplicate) "✕ BIOMETRIC DUPLICATE DETECTED" else "✕ REGISTRATION FAILED",
                                    style = MaterialTheme.fontFamilyPairBold(12),
                                    color = redColor,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }

                            Text(
                                text = friendlyMsg,
                                style = MaterialTheme.fontFamilyPairMedium(13),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = redColor.copy(alpha = 0.05f)
                                ),
                                border = BorderStroke(1.dp, redColor.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isImg1PathError) {
                                        Text("• Cause: Face not detected in photo during server registration.", style = MaterialTheme.fontFamilyPairBold(11), color = redColor)
                                        Text("• Solution: Re-capture with good lighting & direct camera alignment, or set enforce_detection=False in Python backend.", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurface)
                                    } else if (isOpenCvError) {
                                        Text("• Cause: OpenCV package conflict or broken installation in Python.", style = MaterialTheme.fontFamilyPairBold(11), color = redColor)
                                        Text("• Solution: Cleanly reinstall opencv-python in terminal:\npip uninstall opencv-python opencv-python-headless opencv-contrib-python -y\npip install opencv-python", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurface)
                                    } else if (isRealDuplicate) {
                                        Text("Security Protocol Rule: A duplicate biometric face fingerprint cannot be mapped to multiple credentials. Please update registration parameters or re-capture and submit again.", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                                    } else {
                                        Text("System Notice: Biometric registration requires a successful handshake with the identity ledger. Please check network telemetry or adjust similarity thresholds.", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.resetRegistrationForm() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Reset Form", style = MaterialTheme.fontFamilyPairBold(13))
                                }

                                Button(
                                    onClick = {
                                        viewModel.startRegistrationPhotoCapture()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = redColor
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 48.dp),
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
                                text = "The biometric embedding was successfully registered! Here is your cryptographic identity QR Code.",
                                style = MaterialTheme.fontFamilyPairMedium(13),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

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

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "Credentials bound to: ${state.userId}",
                                    style = MaterialTheme.fontFamilyPairBold(12),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
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
                                        Toast.makeText(context, "QR image saved to /Download/Credential-${state.userId}.png", Toast.LENGTH_LONG).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save QR", style = MaterialTheme.fontFamilyPairBold(13))
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

                            TextButton(
                                onClick = { viewModel.resetRegistrationForm() }
                            ) {
                                Text("Register Another Profile", style = MaterialTheme.fontFamilyPairBold(12))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerificationScreen(
    viewModel: LivenessViewModel,
    onRegisterTrigger: () -> Unit
) {
    val verificationState by viewModel.verificationState.collectAsState()
    val userIdInput by viewModel.verUserIdInput.collectAsState()
    val verUserFoundName by viewModel.verUserFoundName.collectAsState()
    val context = LocalContext.current
    var showScannerDialog by remember { mutableStateOf(false) }
    var showVerificationConfirmDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showScannerDialog || showVerificationConfirmDialog || verificationState !is VerificationState.Idle) {
        if (showScannerDialog) {
            showScannerDialog = false
        } else if (showVerificationConfirmDialog) {
            showVerificationConfirmDialog = false
        } else if (verificationState !is VerificationState.Idle) {
            viewModel.resetVerification()
        }
    }

    LaunchedEffect(showScannerDialog, verificationState) {
        if (showScannerDialog || verificationState is VerificationState.FaceCapture) {
            kotlinx.coroutines.delay(60000L)
            showScannerDialog = false
            viewModel.resetVerification()
            Toast.makeText(context, "Verification timed out due to inactivity.", Toast.LENGTH_LONG).show()
        }
    }

    Crossfade(targetState = verificationState, label = "verify_screens") { state ->
        when (state) {
            is VerificationState.Idle -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Header Card
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
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "SECURE VERIFICATION",
                                    style = MaterialTheme.fontFamilyPairBold(15),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Resolve User ID first, then execute a 3-second liveness check.",
                                    style = MaterialTheme.fontFamilyPairMedium(12),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Target User ID field with contextual QR Scan button
                    val registeredUsers by viewModel.allUsers.collectAsState()
                    val cleanVerUserId = userIdInput.trim()
                    val isVerUserIdValid = cleanVerUserId.length in 3..30 && cleanVerUserId.matches(Regex("^[a-zA-Z0-9_\\-]+$"))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = userIdInput,
                            onValueChange = { viewModel.verUserIdInput.value = it },
                            label = { Text("Target User ID") },
                            placeholder = { Text("e.g. USR-12345") },
                            singleLine = true,
                            isError = userIdInput.isNotEmpty() && !isVerUserIdValid,
                            supportingText = {
                                if (userIdInput.isNotEmpty() && !isVerUserIdValid) {
                                    Text("User ID must be 3–30 characters long", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                            trailingIcon = {
                                if (userIdInput.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.verUserIdInput.value = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear target user ID")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        IconButton(
                            onClick = { showScannerDialog = true },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR Identity Credential",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (showScannerDialog) {
                        AlertDialog(
                            onDismissRequest = { showScannerDialog = false },
                            title = {
                                Text(
                                    text = "Scan Identity Credential",
                                    style = MaterialTheme.fontFamilyPairBold(16),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            text = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .clip(RoundedCornerShape(16.dp))
                                ) {
                                    QrScannerView(
                                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                        registeredUsers = registeredUsers,
                                        onQrDecoded = { decodedId ->
                                            viewModel.verUserIdInput.value = decodedId
                                            showScannerDialog = false
                                            Toast.makeText(context, "Credential Resolved: $decodedId", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showScannerDialog = false }) {
                                    Text("Cancel", style = MaterialTheme.fontFamilyPairBold(14))
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Tips details card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Passive Liveness Protocol:", style = MaterialTheme.fontFamilyPairBold(12), color = MaterialTheme.colorScheme.primary)
                            }
                             Text(
                                "1. Scan a registered user's QR Code or enter the User ID above.\n" +
                                        "2. Tap 'Start Live Passive Capture' below to launch the camera feed.\n" +
                                        "3. Position face inside oval frame for automatic passive biometric scanning (texture, reflections & Moire pattern detection).\n" +
                                        "4. Verification and anti-spoofing score will process automatically without needing manual capture taps.",
                                style = MaterialTheme.fontFamilyPairMedium(11),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Confirmation Dialog before launching Live Camera Feed
                    if (showVerificationConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showVerificationConfirmDialog = false },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            title = {
                                Text(
                                    text = "Confirm Identity & Live Capture",
                                    style = MaterialTheme.fontFamilyPairBold(18),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Target User Credential:",
                                                style = MaterialTheme.fontFamilyPairBold(11),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = userIdInput.trim(),
                                                style = MaterialTheme.fontFamilyPairBold(15),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (!verUserFoundName.isNullOrEmpty()) {
                                                Text(
                                                    text = "Name: $verUserFoundName",
                                                    style = MaterialTheme.fontFamilyPairMedium(12),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "3-Second Live Stream Notice:",
                                            style = MaterialTheme.fontFamilyPairBold(12),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "• A 3-second live camera capture will initialize automatically.\n" +
                                                   "• Keep face centered inside the oval guide frame.\n" +
                                                   "• Timer counts down from 3s to 0s to auto-capture biometric frame.",
                                            style = MaterialTheme.fontFamilyPairMedium(11),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showVerificationConfirmDialog = false
                                        viewModel.startVerificationFlow(userIdInput)
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Confirm & Start 3s Live Capture", style = MaterialTheme.fontFamilyPairBold(13))
                                }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = { showVerificationConfirmDialog = false },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Cancel", style = MaterialTheme.fontFamilyPairBold(13))
                                }
                            }
                        )
                    }

                    // Verification Action button (Triggers Confirmation Dialog First)
                    Button(
                        onClick = { showVerificationConfirmDialog = true },
                        enabled = isVerUserIdValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Proceed to Verification", style = MaterialTheme.fontFamilyPairBold(15))
                    }
                }
            }
            is VerificationState.FaceCapture -> {
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
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Text(
                                    text = "PASSIVE FACE CAPTURE",
                                    style = MaterialTheme.fontFamilyPairBold(14),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Position face in oval. Passive biometric scan in progress...",
                                style = MaterialTheme.fontFamilyPairMedium(11),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.resetVerification() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel verification",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Active Face Capture Camera (Automatic Passive Capture, No Manual Button Needed)
                    CameraView(
                        modifier = Modifier.weight(1f),
                        showCaptureButton = false,
                        isPassiveAutoCapture = true,
                        onPhotoCaptured = { bitmap ->
                            viewModel.verifyCapturedFace(state.userId, bitmap)
                        }
                    )
                }
            }
            is VerificationState.Uploading -> {
                val animatedProgress by animateFloatAsState(
                    targetValue = state.progress,
                    animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
                    label = "VerificationProgress"
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
                                        .background(Color(0xFFEAB308))
                                )
                                Text(
                                    text = "LIVENESS FACE ANALYSIS",
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
                                    Text("video.mp4 (video/mp4)", style = MaterialTheme.fontFamilyPairBold(11), color = MaterialTheme.colorScheme.onSurface)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Frame Rate", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("30 fps (Simulated)", style = MaterialTheme.fontFamilyPairBold(11), color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
            is VerificationState.LivenessFailed -> {
                var showTechDetails by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "LIVENESS FAILURE (SPOOFING ALERT)",
                                style = MaterialTheme.fontFamilyPairBold(16),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.fontFamilyPairMedium(13),
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Tips for a successful check:\n• Face the camera directly in a well-lit area.\n• Ensure your camera lens is clean.\n• Avoid glare or using printed photos/screens.",
                                style = MaterialTheme.fontFamilyPairMedium(12),
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )

                            // Live score & biometric metrics brought directly to front
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Pixel Diff Score",
                                            style = MaterialTheme.fontFamilyPairMedium(11),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${String.format("%.3f", state.score)}%",
                                            style = MaterialTheme.fontFamilyPairBold(18),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Liveness Threshold",
                                            style = MaterialTheme.fontFamilyPairMedium(11),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = ">= ${String.format("%.1f", state.threshold)}%",
                                            style = MaterialTheme.fontFamilyPairBold(18),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.25f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.resetVerification() },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = "Dismiss",
                                        style = MaterialTheme.fontFamilyPairBold(13),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.startVerificationFlow(state.userId)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Retake",
                                        style = MaterialTheme.fontFamilyPairBold(13),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is VerificationState.RecordingCountdown -> {
                // Deprecated in favor of secure face capture flow. Resetting to Idle.
                LaunchedEffect(Unit) {
                    viewModel.resetVerification()
                }
            }
            is VerificationState.MatchResult -> {
                val greenColor = Color(0xFF10B981) // Modern Emerald Green
                val redColor = Color(0xFFEF4444)   // Modern Crimson Red
                val statusColor = if (state.isSuccess) greenColor else redColor
                var showTechDetails by remember { mutableStateOf(false) }

                val isImg1PathError = state.message.contains("img1_path", ignoreCase = true) || 
                                     state.message.contains("Face could not be detected", ignoreCase = true)
                val isOpenCvError = state.message.contains("CascadeClassifier", ignoreCase = true) || 
                                    state.message.contains("cv2", ignoreCase = true) || 
                                    state.message.contains("no attribute", ignoreCase = true)

                val friendlyMsg = if (isImg1PathError) {
                    "Backend Server Error: The registered face photo (img1_path) could not be detected or processed by the server model."
                } else if (isOpenCvError) {
                    "Backend Server Error: OpenCV 'CascadeClassifier' module is missing or corrupt on the Python backend server."
                } else if (state.message.contains("Application failed to respond") || state.message.contains("502") || state.message.contains("503")) {
                    "The biometrics backend server is temporarily unavailable or offline. Please retry in a few moments."
                } else if (state.message.contains("Connection error") || state.message.contains("Connection failed") || state.message.contains("timeout")) {
                    "Unable to reach the biometrics backend. Please verify your internet connection and try again."
                } else if (state.message.contains("User not found") || state.message.contains("404")) {
                    "This User ID is not enrolled in the biometric identity database. Please register first."
                } else if (state.message.contains("Mismatch") || state.message.contains("mismatch")) {
                    "Facial structure mismatch: Captured face does not match the enrolled biometric profile. Please ensure proper lighting and front-facing capture."
                } else {
                    state.message
                }

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
                        border = BorderStroke(
                            1.5.dp,
                            statusColor.copy(alpha = 0.6f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Status Header Badge
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.12f))
                                    .border(2.dp, statusColor.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (state.isSuccess) Icons.Default.VerifiedUser else Icons.Default.GppBad,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            // Status Tag Pill
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = statusColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = if (state.isSuccess) "✓ ACCESS GRANTED • VERIFIED" else "✕ ACCESS REJECTED • MISMATCH",
                                    style = MaterialTheme.fontFamilyPairBold(12),
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }

                            Text(
                                text = friendlyMsg,
                                style = MaterialTheme.fontFamilyPairMedium(13),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            if (!state.isSuccess) {
                                // Actionable Troubleshooting Guide for Failure
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = redColor.copy(alpha = 0.06f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, redColor.copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = redColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (isImg1PathError || isOpenCvError) "PYTHON BACKEND RESOLUTION GUIDE" else "TROUBLESHOOTING TIPS",
                                                style = MaterialTheme.fontFamilyPairBold(11),
                                                color = redColor
                                            )
                                        }

                                        if (isImg1PathError) {
                                            Text("• Root Cause: DeepFace model could not detect a face in the registered image (img1_path) saved on the server.", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.onSurface)
                                            Text("• Fix 1 (User Action): Re-register this User ID with a clear, upright, well-lit face photo.", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.onSurface)
                                            Text("• Fix 2 (Python Backend main.py): Add enforce_detection=False to DeepFace.verify():", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.onSurface)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "DeepFace.verify(img1_path, img2_path, enforce_detection=False)",
                                                    style = MaterialTheme.fontFamilyPairBold(10),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        } else if (isOpenCvError) {
                                            Text("• Root Cause: OpenCV installation in Python is corrupt or conflicting packages (e.g. opencv-python and opencv-contrib-python) are interfering with each other.", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.onSurface)
                                            Text("• Fix 1 (Terminal): Cleanly reinstall opencv-python:", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.onSurface)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "pip uninstall opencv-python opencv-python-headless opencv-contrib-python -y\npip install opencv-python",
                                                    style = MaterialTheme.fontFamilyPairBold(10),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                            Text("• Fix 2 (Python Code): Use a robust detector backend in DeepFace.verify():", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.onSurface)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "DeepFace.verify(img1, img2, detector_backend='ssd', enforce_detection=False)",
                                                    style = MaterialTheme.fontFamilyPairBold(10),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        } else {
                                            Text("• Verify that the entered User ID matches your registered profile.", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.onSurface)
                                            Text("• Ensure clear lighting without harsh shadows or backlighting.", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.onSurface)
                                            Text("• Face the camera straight and keep expression neutral during scan.", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }

                            if (state.isSuccess) {
                                // Prominent Verified Profile Details Card
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = greenColor.copy(alpha = 0.05f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, greenColor.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(greenColor.copy(alpha = 0.18f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = greenColor,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = state.userName,
                                                    style = MaterialTheme.fontFamilyPairBold(17),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "AUTHENTICATED IDENTITY",
                                                    style = MaterialTheme.fontFamilyPairBold(10),
                                                    color = greenColor
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("USER ID", style = MaterialTheme.fontFamilyPairBold(10), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(state.userId, style = MaterialTheme.fontFamilyPairBold(13), color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            if (state.userAge != null) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("AGE", style = MaterialTheme.fontFamilyPairBold(10), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("${state.userAge} yrs", style = MaterialTheme.fontFamilyPairBold(13), color = MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}",
                                            style = MaterialTheme.fontFamilyPairMedium(11),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Match Score & Threshold Progress Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Facial Similarity Score", style = MaterialTheme.fontFamilyPairBold(12), color = MaterialTheme.colorScheme.onSurface)
                                        Text(
                                            text = "${String.format("%.1f", state.similarityScore)}%",
                                            style = MaterialTheme.fontFamilyPairBold(16),
                                            color = if (state.similarityScore >= state.thresholdPercent) greenColor else redColor
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { (state.similarityScore / 100f).coerceIn(0f, 1f) },
                                        color = if (state.similarityScore >= state.thresholdPercent) greenColor else redColor,
                                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Required Threshold: ${String.format("%.0f", state.thresholdPercent)}%",
                                            style = MaterialTheme.fontFamilyPairMedium(11),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (state.similarityScore >= state.thresholdPercent) "MATCH APPROVED" else "BELOW THRESHOLD",
                                            style = MaterialTheme.fontFamilyPairBold(10),
                                            color = if (state.similarityScore >= state.thresholdPercent) greenColor else redColor
                                        )
                                    }
                                }
                            }

                            // Live Biometric Score (Front & Center)
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Biometric Liveness Score",
                                            style = MaterialTheme.fontFamilyPairBold(11),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${String.format("%.1f", state.livenessScore)}% Live Score",
                                            style = MaterialTheme.fontFamilyPairBold(12),
                                            color = statusColor
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (state.livenessScore / 100f).coerceIn(0f, 1f) },
                                        color = statusColor,
                                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // Primary & Secondary Action Hierarchy
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.resetVerification() },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "Dismiss",
                                        style = MaterialTheme.fontFamilyPairBold(13),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (!state.isSuccess) {
                                    Button(
                                        onClick = { viewModel.startVerificationFlow(state.userId) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = statusColor
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .defaultMinSize(minHeight = 48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Retake",
                                            style = MaterialTheme.fontFamilyPairBold(13),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.resetVerification() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .defaultMinSize(minHeight = 48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = statusColor
                                        )
                                    ) {
                                        Text("Done", style = MaterialTheme.fontFamilyPairBold(13))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperConsoleScreen(viewModel: LivenessViewModel) {
    val forceDup by viewModel.forceDuplicateBiometric.collectAsState()
    val forceSpoof by viewModel.forceSpoofingAttack.collectAsState()
    val forceSimFail by viewModel.forceSimilarityFail.collectAsState()
    val customSimilarity by viewModel.customSimilarityScore.collectAsState()
    val apiLogs by viewModel.apiLogs.collectAsState()

    var activeLogTab by remember { mutableStateOf(0) } // 0 = Simulator controls, 1 = REST API Logs, 2 = DB Profiles

    val registeredUsers by viewModel.allUsers.collectAsState()
    val verificationLogs by viewModel.verificationLogs.collectAsState()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Switches
        TabRow(
            selectedTabIndex = activeLogTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(selected = activeLogTab == 0, onClick = { activeLogTab = 0 }) {
                Text("Simulator Dials", style = MaterialTheme.fontFamilyPairBold(12), modifier = Modifier.padding(bottom = 12.dp, top = 6.dp))
            }
            Tab(selected = activeLogTab == 1, onClick = { activeLogTab = 1 }) {
                BadgedBox(badge = {
                    if (apiLogs.isNotEmpty()) {
                        Badge { Text("${apiLogs.size}") }
                    }
                }) {
                    Text("API Logs", style = MaterialTheme.fontFamilyPairBold(12), modifier = Modifier.padding(bottom = 12.dp, top = 6.dp))
                }
            }
            Tab(selected = activeLogTab == 2, onClick = { activeLogTab = 2 }) {
                Text("Database Info", style = MaterialTheme.fontFamilyPairBold(12), modifier = Modifier.padding(bottom = 12.dp, top = 6.dp))
            }
        }

        Crossfade(targetState = activeLogTab, label = "console_panels", modifier = Modifier.weight(1f)) { tabIndex ->
            when (tabIndex) {
                0 -> {
                    // Simulator controls
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "DEVELOPER SIMULATOR DASHBOARD",
                            style = MaterialTheme.fontFamilyPairBold(14),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Force unique logical branches to test duplicate database embeddings, passive presentation spoofing attacks, and custom similarity match percentages directly in the browser emulator.",
                            style = MaterialTheme.fontFamilyPairMedium(12),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Control Card 1: Registration duplicate
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Force Duplicate Biometric", style = MaterialTheme.fontFamilyPairBold(14))
                                    Text("Simulates detection of duplicate face profile embedding on register request, triggering error retries.", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = forceDup,
                                    onCheckedChange = { viewModel.forceDuplicateBiometric.value = it }
                                )
                            }
                        }

                        // Control Card 2: Liveness Spoofing failure
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Force Liveness Spoof Fail", style = MaterialTheme.fontFamilyPairBold(14))
                                    Text("Simulates passive liveness pixel diff delta check failures (e.g. static photo presentation attack).", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = forceSpoof,
                                    onCheckedChange = { viewModel.forceSpoofingAttack.value = it }
                                )
                            }
                        }

                        // Control Card 3: Match similarity sliders
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Force Similarity Match Fail", style = MaterialTheme.fontFamilyPairBold(14))
                                        Text("Forces facial Cosine Similarity to fail threshold (<60%), triggering re-take options.", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = forceSimFail,
                                        onCheckedChange = { viewModel.forceSimilarityFail.value = it }
                                    )
                                }

                                if (!forceSimFail) {
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Biometric Similarity Output Score", style = MaterialTheme.fontFamilyPairBold(12))
                                            Text("${customSimilarity.toInt()}% Similarity", style = MaterialTheme.fontFamilyPairBold(13), color = MaterialTheme.colorScheme.primary)
                                        }
                                        Slider(
                                            value = customSimilarity,
                                            onValueChange = { viewModel.customSimilarityScore.value = it },
                                            valueRange = 60f..100f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Wipe Database button
                        Button(
                            onClick = {
                                viewModel.clearDbData()
                                Toast.makeText(context, "Database & Console wiped successfully!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wipe Local Profiles & Database Logs", style = MaterialTheme.fontFamilyPairBold(13))
                        }

                        // Hidden Deep Developer Credits Card
                        var showDeepCredits by remember { mutableStateOf(false) }
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDeepCredits = !showDeepCredits }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "System Build & Developer Meta",
                                        style = MaterialTheme.fontFamilyPairMedium(11),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = if (showDeepCredits) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                AnimatedVisibility(visible = showDeepCredits) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Developer: Aaditya",
                                            style = MaterialTheme.fontFamilyPairBold(12),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "GitHub: /AadityaGeek",
                                                style = MaterialTheme.fontFamilyPairMedium(11),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable {
                                                    try { uriHandler.openUri("https://github.com/AadityaGeek") } catch (e: Exception) {}
                                                }
                                            )
                                            Text(
                                                text = "LinkedIn: /aadityakr",
                                                style = MaterialTheme.fontFamilyPairMedium(11),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable {
                                                    try { uriHandler.openUri("https://linkedin.com/in/aadityakr") } catch (e: Exception) {}
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // API Logs panel
                    if (apiLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                                Text("No API traffic recorded yet.", style = MaterialTheme.fontFamilyPairBold(14), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Submit registrations or complete verifications to inspect the multipart payloads.", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(apiLogs) { log ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF020617) // Deep pitch terminal slate
                                    ),
                                    border = BorderStroke(1.dp, if (log.responseStatusCode in 200..299) Color(0xFF22C55E).copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .verticalScroll(rememberScrollState(), enabled = false),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(log.endpoint, style = MaterialTheme.fontFamilyPairBold(13), color = Color(0xFFF8FAFC))
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (log.responseStatusCode in 200..299) Color(0xFF166534) else Color(0xFF991B1B))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("HTTP ${log.responseStatusCode}", style = MaterialTheme.fontFamilyPairBold(10), color = Color.White)
                                            }
                                        }

                                        Text(
                                            text = "Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp))}",
                                            style = MaterialTheme.fontFamilyPairMedium(10),
                                            color = Color(0xFF94A3B8)
                                        )

                                        Divider(color = Color(0xFF1E293B))

                                        // Headers
                                        Text("Request Headers:", style = MaterialTheme.fontFamilyPairBold(11), color = Color(0xFF38BDF8))
                                        log.requestHeaders.forEach { (k, v) ->
                                            Text("$k: $v", style = MaterialTheme.fontFamilyPairMedium(10), color = Color(0xFF94A3B8))
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Request Body
                                        Text("Request Payload (Multipart):", style = MaterialTheme.fontFamilyPairBold(11), color = Color(0xFF38BDF8))
                                        Text(log.requestBody, style = MaterialTheme.fontFamilyPairMedium(10), color = Color(0xFFE2E8F0))

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Response Body
                                        Text("JSON Response Body:", style = MaterialTheme.fontFamilyPairBold(11), color = Color(0xFF22C55E))
                                        Text(log.responseBody, style = MaterialTheme.fontFamilyPairMedium(10), color = Color(0xFFE2E8F0))
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // DB Profiles & verification records
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("REGISTERED SYSTEM USERS:", style = MaterialTheme.fontFamilyPairBold(12), color = MaterialTheme.colorScheme.primary)

                        if (registeredUsers.isEmpty()) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("No registered user profiles found.", style = MaterialTheme.fontFamilyPairMedium(12), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(registeredUsers) { user ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Name: ${user.name} • Age: ${user.age}", style = MaterialTheme.fontFamilyPairBold(13))
                                            Text("User ID: ${user.userId}", style = MaterialTheme.fontFamilyPairMedium(12), color = MaterialTheme.colorScheme.primary)
                                            Text("Biometric Embedding (Partial): ${user.biometricEmbeddingHex.take(20)}...", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        Text("VERIFICATION ACCESS LOGS:", style = MaterialTheme.fontFamilyPairBold(12), color = MaterialTheme.colorScheme.secondary)

                        if (verificationLogs.isEmpty()) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("No verification log records registered.", style = MaterialTheme.fontFamilyPairMedium(12), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(verificationLogs) { log ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (log.isMatched) MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
                                        ),
                                        border = BorderStroke(1.dp, if (log.isMatched) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("User Target: ${log.userId}", style = MaterialTheme.fontFamilyPairBold(13))
                                                Text(
                                                    text = if (log.isMatched) "PASSED" else "FAILED",
                                                    style = MaterialTheme.fontFamilyPairBold(11),
                                                    color = if (log.isMatched) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                                                )
                                            }
                                            Text("Details: ${log.statusMessage}", style = MaterialTheme.fontFamilyPairMedium(11))
                                            Text(
                                                text = "Similarity: ${String.format("%.1f", log.similarityScore)}% • Liveness delta: ${String.format("%.2f", log.livenessScore)}%",
                                                style = MaterialTheme.fontFamilyPairMedium(10),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackendUrlConfigDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    var currentUrlInput by remember { mutableStateOf(com.example.data.ApiConfig.BASE_URL) }
    var testingConnection by remember { mutableStateOf(false) }
    var testResultStatus by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Backend Server URL",
                style = MaterialTheme.fontFamilyPairBold(18),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configure the backend API server base URL dynamically for development and testing.",
                    style = MaterialTheme.fontFamilyPairMedium(12),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = currentUrlInput,
                    onValueChange = {
                        currentUrlInput = it
                        testResultStatus = null
                    },
                    label = { Text("Base URL", style = MaterialTheme.fontFamilyPairMedium(12)) },
                    placeholder = { Text("https://example.com/", style = MaterialTheme.fontFamilyPairMedium(12)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (currentUrlInput.isNotBlank()) {
                            IconButton(onClick = { currentUrlInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )

                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.fontFamilyPairBold(11),
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssistChip(
                        onClick = {
                            currentUrlInput = com.example.data.ApiConfig.DEFAULT_BASE_URL
                            testResultStatus = null
                        },
                        label = { Text("Railway Cloud", style = MaterialTheme.fontFamilyPairMedium(10)) },
                        leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    AssistChip(
                        onClick = {
                            currentUrlInput = "http://10.0.2.2:8000/"
                            testResultStatus = null
                        },
                        label = { Text("Emulator (10.0.2.2)", style = MaterialTheme.fontFamilyPairMedium(10)) },
                        leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    AssistChip(
                        onClick = {
                            currentUrlInput = "http://192.168.29.221:8000/"
                            testResultStatus = null
                        },
                        label = { Text("Local Wi-Fi IP", style = MaterialTheme.fontFamilyPairMedium(10)) },
                        leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    AssistChip(
                        onClick = {
                            currentUrlInput = "https://xxx.ngrok-free.app/"
                            testResultStatus = null
                        },
                        label = { Text("ngrok Tunnel", style = MaterialTheme.fontFamilyPairMedium(10)) },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }

                if (testingConnection) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Testing connection (10s timeout)...", style = MaterialTheme.fontFamilyPairMedium(12))
                    }
                } else if (testResultStatus != null) {
                    val isSuccess = testResultStatus == "SUCCESS"
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isSuccess) "Connection Successful!" else testResultStatus!!,
                                    style = MaterialTheme.fontFamilyPairMedium(11),
                                    color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        if (!isSuccess && currentUrlInput.contains("192.168.")) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "💡 Local IP Troubleshooting:",
                                        style = MaterialTheme.fontFamilyPairBold(11),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "1. Bind Host to 0.0.0.0:\n   uvicorn main:app --host 0.0.0.0 --port 8000\n2. Windows/Mac Firewall:\n   Allow inbound port 8000.\n3. Recommended Alternative:\n   Run 'ngrok http 8000' for instant HTTPS tunnel.",
                                        style = MaterialTheme.fontFamilyPairMedium(10),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = {
                        testingConnection = true
                        testResultStatus = null
                        val tempUrl = currentUrlInput
                        coroutineScope.launch {
                            try {
                                val response = com.example.data.FaceRecognitionApi.checkStatusForUrl(tempUrl)
                                if (response.isSuccessful) {
                                    testResultStatus = "SUCCESS"
                                } else {
                                    testResultStatus = "Server returned HTTP ${response.code()}"
                                }
                            } catch (e: Exception) {
                                testResultStatus = "Failed: ${e.localizedMessage ?: "Network error"}"
                            } finally {
                                testingConnection = false
                            }
                        }
                    },
                    enabled = !testingConnection && currentUrlInput.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("Test Connection", style = MaterialTheme.fontFamilyPairBold(12))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    com.example.data.ApiConfig.updateBaseUrl(context, currentUrlInput)
                    Toast.makeText(context, "Backend URL saved: ${com.example.data.ApiConfig.BASE_URL}", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Text("Save & Apply", style = MaterialTheme.fontFamilyPairBold(13))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = MaterialTheme.fontFamilyPairMedium(13))
            }
        }
    )
}
