package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.isGranted
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Plant
import com.example.ui.theme.*

@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit,
    onNavigateToPlant: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val plants by viewModel.allPlants.collectAsState()
    val weatherData by viewModel.weatherData
    val issuesCount = plants.count { it.healthStatus != "Healthy" }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val notificationPermission = com.google.accompanist.permissions.rememberPermissionState(
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        androidx.compose.runtime.LaunchedEffect(Unit) {
            if (!notificationPermission.status.isGranted) {
                notificationPermission.launchPermissionRequest()
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.fetchWeather(40.7128, -74.0060)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            item(span = { GridItemSpan(2) }) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LeafLens", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(
                                "BOTANICAL ASSISTANT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 2.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceVariant)
                                .border(1.dp, TextSecondary.copy(alpha = 0.2f), CircleShape)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 18.sp)
                        }
                    }
                    
                    AdBanner(modifier = Modifier.padding(bottom = 12.dp))
                }
            }

            // AI Insight / Follow-up alert
            item(span = { GridItemSpan(2) }) {
                val diseasedPlants = plants.filter { it.disease != null && it.disease.isNotBlank() && it.disease != "null" && it.healthStatus != "Healthy" }
                val insightEmoji = if (diseasedPlants.isNotEmpty()) "🩺" else "✨"
                val insightTitle = if (diseasedPlants.isNotEmpty()) "Follow-up Required" else "AI Garden Insight"
                val insightMessage = if (diseasedPlants.isNotEmpty()) {
                    val plant = diseasedPlants.first()
                    "${plant.name} needs a daily checkup for ${plant.disease}. Stick to the care routine!"
                } else {
                    "Luna's leaves are drooping. Consider watering tomorrow given the current 24°C room temperature."
                }
                val bgColor = if (diseasedPlants.isNotEmpty()) AlertCardBg else EnvPillBg.copy(alpha = 0.5f)
                val titleColor = if (diseasedPlants.isNotEmpty()) AlertTextPrimary else TextPrimary

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(insightEmoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(insightTitle, fontWeight = FontWeight.Bold, color = titleColor, fontSize = 14.sp)
                            Text(insightMessage, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
                        }
                    }
                }
            }

            // Hero Card
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 160.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = HeroCardBg),
                    onClick = onNavigateToScan
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Identify any\nplant species",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 28.sp
                                )
                                Text(
                                    "Instant diagnosis & care guide",
                                    color = HeroTextLight,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            Button(
                                onClick = onNavigateToScan,
                                colors = ButtonDefaults.buttonColors(containerColor = HeroButtonBg, contentColor = HeroButtonText),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Start Scan", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Health Radar Card
            item(span = { GridItemSpan(1) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 160.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertCardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (issuesCount > 0) "⚠️" else "✨", fontSize = 24.sp)
                            }
                        }
                        Column {
                            Text("Health\nRadar", color = AlertTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                            Text(
                                if (issuesCount > 0) "$issuesCount ISSUES" else "ALL CLEAR",
                                color = AlertTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            // My Garden Sub-card
            item(span = { GridItemSpan(1) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 160.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = GardenCardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🪴", fontSize = 24.sp)
                            }
                        }
                        Column {
                            Text("My\nGarden", color = GardenTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                            Text(
                                "${plants.size} PLANTS",
                                color = GardenTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            // Env Stats
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = EnvCardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Outdoor Climate", color = EnvTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                            Surface(
                                color = EnvPillBg,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "LIVE",
                                    color = EnvTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val temp = weatherData?.current?.temperature_2m?.let { "${Math.round(it)}°C" } ?: "--°C"
                            val humidity = weatherData?.current?.relative_humidity_2m?.let { "$it%" } ?: "--%"
                            val light = weatherData?.current?.is_day?.let { if (it == 1) "Day" else "Night" } ?: "--"

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(temp, fontSize = 28.sp, fontWeight = FontWeight.Light, color = EnvTextPrimary)
                                Text("TEMP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EnvTextPrimary.copy(alpha = 0.6f))
                            }
                            VerticalDivider(modifier = Modifier.height(48.dp), color = EnvTextPrimary.copy(alpha = 0.1f))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(humidity, fontSize = 28.sp, fontWeight = FontWeight.Light, color = EnvTextPrimary)
                                Text("HUMIDITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EnvTextPrimary.copy(alpha = 0.6f))
                            }
                            VerticalDivider(modifier = Modifier.height(48.dp), color = EnvTextPrimary.copy(alpha = 0.1f))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(light, fontSize = 28.sp, fontWeight = FontWeight.Light, color = EnvTextPrimary)
                                Text("OUTDOOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EnvTextPrimary.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }

            // Collection Header
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Scans",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
            
            // Keep Home lightweight; rewarded ads are loaded only after the user taps in Profile.
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                ) {
                    Text(
                        text = "Use points for scans and AI plant care.",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (plants.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text("Your garden is empty. Scan a plant to get started!", color = TextPrimary)
                }
            } else {
                items(plants.take(4)) { plant -> // Limit to 4 latest ones
                    PlantCard(plant, onClick = { onNavigateToPlant(plant.name) })
                }
                
                if (plants.size > 4) {
                    val remaining = plants.size - 4
                    item(span = { GridItemSpan(2) }) {
                        OutlinedButton(
                            onClick = { /* Could navigate to full collection */ },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary.copy(alpha = 0.3f))
                        ) {
                            Text("See $remaining more", color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlantCard(plant: Plant, onClick: () -> Unit) {
    val healthColor = if (plant.healthStatus.equals("Healthy", ignoreCase = true)) HeroCardBg else AlertTextPrimary
    val bgColor = if (plant.healthStatus.equals("Healthy", ignoreCase = true)) SurfaceVariant else AlertCardBg
    
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().height(160.dp).clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text("🌿", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = plant.name,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${plant.healthStatus} ${plant.healthScore}%",
                color = healthColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
