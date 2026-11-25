package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.launch

private val BrandGreen = Color(0xFF246849)
private val BrandGreenDark = Color(0xFF1A4C35)
private val BrandGreenLight = Color(0xFFEAF4ED)
private val BrandGreenAccent = Color(0xFF48C774)

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit = {}
) {
    var showCameraPermissionStep by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = showCameraPermissionStep) {
        showCameraPermissionStep = false
    }

    BackHandler(enabled = !showCameraPermissionStep && pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .statusBarsPadding()
            .testTag("onboarding_root")
    ) {
        AnimatedVisibility(
            visible = !showCameraPermissionStep,
            enter = fadeIn() + slideInHorizontally { -it },
            exit = fadeOut() + slideOutHorizontally { -it }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) { page ->
                when (page) {
                    0 -> Slide1IdentifyPlant(
                        onNext = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onLogin = onLogin
                    )
                    1 -> Slide2LearnAboutIt(
                        onBack = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onNext = {
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        }
                    )
                    2 -> Slide4StartScanning(
                        onStartScanning = {
                            showCameraPermissionStep = true
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showCameraPermissionStep,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it }
        ) {
            CameraPermissionScreen(
                onCompleted = onGetStarted,
                onNotNow = onGetStarted
            )
        }
    }
}

// ==========================================
// 1. IDENTIFY ANY PLANT (Meet Your Plant)
// ==========================================
@Composable
private fun Slide1IdentifyPlant(
    onNext: () -> Unit,
    onLogin: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF111713) else Color(0xFFF3F7F4)
    val cardBg = if (isDark) Color(0xFF1E2520) else Color.White
    val textPrimary = if (isDark) Color(0xFFF0F5F1) else Color(0xFF182E22)
    val textSecondary = if (isDark) Color(0xFFA5B4A9) else Color(0xFF637367)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Top Brand Logo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            LeafLensLogoHeader()
        }

        // Top Visual Area: Phone Viewfinder Framing a Plant
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Decorative background leaf glow
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                BrandGreen.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            // Phone Mockup Frame
            Surface(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight(0.92f)
                    .shadow(16.dp, RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF151D18),
                border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF2C3931))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Plant camera image
                    Image(
                        painter = painterResource(id = R.drawable.img_onboard_scan_1790602367344),
                        contentDescription = "Plant camera scanner viewfinder",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Top phone speaker slit
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .size(width = 44.dp, height = 4.dp)
                            .background(Color(0xFF2B332E), RoundedCornerShape(2.dp))
                    )

                    // Green Scanner Focus Brackets [ ]
                    ScannerBracketsOverlay(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(130.dp)
                    )

                    // Bottom Shutter Button inside phone
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp)
                            .size(46.dp)
                            .border(3.dp, Color.White, CircleShape)
                            .padding(4.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }

        // Bottom White Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Meet Your Plant",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Snap a photo of any plant and let LeafLens identify it for you.",
                    fontSize = 15.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                PageDotsIndicator(totalPages = 3, currentPage = 0)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("slide1_next_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    shape = RoundedCornerShape(27.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Next",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("onboarding_login_button")
                ) {
                    Text(
                        text = "Already have an account? Log In",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGreen
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. LEARN ABOUT IT (Know Your Plant Better)
// ==========================================
@Composable
private fun Slide2LearnAboutIt(
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF101612) else Color(0xFFF3F7F4)
    val cardBg = if (isDark) Color(0xFF1E2520) else Color.White
    val textPrimary = if (isDark) Color(0xFFF0F5F1) else Color(0xFF182E22)
    val textSecondary = if (isDark) Color(0xFFA5B4A9) else Color(0xFF637367)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Top Bar: Back & Favorite buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onBack() },
                shape = CircleShape,
                color = if (isDark) Color(0xFF252E28) else Color.White.copy(alpha = 0.9f),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (isDark) Color(0xFF252E28) else Color.White.copy(alpha = 0.9f),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.FavoriteBorder,
                        contentDescription = "Save",
                        tint = BrandGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Top Visual Area: Result Card floating over Monstera Leaves
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background monstera image with rounded aesthetic
            Image(
                painter = painterResource(id = R.drawable.img_monstera_leaf_1790602381349),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )

            // Soft dark/frost gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            )

            // Result Preview Card (Matches screenshot 2)
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF1E2621) else Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Header row: Monstera thumb + Title + Common badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_monstera_leaf_1790602381349),
                            contentDescription = "Monstera",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Monstera Deliciosa",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Swiss Cheese Plant",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BrandGreenLight
                        ) {
                            Text(
                                text = "Common",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandGreenDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tabs: Overview | Care | Facts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isDark) Color(0xFF28322B) else Color(0xFFF1F5F2),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isDark) Color(0xFF1E2621) else Color.White,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Overview",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Care",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Facts",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Info items
                    PlantDetailRow(
                        icon = Icons.Filled.WbSunny,
                        iconTint = Color(0xFFE5A93C),
                        label = "Sunlight",
                        value = "Bright, indirect light"
                    )
                    PlantDetailRow(
                        icon = Icons.Filled.WaterDrop,
                        iconTint = Color(0xFF3892D3),
                        label = "Watering",
                        value = "Keep soil slightly moist"
                    )
                    PlantDetailRow(
                        icon = Icons.Filled.Spa,
                        iconTint = BrandGreen,
                        label = "Soil",
                        value = "Well-draining, rich soil"
                    )
                    PlantDetailRow(
                        icon = Icons.Filled.Info,
                        iconTint = Color(0xFF43A047),
                        label = "Fun Fact",
                        value = "Known for its unique split leaves!"
                    )
                }
            }
        }

        // Bottom White Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Know Your Plant Better",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Discover its name, characteristics, care needs, and interesting facts in seconds.",
                    fontSize = 15.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                PageDotsIndicator(totalPages = 3, currentPage = 1)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("slide2_next_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    shape = RoundedCornerShape(27.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Next",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. AI PLANT EXPERT (Chat With Your AI Expert)
// ==========================================
@Composable
private fun Slide3AiPlantExpert(
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF101612) else Color(0xFFF3F7F4)
    val cardBg = if (isDark) Color(0xFF1E2520) else Color.White
    val textPrimary = if (isDark) Color(0xFFF0F5F1) else Color(0xFF182E22)
    val textSecondary = if (isDark) Color(0xFFA5B4A9) else Color(0xFF637367)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Top Bar: Back button + Expert Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onBack() },
                shape = CircleShape,
                color = if (isDark) Color(0xFF252E28) else Color.White,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            AiRobotAvatar(size = 36.dp)

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "LeafLens AI",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "Your plant expert",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }
        }

        // Top Visual Area: Chat conversation mockup
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Plant context chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF1D2620) else Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_monstera_leaf_1790602381349),
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Monstera Deliciosa",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        Text(
                            text = "Swiss Cheese Plant",
                            fontSize = 10.sp,
                            color = textSecondary
                        )
                    }
                }
            }

            // User Chat Bubble (Right aligned)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
                    color = BrandGreen
                ) {
                    Text(
                        text = "How often should I water this?",
                        fontSize = 13.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF28342D) else Color(0xFFD6E3D8)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = BrandGreenDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // AI Chat Bubble (Left aligned)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                AiRobotAvatar(size = 28.dp)

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = if (isDark) Color(0xFF1E2621) else Color.White,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "Monstera Deliciosa prefers moderate watering. Usually, water it when the top 2-3 cm of soil feels dry. This is typically once a week, but it may vary based on your environment and season.",
                        fontSize = 13.sp,
                        color = textPrimary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // Suggestion Chips
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Why are the leaves turning yellow?",
                    "Does it need direct sunlight?",
                    "How can I make it grow bigger?"
                ).forEach { chipText ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF1B231D) else Color(0xFFEDF5EF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = chipText,
                            fontSize = 12.sp,
                            color = BrandGreenDark,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Input Bar Mockup
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) Color(0xFF1C241E) else Color.White,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ask a question...",
                        fontSize = 13.sp,
                        color = textSecondary,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = BrandGreen
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom White Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Chat With Your AI Plant Expert",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Ask questions about your plant and get personalized answers whenever you need them.",
                    fontSize = 15.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                PageDotsIndicator(totalPages = 4, currentPage = 2)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("slide3_next_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    shape = RoundedCornerShape(27.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Next",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. START SCANNING (Your Plant Journey Starts Here)
// ==========================================
@Composable
private fun Slide4StartScanning(onStartScanning: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF101713) else Color(0xFFF7FAF7)
    val textPrimary = if (isDark) Color(0xFFF0F5F1) else Color(0xFF162D20)
    val textSecondary = if (isDark) Color(0xFFA5B4A9) else Color(0xFF637367)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Brand Logo
        LeafLensLogoHeader()

        Spacer(modifier = Modifier.height(24.dp))

        // Main Title
        Text(
            text = "Your Plant Journey\nStarts Here \uD83C\uDF3F",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitle
        Text(
            text = "Identify plants, understand their needs, and become better at caring for them with LeafLens.",
            fontSize = 14.5.sp,
            color = textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Collage of Plant Images (Matches screenshot 4 collage)
        PlantCollageLayout(modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(32.dp))

        // Dots Indicator
        PageDotsIndicator(totalPages = 3, currentPage = 2)

        Spacer(modifier = Modifier.height(24.dp))

        // Start Scanning Button
        Button(
            onClick = onStartScanning,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_scanning_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            shape = RoundedCornerShape(27.dp)
        ) {
            Text(
                text = "Start Scanning",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==========================================
// CAMERA PERMISSION SCREEN
// ==========================================
@Composable
private fun CameraPermissionScreen(
    onCompleted: () -> Unit,
    onNotNow: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF101713) else Color(0xFFF7FAF7)
    val textPrimary = if (isDark) Color(0xFFF0F5F1) else Color(0xFF162D20)
    val textSecondary = if (isDark) Color(0xFFA5B4A9) else Color(0xFF637367)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Camera Icon with leaf decorations
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Leaf accents behind circle
            DecorativeLeaf(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.TopStart)
                    .offset(x = 10.dp, y = 10.dp),
                color = BrandGreenAccent
            )
            DecorativeLeaf(
                modifier = Modifier
                    .size(42.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-6).dp, y = (-6).dp),
                color = BrandGreen
            )

            // Circle container
            Surface(
                modifier = Modifier.size(110.dp),
                shape = CircleShape,
                color = if (isDark) Color(0xFF1C2720) else BrandGreenLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = "Camera Access",
                        tint = BrandGreen,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Let LeafLens See\nYour Plant",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Camera access lets you scan plants and get instant information about them.",
            fontSize = 15.sp,
            color = textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Allow Camera Button
        Button(
            onClick = onCompleted,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("allow_camera_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            shape = RoundedCornerShape(27.dp)
        ) {
            Text(
                text = "Allow Camera",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Not Now Button
        TextButton(
            onClick = onNotNow,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("not_now_button")
        ) {
            Text(
                text = "Not Now",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==========================================
// REUSABLE SUB-COMPONENTS
// ==========================================

@Composable
private fun LeafLensLogoHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Custom Leaf Icon
        Canvas(modifier = Modifier.size(24.dp)) {
            val leafPath = Path().apply {
                moveTo(size.width * 0.15f, size.height * 0.85f)
                cubicTo(
                    size.width * 0.05f, size.height * 0.35f,
                    size.width * 0.45f, size.height * 0.05f,
                    size.width * 0.85f, size.height * 0.15f
                )
                cubicTo(
                    size.width * 0.95f, size.height * 0.65f,
                    size.width * 0.55f, size.height * 0.95f,
                    size.width * 0.15f, size.height * 0.85f
                )
                close()
            }
            drawPath(leafPath, BrandGreen)
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(size.width * 0.2f, size.height * 0.8f),
                end = Offset(size.width * 0.8f, size.height * 0.2f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "LeafLens",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSystemInDarkTheme()) Color(0xFFF0F5F1) else Color(0xFF162D20),
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
private fun PageDotsIndicator(totalPages: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            val isActive = index == currentPage
            Box(
                modifier = Modifier
                    .size(
                        width = if (isActive) 18.dp else 6.dp,
                        height = 6.dp
                    )
                    .background(
                        color = if (isActive) BrandGreen else Color(0xFFD6E0D8),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun PlantDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(26.dp),
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFCBD6CD) else Color(0xFF334638)
            )
            Text(
                text = value,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF9AA79D) else Color(0xFF6B7B70)
            )
        }
    }
}

@Composable
private fun ScannerBracketsOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 3.dp.toPx()
        val cornerLength = size.width * 0.22f
        val color = Color(0xFF48C774)

        // Top-Left
        drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), stroke, StrokeCap.Round)
        drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), stroke, StrokeCap.Round)

        // Top-Right
        drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLength), stroke, StrokeCap.Round)

        // Bottom-Left
        drawLine(color, Offset(0f, size.height), Offset(cornerLength, size.height), stroke, StrokeCap.Round)
        drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLength), stroke, StrokeCap.Round)

        // Bottom-Right
        drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), stroke, StrokeCap.Round)
    }
}

