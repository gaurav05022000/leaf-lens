package com.example.ui

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.ui.ScanHistoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object ScanHistoryManager {
    private lateinit var prefs: SharedPreferences

    private val _history = MutableStateFlow<List<ScanHistoryItem>>(emptyList())
    val history = _history.asStateFlow()

    private val auth get() = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    private val firestore get() = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }

    private var deviceId: String = ""
    private fun getUserId(): String = auth?.currentUser?.uid ?: deviceId

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences("flora_scan_history_prefs", Context.MODE_PRIVATE)
        deviceId = prefs.getString("device_id", null) ?: run {
            val newId = "device_" + java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
        
        loadLocalHistory()
        syncWithFirestore()
        
        auth?.addAuthStateListener { firebaseAuth ->
            syncWithFirestore()
        }
    }

    private fun loadLocalHistory() {
        val count = prefs.getInt("sh_count", 0)
        val list = mutableListOf<ScanHistoryItem>()
        for (i in 0 until count) {
            val id = prefs.getString("sh_${i}_id", "") ?: ""
            val name = prefs.getString("sh_${i}_name", "") ?: ""
            val species = prefs.getString("sh_${i}_species", "") ?: ""
            val health = prefs.getString("sh_${i}_health", "") ?: ""
            val disease = prefs.getString("sh_${i}_disease", null)
            val severity = prefs.getString("sh_${i}_severity", null)
            val ts = prefs.getLong("sh_${i}_ts", 0L)
            list.add(ScanHistoryItem(id, name, species, health, disease, severity, ts))
        }
        _history.value = list
    }

    private fun saveLocalHistory(list: List<ScanHistoryItem>) {
        val editor = prefs.edit()
        editor.putInt("sh_count", list.size)
        list.forEachIndexed { i, item ->
            editor.putString("sh_${i}_id", item.id)
            editor.putString("sh_${i}_name", item.plantName)
            editor.putString("sh_${i}_species", item.species)
            editor.putString("sh_${i}_health", item.healthStatus)
            editor.putString("sh_${i}_disease", item.disease)
            editor.putString("sh_${i}_severity", item.severityLevel)
            editor.putLong("sh_${i}_ts", item.timestamp)
        }
        editor.apply()
    }

    fun addScanResult(result: com.example.data.Plant) {
        val item = ScanHistoryItem(
            id = java.util.UUID.randomUUID().toString(),
            plantName = result.name,
            species = result.species,
            healthStatus = result.healthStatus,
            disease = result.disease,
            severityLevel = result.severityLevel,
            timestamp = System.currentTimeMillis()
        )
        val list = _history.value.toMutableList()
        list.add(0, item)
        _history.value = list
        saveLocalHistory(list)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ScanHistoryManager", "Saving scan history to Firestore...")
                val db = firestore ?: return@launch
                val map = mapOf<String, Any>(
                    "id" to item.id,
                    "plantName" to item.plantName,
                    "species" to item.species,
                    "healthStatus" to item.healthStatus,
                    "disease" to (item.disease ?: "None"),
                    "severityLevel" to (item.severityLevel ?: "None"),
                    "timestamp" to item.timestamp
                )
                
                val uid = getUserId()
                
                // Store user plant history
                db.collection("users").document(uid).collection("scan_history").document(item.id).set(map).await()
                
                // Store global scan history
                db.collection("scan_history").document(item.id).set(map).await()

                // Store identified disease logs
                if (!item.disease.isNullOrBlank() && item.disease != "None") {
                    val diseaseLog = map + mapOf("logType" to "Disease Identificaton", "userId" to uid)
                    db.collection("users").document(uid).collection("disease_logs").document(item.id).set(diseaseLog).await()
                    db.collection("disease_logs").document(item.id).set(diseaseLog).await()
                }
                Log.d("ScanHistoryManager", "Successfully saved scan history id=${item.id}")
            } catch (e: Exception) {
                Log.e("ScanHistoryManager", "Error saving scan history to firestore", e)
            }
        }
    }

    private var snapshotListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun clearLocalData() {
        snapshotListener?.remove()
        snapshotListener = null
        prefs.edit().clear().apply()
        _history.value = emptyList()
    }

    fun syncWithFirestore() {
        val uid = getUserId()
        val db = firestore ?: return
        snapshotListener?.remove()
        snapshotListener = db.collection("users").document(uid).collection("scan_history")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                
                val cloudItems = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("plantName") ?: return@mapNotNull null
                    val species = doc.getString("species") ?: ""
                    val health = doc.getString("healthStatus") ?: "Unknown"
                    val disease = doc.getString("disease")
                    val severity = doc.getString("severityLevel")
                    val ts = doc.getLong("timestamp") ?: 0L
                    ScanHistoryItem(id, name, species, health, disease, severity, ts)
                }.sortedByDescending { it.timestamp }

                val localItems = _history.value
                val merged = (cloudItems + localItems).distinctBy { it.id }.sortedByDescending { it.timestamp }
                
                _history.value = merged
                saveLocalHistory(merged)
                
                val cloudIds = cloudItems.map { it.id }.toSet()
                val missingInCloud = localItems.filter { !cloudIds.contains(it.id) }
                
                missingInCloud.forEach { item ->
                    val map = mapOf<String, Any>(
                        "id" to item.id,
                        "plantName" to item.plantName,
                        "species" to item.species,
                        "healthStatus" to item.healthStatus,
                        "disease" to (item.disease ?: "None"),
                        "severityLevel" to (item.severityLevel ?: "None"),
                        "timestamp" to item.timestamp
                    )
                    db.collection("users").document(uid).collection("scan_history").document(item.id).set(map)
                    db.collection("scan_history").document(item.id).set(map)
                    
                    if (!item.disease.isNullOrBlank() && item.disease != "None") {
                        val diseaseLog = map + mapOf("logType" to "Disease Identificaton", "userId" to uid)
                        db.collection("users").document(uid).collection("disease_logs").document(item.id).set(diseaseLog)
                        db.collection("disease_logs").document(item.id).set(diseaseLog)
                    }
                }
            }
    }
}
