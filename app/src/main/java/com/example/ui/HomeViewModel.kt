package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Plant
import com.example.data.PlantRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlantRepository
    
    val allPlants: StateFlow<List<Plant>>

    init {
        repository = PlantRepository.getInstance(application)
        
        allPlants = repository.allPlants.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        seedInitialData()
    }

    fun addPlant(
        name: String,
        species: String,
        lastWateredDaysAgo: Int,
        lastFertilizedDaysAgo: Int,
        healthStatus: String = "Healthy",
        sunlight: String = "Indirect"
    ) {
        viewModelScope.launch {
            val wateringInterval = when (sunlight) {
                "Bright Direct" -> 4L
                "Low Light" -> 14L
                else -> 7L
            }
            val feedingInterval = 30L // default monthly

            val nextWateringTimeMs = System.currentTimeMillis() + (wateringInterval - lastWateredDaysAgo) * 86400000L
            val nextFeedingTimeMs = System.currentTimeMillis() + (feedingInterval - lastFertilizedDaysAgo) * 86400000L
            
            val plant = Plant(
                name = name,
                species = species,
                healthStatus = healthStatus,
                healthScore = if (healthStatus.lowercase() == "healthy") 95 else 60,
                wateringLevel = if (wateringInterval <= 4) "High" else if (wateringInterval >= 14) "Low" else "Medium",
                wateringScore = (100 - lastWateredDaysAgo * 12).coerceIn(0, 100),
                sunlight = sunlight,
                sunlightScore = if (sunlight == "Bright Direct") 90 else 75,
                nextWateringTimeMs = nextWateringTimeMs,
                nextFeedingTimeMs = nextFeedingTimeMs
            )
            repository.insert(plant)
        }
    }

    fun updatePlant(
        plant: Plant,
        name: String,
        species: String,
        lastWateredDaysAgo: Int,
        lastFertilizedDaysAgo: Int,
        healthStatus: String = "Healthy",
        sunlight: String = "Indirect"
    ) {
        viewModelScope.launch {
            val wateringInterval = when (sunlight) {
                "Bright Direct" -> 4L
                "Low Light" -> 14L
                else -> 7L
            }
            val feedingInterval = 30L // default monthly

            val nextWateringTimeMs = System.currentTimeMillis() + (wateringInterval - lastWateredDaysAgo) * 86400000L
            val nextFeedingTimeMs = System.currentTimeMillis() + (feedingInterval - lastFertilizedDaysAgo) * 86400000L
            
            val updated = plant.copy(
                name = name,
                species = species,
                healthStatus = healthStatus,
                healthScore = if (healthStatus.lowercase() == "healthy") 95 else 60,
                wateringLevel = if (wateringInterval <= 4) "High" else if (wateringInterval >= 14) "Low" else "Medium",
                wateringScore = (100 - lastWateredDaysAgo * 12).coerceIn(0, 100),
                sunlight = sunlight,
                sunlightScore = if (sunlight == "Bright Direct") 90 else 75,
                nextWateringTimeMs = nextWateringTimeMs,
                nextFeedingTimeMs = nextFeedingTimeMs
            )
            repository.update(updated)
        }
    }

    fun waterPlant(plant: Plant, actionTimeMs: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val interval = when (plant.wateringLevel) {
                "High" -> 4L
                "Low" -> 14L
                else -> 7L
            }
            val updated = plant.copy(
                nextWateringTimeMs = actionTimeMs + interval * 86400000L,
                wateringScore = 100
            )
            repository.update(updated)
        }
    }

    fun fertilizePlant(plant: Plant, actionTimeMs: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val updated = plant.copy(
                nextFeedingTimeMs = actionTimeMs + 30L * 86400000L
            )
            repository.update(updated)
        }
    }

    fun updatePlantHealth(plant: Plant, isHealthy: Boolean) {
        viewModelScope.launch {
            val updated = plant.copy(
                healthStatus = if (isHealthy) "Healthy" else "Needs Care",
                healthScore = if (isHealthy) 95 else 50
            )
            repository.update(updated)
        }
    }

    fun getPlantByNameFlow(name: String) = repository.getPlantByName(name)

    var weatherData = androidx.compose.runtime.mutableStateOf<com.example.api.WeatherResponse?>(null)
        private set

    fun fetchWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                weatherData.value = com.example.api.WeatherApi.retrofitService.getCurrentWeather(lat, lon)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePlant(plant: Plant) {
        viewModelScope.launch {
            repository.delete(plant)
        }
    }

    private fun seedInitialData() {
        viewModelScope.launch {
            repository.syncAllFromFirestore()
        }
    }
}
