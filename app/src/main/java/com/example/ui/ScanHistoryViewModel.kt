package com.example.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

data class ScanHistoryItem(
    val id: String,
    val plantName: String,
    val species: String,
    val healthStatus: String,
    val disease: String?,
    val severityLevel: String?,
    val timestamp: Long,
    val imageUrl: String? = null,
    val description: String = "",
    val symptoms: String = "",
    val treatmentSteps: String = "",
    val careTips: String = "",
    val healthScore: Int = 100,
    val wateringLevel: String = "",
    val sunlight: String = ""
)

class ScanHistoryViewModel : ViewModel() {
    val history: StateFlow<List<ScanHistoryItem>> = ScanHistoryManager.history
    
    // Kept for backward compatibility
    val isLoading: StateFlow<Boolean> = MutableStateFlow(false)
    
    // Kept for backward compatibility, though no longer needed
    // as it's synced automatically
    fun fetchScanHistory() {
        ScanHistoryManager.syncWithFirestore()
    }
}
