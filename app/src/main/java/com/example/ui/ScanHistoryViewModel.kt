package com.example.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

data class ScanHistoryItem(
    val id: String,
    val plantName: String,
    val species: String,
    val healthStatus: String,
    val disease: String?,
    val severityLevel: String?,
    val timestamp: Long
)

class ScanHistoryViewModel : ViewModel() {
    val history: StateFlow<List<ScanHistoryItem>> = ScanHistoryManager.history
    
    // Kept for backward compatibility
    val isLoading: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)
    
    // Kept for backward compatibility, though no longer needed
    // as it's synced automatically
    fun fetchScanHistory() {
        ScanHistoryManager.syncWithFirestore()
    }
}
