package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Plant
import com.example.ui.theme.*
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantProfileScreen(
    plantName: String, 
    onBack: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val plant by viewModel.getPlantByNameFlow(plantName).collectAsState(initial = null)
    val scrollState = rememberScrollState()

    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundLight) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(plantName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = TextPrimary
                )
            )

            plant?.let { livePlant ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    // Plant Info Summary Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🌿", fontSize = 36.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(livePlant.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(livePlant.species, fontSize = 14.sp, color = TextSecondary)
                        }
                    }

                    // Images Carousel
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        val images = mutableListOf<String>()
                        if (livePlant.imageUri != null) {
                            images.add(livePlant.imageUri)
                        }
                        if (livePlant.similarImageUris.isNotBlank()) {
                            images.addAll(livePlant.similarImageUris.split(","))
                        }
                        
                        items(images.size) { index ->
                            val url = images[index]
                            coil.compose.AsyncImage(
                                model = url,
                                contentDescription = "Plant image",
                                modifier = Modifier
                                    .size(220.dp)
                                    .clip(RoundedCornerShape(24.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }

                    if (livePlant.description.isNotBlank()) {
                        Text("About", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(livePlant.description, fontSize = 14.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Tracked Stats Card (Water & Fertilizer Status)
                    val timeNow = System.currentTimeMillis()
                    val needsWater = livePlant.nextWateringTimeMs < timeNow
                    val needsFertilizer = livePlant.nextFeedingTimeMs < timeNow

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Care Tracking Status", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Water Stat Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(if (needsWater) AlertCardBg else EnvPillBg, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("💧", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Watering Status", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    val waterText = if (needsWater) "Needs water immediately!" else "Next watering: " + getDaysRemaining(livePlant.nextWateringTimeMs, timeNow)
                                    Text(
                                        text = waterText,
                                        fontSize = 12.sp,
                                        color = if (needsWater) AlertTextPrimary else TextSecondary,
                                        fontWeight = if (needsWater) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Fertilizer Stat Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(if (needsFertilizer) AlertCardBg else EnvPillBg, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🧪", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Fertilizer Status", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    val fertilizeText = if (needsFertilizer) "Requires fertilization tracker!" else "Next feeding: " + getDaysRemaining(livePlant.nextFeedingTimeMs, timeNow)
                                    Text(
                                        text = fertilizeText,
                                        fontSize = 12.sp,
                                        color = if (needsFertilizer) AlertTextPrimary else TextSecondary,
                                        fontWeight = if (needsFertilizer) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Health score row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isHealthy = livePlant.healthStatus.equals("Healthy", ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(if (isHealthy) HeroButtonBg.copy(alpha = 0.5f) else AlertCardBg, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (isHealthy) "💖" else "⚠️", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Overall Health", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        text = "Status: ${livePlant.healthStatus} (${livePlant.healthScore}% score)",
                                        fontSize = 12.sp,
                                        color = if (isHealthy) HeroCardBg else AlertTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Action Care Buttons
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = EnvPillBg.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Perform Garden Actions", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.waterPlant(livePlant) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = HeroCardBg),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("💧 Water Me", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { viewModel.fertilizePlant(livePlant) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("🧪 Fertilize Me", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Plant Healthy?", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Switch(
                                    checked = livePlant.healthStatus.equals("Healthy", ignoreCase = true),
                                    onCheckedChange = { isChecked ->
                                        viewModel.updatePlantHealth(livePlant, isChecked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = HeroCardBg
                                    )
                                )
                            }
                        }
                    }

                    if (livePlant.disease != null && livePlant.disease != "null" && livePlant.disease.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Diagnosis History", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Issue: ${livePlant.disease}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                if (livePlant.severityLevel != null && livePlant.severityLevel != "null") {
                                    Text("Severity: ${livePlant.severityLevel}", color = Color(0xFFC62828))
                                }
                                
                                val symptomsList = livePlant.symptoms.split("|").filter { it.isNotBlank() }
                                if (symptomsList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Symptoms:", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                    symptomsList.forEach {
                                        Text("• $it", color = Color(0xFFC62828), fontSize = 14.sp)
                                    }
                                }

                                val treatmentList = livePlant.treatmentSteps.split("|").filter { it.isNotBlank() }
                                if (treatmentList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Treatment:", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                    treatmentList.forEach {
                                        Text("• $it", color = Color(0xFFC62828), fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    val careTipsList = livePlant.careTips.split("|").filter { it.isNotBlank() }
                    if (careTipsList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Actionable Care Tips", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.height(8.dp))
                                careTipsList.forEach { tip ->
                                    Text("• $tip", color = Color(0xFF2E7D32), fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = HeroCardBg)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Searching local garden...", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun Plant3DViewerWebView() {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                // Make webview background transparent
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                val htmlData = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body { margin: 0; padding: 0; background-color: transparent; }
                            model-viewer { 
                                width: 100%; 
                                height: 100vh; 
                                background-color: transparent; 
                            }
                        </style>
                        <script type="module" src="https://ajax.googleapis.com/ajax/libs/model-viewer/3.1.1/model-viewer.min.js"></script>
                    </head>
                    <body>
                        <model-viewer 
                            src="https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/main/2.0/Avocado/glTF-Binary/Avocado.glb" 
                            camera-controls 
                            auto-rotate 
                            shadow-intensity="1"
                            environment-image="neutral"
                            exposure="1.0">
                        </model-viewer>
                    </body>
                    </html>
                """.trimIndent()
                loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
