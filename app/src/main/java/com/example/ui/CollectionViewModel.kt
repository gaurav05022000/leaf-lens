package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Plant
import com.example.data.PlantRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CollectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlantRepository.getInstance(application)

    val plants: StateFlow<List<Plant>> = repository.allPlants.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val isLoading: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    fun deletePlant(plant: Plant) {
        viewModelScope.launch {
            repository.delete(plant)
        }
    }
}
