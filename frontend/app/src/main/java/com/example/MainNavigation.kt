package com.example

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.UserEntity
import com.example.ui.LivenessViewModel
import com.example.ui.theme.fontFamilyPairBold
import com.example.ui.theme.fontFamilyPairMedium
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NavigationTab(val label: String, val icon: ImageVector) {
    REGISTER("Register", Icons.Default.PersonAdd),
    VERIFY("Verify", Icons.Default.Fingerprint),
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
                                        NavigationTab.VERIFY -> Icons.Default.Fingerprint
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
                                        imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.NightsStay,
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
                                imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.NightsStay,
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
                                    imageVector = Icons.Default.Fingerprint,
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
                                                imageVector = if (isDarkTheme) Icons.Default.NightsStay else Icons.Default.WbSunny,
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
    val uriHandler = LocalUriHandler.current
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
fun DeveloperConsoleScreen(viewModel: LivenessViewModel) {
    val forceDup by viewModel.forceDuplicateBiometric.collectAsState()
    val forceSpoof by viewModel.forceSpoofingAttack.collectAsState()
    val forceMotionFail by viewModel.forceMotionFail.collectAsState()
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

                        // Control Card 2b: Motion Liveness Failure
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
                                    Text("Force Motion Gesture Fail", style = MaterialTheme.fontFamilyPairBold(14))
                                    Text("Simulates active motion liveness verification rejection (gesture inconsistency/timeout).", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = forceMotionFail,
                                    onCheckedChange = { viewModel.forceMotionFail.value = it }
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
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
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
                        val uriHandler = LocalUriHandler.current

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

                                        HorizontalDivider(color = Color(0xFF1E293B))

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
                    var selectedQrUser by remember { mutableStateOf<UserEntity?>(null) }

                    if (selectedQrUser != null) {
                        val qrUser = selectedQrUser!!
                        val userQrBitmap = remember(qrUser.base64QrCode) {
                            if (qrUser.base64QrCode.isNotEmpty()) QrCodeGenerator.base64ToBitmap(qrUser.base64QrCode) else null
                        }
                        AlertDialog(
                            onDismissRequest = { selectedQrUser = null },
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("QR Identity Credential", style = MaterialTheme.fontFamilyPairBold(16))
                                }
                            },
                            text = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("User: ${qrUser.name} (${qrUser.userId})", style = MaterialTheme.fontFamilyPairBold(13))
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        modifier = Modifier.size(200.dp).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                            if (userQrBitmap != null) {
                                                Image(bitmap = userQrBitmap.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.fillMaxSize())
                                            } else {
                                                Text("QR Unavailable", style = MaterialTheme.fontFamilyPairMedium(12), color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { selectedQrUser = null }) {
                                    Text("Close", style = MaterialTheme.fontFamilyPairBold(13))
                                }
                            }
                        )
                    }

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
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Fingerprint,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Column {
                                                    Text("${user.name} • ${user.age} yrs", style = MaterialTheme.fontFamilyPairBold(13), color = MaterialTheme.colorScheme.onSurface)
                                                    Text("ID: ${user.userId}", style = MaterialTheme.fontFamilyPairMedium(11), color = MaterialTheme.colorScheme.primary)
                                                    Text("Embedding: ${user.biometricEmbeddingHex.take(16)}...", style = MaterialTheme.fontFamilyPairMedium(10), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }

                                            IconButton(
                                                onClick = { selectedQrUser = user }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.QrCode2,
                                                    contentDescription = "View QR Credential",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
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
