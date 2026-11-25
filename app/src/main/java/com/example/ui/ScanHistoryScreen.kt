package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.PlantRepository
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryScreen(onBackClick: () -> Unit, viewModel: ScanHistoryViewModel = viewModel()) {
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedItem by remember { mutableStateOf<ScanHistoryItem?>(null) }
    val context = LocalContext.current

    BackHandler(enabled = selectedItem != null) {
        selectedItem = null
    }

    if (selectedItem != null) {
        val item = selectedItem!!
        val matchedPlant = remember(item) {
            PlantRepository.getInstance(context).getPlantByNameSync(item.plantName)
        }

        ScanResultDetailView(
            item = item,
            matchedPlantDescription = matchedPlant?.description ?: "",
            matchedPlantSymptoms = matchedPlant?.symptoms ?: "",
            matchedPlantTreatment = matchedPlant?.treatmentSteps ?: "",
            matchedPlantCareTips = matchedPlant?.careTips ?: "",
            matchedPlantSunlight = matchedPlant?.sunlight ?: "",
            matchedPlantWatering = matchedPlant?.wateringLevel ?: "",
            onBack = { selectedItem = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Scan History", fontWeight = FontWeight.Bold, color = TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
                )
            },
            containerColor = BackgroundLight
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = GreenPrimary
                    )
                } else if (history.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextSecondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No scan history found.",
                            color = TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(history) { item ->
                            ScanHistoryCard(
                                item = item,
                                onClick = { selectedItem = item }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanHistoryCard(item: ScanHistoryItem, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val dateString = dateFormat.format(Date(item.timestamp))

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scan_history_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.plantName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Text(
                    text = dateString,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            
            if (!item.imageUrl.isNullOrBlank()) {
                PlantImage(
                    model = item.imageUrl,
                    contentDescription = item.plantName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .padding(vertical = 4.dp),
                    contentScale = ContentScale.Crop
                )
            }
            
            Text(
                text = item.species,
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusColor = when (item.healthStatus.lowercase(Locale.getDefault())) {
                    "healthy" -> GreenPrimary
                    "alert", "action required" -> Color(0xFFFF9800)
                    else -> AlertRed
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Health: ${item.healthStatus}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            if (!item.disease.isNullOrBlank() && item.disease != "None") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Disease: ${item.disease}",
                    fontSize = 14.sp,
                    color = AlertRed
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                color = TextSecondary.copy(alpha = 0.2f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to view scan diagnosis & care result",
                    fontSize = 12.sp,
                    color = GreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Details",
                    tint = GreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultDetailView(
    item: ScanHistoryItem,
    matchedPlantDescription: String = "",
    matchedPlantSymptoms: String = "",
    matchedPlantTreatment: String = "",
    matchedPlantCareTips: String = "",
    matchedPlantSunlight: String = "",
    matchedPlantWatering: String = "",
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.plantName, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy - hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date(item.timestamp))

        val effectiveDescription = item.description.ifBlank { matchedPlantDescription }
        val effectiveSymptoms = item.symptoms.ifBlank { matchedPlantSymptoms }
        val effectiveTreatment = item.treatmentSteps.ifBlank { matchedPlantTreatment }
        val effectiveCareTips = item.careTips.ifBlank { matchedPlantCareTips }
        val effectiveSunlight = item.sunlight.ifBlank { matchedPlantSunlight }
        val effectiveWatering = item.wateringLevel.ifBlank { matchedPlantWatering }

        val hasDisease = !item.disease.isNullOrBlank() && item.disease != "None" && item.disease != "null"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("scan_result_detail_screen")
        ) {
            // Photo if available
            if (!item.imageUrl.isNullOrBlank()) {
                PlantImage(
                    model = item.imageUrl,
                    contentDescription = item.plantName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .padding(bottom = 16.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = item.species,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Scanned on $dateString",
                fontSize = 12.5.sp,
                color = TextSecondary.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Health Status & Score Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasDisease) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                ),
                shape = RoundedCornerShape(14.dp)
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
                            text = "Health Status",
                            fontSize = 12.sp,
                            color = if (hasDisease) AlertRed else Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = item.healthStatus,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasDisease) Color(0xFFC62828) else Color(0xFF1B5E20)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (hasDisease) Color(0xFFFFCDD2) else Color(0xFFC8E6C9)
                    ) {
                        Text(
                            text = "${item.healthScore}% Health Score",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasDisease) Color(0xFFB71C1C) else Color(0xFF1B5E20),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Diagnosis Section
            if (hasDisease) {
                Text(
                    text = "Diagnosis",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Identified Issue: ${item.disease}",
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (!item.severityLevel.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Severity: ${item.severityLevel}",
                                color = Color(0xFFC62828),
                                fontSize = 13.sp
                            )
                        }

                        val symptomsList = effectiveSymptoms.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        if (symptomsList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Symptoms:",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828),
                                fontSize = 13.sp
                            )
                            symptomsList.forEach { symptom ->
                                Text(
                                    text = "• $symptom",
                                    color = Color(0xFFC62828),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Treatment Steps
            val treatmentList = effectiveTreatment.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (treatmentList.isNotEmpty()) {
                Text(
                    text = "Recommended Treatment",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        treatmentList.forEachIndexed { index, step ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(Color(0xFFF57F17), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = step,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF424242),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Care Tips
            val careTipsList = effectiveCareTips.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (careTipsList.isNotEmpty()) {
                Text(
                    text = "Care Recommendations",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        careTipsList.forEach { tip ->
                            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                Text("🌿", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tip,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 13.5.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Sunlight & Watering
            if (effectiveSunlight.isNotBlank() || effectiveWatering.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (effectiveSunlight.isNotBlank()) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sunlight", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(effectiveSunlight, fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                    }

                    if (effectiveWatering.isNotBlank()) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Watering", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(effectiveWatering, fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Description / About
            if (effectiveDescription.isNotBlank()) {
                Text(
                    text = "About",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = effectiveDescription,
                        fontSize = 13.5.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Back to Scan History Button
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Scan History", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
