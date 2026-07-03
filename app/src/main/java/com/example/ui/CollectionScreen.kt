package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Plant
import com.example.ui.theme.*

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    viewModel: HomeViewModel = viewModel(),
    collectionViewModel: CollectionViewModel = viewModel(),
    onPlantClick: (Plant) -> Unit = {}
) {
    val localPlants by viewModel.allPlants.collectAsState()
    val firestorePlants by collectionViewModel.plants.collectAsState()
    val isLoading by collectionViewModel.isLoading.collectAsState()
    
    // Merge or prioritize firestore
    val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch(e: Exception) { null }
    val displayPlants = if (auth != null && firestorePlants.isNotEmpty()) firestorePlants else localPlants

    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }

    var showAddPlantDialog by remember { mutableStateOf(false) }

    // Manual Plant Form States
    var newPlantName by remember { mutableStateOf("") }
    var newPlantSpecies by remember { mutableStateOf("") }
    var lastWateredDays by remember { mutableStateOf(0f) }
    var lastFertilizedDays by remember { mutableStateOf(0f) }
    var selectedSunlight by remember { mutableStateOf("Indirect") }
    var selectedHealth by remember { mutableStateOf("Healthy") }

    var editingPlant by remember { mutableStateOf<Plant?>(null) }

    if (showAddPlantDialog || editingPlant != null) {
        val isEditing = editingPlant != null
        AlertDialog(
            onDismissRequest = { 
                showAddPlantDialog = false 
                editingPlant = null
            },
            title = { Text(if (isEditing) "Edit Plant" else "Add New Garden Plant", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newPlantName,
                        onValueChange = { newPlantName = it },
                        label = { Text("Plant Nickname (e.g., Pothos)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeroCardBg,
                            unfocusedBorderColor = SurfaceVariant
                        )
                    )

                    OutlinedTextField(
                        value = newPlantSpecies,
                        onValueChange = { newPlantSpecies = it },
                        label = { Text("Species (e.g., Epipremnum aureum)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeroCardBg,
                            unfocusedBorderColor = SurfaceVariant
                        )
                    )

                    // Last watered Slider
                    Column {
                        Text(
                            text = "Last Watered: ${if (lastWateredDays.toInt() == 0) "Today" else "${lastWateredDays.toInt()} days ago"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Slider(
                            value = lastWateredDays,
                            onValueChange = { lastWateredDays = it },
                            valueRange = 0f..14f,
                            steps = 13,
                            colors = SliderDefaults.colors(
                                thumbColor = HeroCardBg,
                                activeTrackColor = HeroCardBg,
                                inactiveTrackColor = SurfaceVariant
                            )
                        )
                    }

                    // Last fertilized Slider
                    Column {
                        Text(
                            text = "Last Fertilized: ${if (lastFertilizedDays.toInt() == 0) "Today" else "${lastFertilizedDays.toInt()} days ago"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Slider(
                            value = lastFertilizedDays,
                            onValueChange = { lastFertilizedDays = it },
                            valueRange = 0f..30f,
                            steps = 29,
                            colors = SliderDefaults.colors(
                                thumbColor = HeroCardBg,
                                activeTrackColor = HeroCardBg,
                                inactiveTrackColor = SurfaceVariant
                            )
                        )
                    }

                    // Sunlight Exposure Selection
                    Column {
                        Text("Sunlight Needs:", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Bright Direct", "Indirect", "Low Light").forEach { option ->
                                val isSelected = selectedSunlight == option
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSunlight = option },
                                    label = { Text(option) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = HeroCardBg.copy(alpha = 0.2f),
                                        selectedLabelColor = HeroCardBg
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Health Status Selection
                    Column {
                        Text("Current Health:", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Healthy", "Needs Care").forEach { option ->
                                val isSelected = selectedHealth == option
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedHealth = option },
                                    label = { Text(option) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = HeroCardBg.copy(alpha = 0.2f),
                                        selectedLabelColor = HeroCardBg
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlantName.isNotBlank() && newPlantSpecies.isNotBlank()) {
                            if (isEditing) {
                                viewModel.updatePlant(
                                    plant = editingPlant!!,
                                    name = newPlantName,
                                    species = newPlantSpecies,
                                    lastWateredDaysAgo = lastWateredDays.toInt(),
                                    lastFertilizedDaysAgo = lastFertilizedDays.toInt(),
                                    healthStatus = selectedHealth,
                                    sunlight = selectedSunlight
                                )
                            } else {
                                viewModel.addPlant(
                                    name = newPlantName,
                                    species = newPlantSpecies,
                                    lastWateredDaysAgo = lastWateredDays.toInt(),
                                    lastFertilizedDaysAgo = lastFertilizedDays.toInt(),
                                    healthStatus = selectedHealth,
                                    sunlight = selectedSunlight
                                )
                            }
                            // Reset form fields
                            newPlantName = ""
                            newPlantSpecies = ""
                            lastWateredDays = 0f
                            lastFertilizedDays = 0f
                            selectedSunlight = "Indirect"
                            selectedHealth = "Healthy"
                            showAddPlantDialog = false
                            editingPlant = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HeroCardBg)
                ) {
                    Text(if (isEditing) "Save" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddPlantDialog = false 
                    editingPlant = null
                    newPlantName = ""
                    newPlantSpecies = ""
                    lastWateredDays = 0f
                    lastFertilizedDays = 0f
                    selectedSunlight = "Indirect"
                    selectedHealth = "Healthy"
                }) {
                    Text("Cancel", color = HeroCardBg)
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { showAddPlantDialog = true },
                    containerColor = HeroCardBg,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Plant")
                }
            }
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = BackgroundLight) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("My Plants", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BackgroundLight,
                        titleContentColor = TextPrimary
                    ),
                    actions = {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp), color = HeroCardBg)
                        }
                    }
                )
                
            AdBanner(modifier = Modifier.padding(bottom = 8.dp))
            
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = BackgroundLight,
                contentColor = HeroCardBg,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = HeroCardBg
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Collection", color = if (selectedTabIndex == 0) HeroCardBg else TextSecondary) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Tasks", color = if (selectedTabIndex == 1) HeroCardBg else TextSecondary) }
                )
            }

            if (selectedTabIndex == 0) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search plants by name, species or status...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = SurfaceVariant,
                        focusedBorderColor = HeroCardBg
                    )
                )
                
                val filteredPlants = displayPlants.filter { 
                    it.name.contains(searchQuery, ignoreCase = true) || 
                    it.species.contains(searchQuery, ignoreCase = true) ||
                    it.healthStatus.contains(searchQuery, ignoreCase = true)
                }
                
                if (filteredPlants.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No plants found in your collection.", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredPlants) { plant ->
                            PlantListRow(
                                plant = plant, 
                                onPlantClick = onPlantClick,
                                onDeleteClick = { 
                                    if (it.firestoreId != null) {
                                        collectionViewModel.deletePlant(it)
                                    } else {
                                        viewModel.deletePlant(it)
                                    }
                                },
                                onEditClick = {
                                    editingPlant = it
                                    newPlantName = it.name
                                    newPlantSpecies = it.species
                                    selectedHealth = it.healthStatus
                                    selectedSunlight = it.sunlight
                                    // Calculate approx days ago, simplify by defaulting to 0 for edit if we don't calculate precisely
                                    val timeNow = System.currentTimeMillis()
                                    // Assuming default intervals based on sunlight: 
                                    // next = now + (interval - daysAgo) * 86400000 -> daysAgo = interval - (next - now)/86400000
                                    
                                    val interval = when (it.sunlight) {
                                        "Bright Direct" -> 4L
                                        "Low Light" -> 14L
                                        else -> 7L
                                    }
                                    val daysUntilWater = (it.nextWateringTimeMs - timeNow) / 86400000L
                                    lastWateredDays = (interval - daysUntilWater).coerceIn(0L, 14L).toFloat()
                                    
                                    val daysUntilFeed = (it.nextFeedingTimeMs - timeNow) / 86400000L
                                    lastFertilizedDays = (30L - daysUntilFeed).coerceIn(0L, 30L).toFloat()
                                }
                            )
                        }
                    }
                }
            } else {
                TasksSection(plants = displayPlants)
            }
        }
    }
}
}