@Composable
private fun PlantCollageLayout(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Decorative floating leaves in the background
        DecorativeLeaf(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 10.dp, y = (-12).dp)
                .size(32.dp),
            color = BrandGreenAccent.copy(alpha = 0.7f)
        )
        DecorativeLeaf(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-16).dp, y = 10.dp)
                .size(24.dp),
            color = BrandGreen.copy(alpha = 0.5f)
        )
        DecorativeLeaf(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 12.dp, y = 8.dp)
                .size(28.dp),
            color = BrandGreenDark.copy(alpha = 0.6f)
        )

        // 1. Monstera leaf card (Top Left, tall)
        Image(
            painter = painterResource(id = R.drawable.img_monstera_leaf_1790602381349),
            contentDescription = "Monstera",
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 12.dp, y = 0.dp)
                .size(width = 96.dp, height = 118.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
        )

        // 2. Pink Flower card (Top Right)
        Image(
            painter = painterResource(id = R.drawable.img_pink_flower_1790602396288),
            contentDescription = "Pink Hibiscus",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-36).dp, y = 8.dp)
                .size(86.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
        )

        // 3. Succulent card (Middle Left)
        Image(
            painter = painterResource(id = R.drawable.img_succulent_1790602411204),
            contentDescription = "Succulent",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 24.dp, y = (-12).dp)
                .size(86.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
        )

        // 4. Sunflower card (Center Bottom)
        Image(
            painter = painterResource(id = R.drawable.img_sunflower_1790602425328),
            contentDescription = "Sunflower",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 42.dp, y = 0.dp)
                .size(88.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
        )

        // 5. Potted houseplant (Middle Right)
        Image(
            painter = painterResource(id = R.drawable.playstore_icon),
            contentDescription = "Potted plant",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-8).dp, y = 14.dp)
                .size(width = 72.dp, height = 94.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun AiRobotAvatar(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = BrandGreenLight
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(size * 0.65f)) {
                val w = this.size.width
                val h = this.size.height
                // Little sprout leaves on top of robot
                val sproutPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.22f)
                    cubicTo(w * 0.2f, 0f, w * 0.45f, 0f, w * 0.5f, h * 0.22f)
                    cubicTo(w * 0.55f, 0f, w * 0.8f, 0f, w * 0.5f, h * 0.22f)
                    close()
                }
                drawPath(sproutPath, BrandGreenAccent)

                // Robot Head
                drawRoundRect(
                    color = BrandGreen,
                    topLeft = Offset(w * 0.14f, h * 0.26f),
                    size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.62f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.18f, w * 0.18f)
                )

                // Two eyes
                drawCircle(Color.White, radius = w * 0.08f, center = Offset(w * 0.38f, h * 0.54f))
                drawCircle(Color.White, radius = w * 0.08f, center = Offset(w * 0.62f, h * 0.54f))

                // Friendly smile
                drawArc(
                    color = Color.White,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.4f, h * 0.62f),
                    size = androidx.compose.ui.geometry.Size(w * 0.2f, h * 0.14f),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
private fun DecorativeLeaf(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0f, size.height)
            cubicTo(0f, size.height * 0.4f, size.width * 0.4f, 0f, size.width, 0f)
            cubicTo(size.width, size.height * 0.6f, size.width * 0.6f, size.height, 0f, size.height)
            close()
        }
        drawPath(path, color)
    }
}