@Composable
fun TasksSection(plants: List<Plant>) {
    val timeNow = System.currentTimeMillis()
    val waterTasks = plants.filter { it.nextWateringTimeMs < timeNow }.map { "Water your ${it.name}" }
    val fertilizeTasks = plants.filter { it.nextFeedingTimeMs < timeNow }.map { "Fertilize your ${it.name}" }
    
    val allTasks = waterTasks + fertilizeTasks
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your Pending Tasks", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (allTasks.isEmpty()) {
            Text("All caught up! No tasks for today.", color = TextSecondary)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allTasks) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = false, 
                                onCheckedChange = { },
                                colors = CheckboxDefaults.colors(checkedColor = HeroCardBg, uncheckedColor = TextSecondary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(task, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlantListRow(plant: Plant, onPlantClick: (Plant) -> Unit = {}, onDeleteClick: (Plant) -> Unit, onEditClick: (Plant) -> Unit) {
    val healthColor = if (plant.healthStatus.equals("Healthy", ignoreCase = true)) com.example.ui.theme.HeroCardBg else com.example.ui.theme.AlertTextPrimary
    val bgColor = if (plant.healthStatus.equals("Healthy", ignoreCase = true)) com.example.ui.theme.SurfaceVariant else com.example.ui.theme.AlertCardBg
    
    val timeNow = System.currentTimeMillis()
    val needsWater = plant.nextWateringTimeMs < timeNow
    val needsFertilizer = plant.nextFeedingTimeMs < timeNow
    
    val waterText = if (needsWater) "Needs Water Now!" else "Water in " + getDaysRemaining(plant.nextWateringTimeMs, timeNow)
    val fertilizeText = if (needsFertilizer) "Needs Fertilizer Now!" else "Fertilize in " + getDaysRemaining(plant.nextFeedingTimeMs, timeNow)

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Plant") },
            text = { Text("Are you sure you want to delete ${plant.name}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { 
                        showDeleteConfirm = false
                        onDeleteClick(plant) 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onPlantClick(plant) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (plant.imageUri != null) {
                    coil.compose.AsyncImage(
                        model = plant.imageUri,
                        contentDescription = "Plant image",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(60.dp).background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌿", fontSize = 28.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(plant.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(plant.species, fontSize = 12.sp, color = TextSecondary)
                    if (plant.description.isNotBlank()) {
                         Text(plant.description, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                
                Row {
                    IconButton(onClick = { onEditClick(plant) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Plant", tint = TextSecondary)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Plant", tint = TextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💧", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = waterText, 
                        fontSize = 12.sp, 
                        color = if (needsWater) com.example.ui.theme.AlertTextPrimary else TextSecondary,
                        fontWeight = if (needsWater) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧪", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = fertilizeText, 
                        fontSize = 12.sp, 
                        color = if (needsFertilizer) com.example.ui.theme.AlertTextPrimary else TextSecondary,
                        fontWeight = if (needsFertilizer) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

fun getDaysRemaining(targetTimeMs: Long, currentTimeMs: Long): String {
    val diff = targetTimeMs - currentTimeMs
    if (diff <= 0) return "0 days"
    val days = diff / (1000 * 60 * 60 * 24)
    return if (days > 0) "$days days" else "Hours away"
}

