package com.example.ui

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object ScanHistoryManager {
    private const val TAG = "ScanHistoryManager"
    private lateinit var appContext: Context
    private val _history = MutableStateFlow<List<ScanHistoryItem>>(emptyList())
    val history = _history.asStateFlow()

    private val auth get() = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    private val firestore get() = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    private val storage get() = try { FirebaseStorage.getInstance() } catch (e: Exception) { null }

    private var snapshotListener: ListenerRegistration? = null

    private fun getUserId(): String? = auth?.currentUser?.uid

    private fun getPrefsForUser(uid: String?): SharedPreferences? {
        if (!::appContext.isInitialized || uid.isNullOrBlank()) return null
        val safeUid = uid.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return appContext.getSharedPreferences("scan_history_$safeUid", Context.MODE_PRIVATE)
    }

    fun initialize(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        val uid = getUserId()
        if (uid != null) {
            loadLocalHistory(uid)
            syncWithFirestore()
        } else {
            clearInMemory()
        }

        auth?.addAuthStateListener { firebaseAuth ->
            val currentUid = firebaseAuth.currentUser?.uid
            if (currentUid == null) {
                clearInMemory()
            } else {
                loadLocalHistory(currentUid)
                syncWithFirestore()
            }
        }
    }

    private fun clearInMemory() {
        snapshotListener?.remove()
        snapshotListener = null
        _history.value = emptyList()
    }

    fun clearLocalData() {
        clearInMemory()
    }

    private fun loadLocalHistory(uid: String) {
        val prefs = getPrefsForUser(uid) ?: return
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
            val imageUrl = prefs.getString("sh_${i}_image", null)
            val description = prefs.getString("sh_${i}_description", "") ?: ""
            val symptoms = prefs.getString("sh_${i}_symptoms", "") ?: ""
            val treatmentSteps = prefs.getString("sh_${i}_treatment", "") ?: ""
            val careTips = prefs.getString("sh_${i}_care_tips", "") ?: ""
            val healthScore = prefs.getInt("sh_${i}_health_score", 100)
            val wateringLevel = prefs.getString("sh_${i}_watering", "") ?: ""
            val sunlight = prefs.getString("sh_${i}_sunlight", "") ?: ""
            if (id.isNotBlank() && name.isNotBlank()) {
                list.add(
                    ScanHistoryItem(
                        id = id,
                        plantName = name,
                        species = species,
                        healthStatus = health,
                        disease = disease,
                        severityLevel = severity,
                        timestamp = ts,
                        imageUrl = imageUrl,
                        description = description,
                        symptoms = symptoms,
                        treatmentSteps = treatmentSteps,
                        careTips = careTips,
                        healthScore = healthScore,
                        wateringLevel = wateringLevel,
                        sunlight = sunlight
                    )
                )
            }
        }
        _history.value = list
    }

    private fun saveLocalHistory(uid: String, list: List<ScanHistoryItem>) {
        val prefs = getPrefsForUser(uid) ?: return
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
            editor.putString("sh_${i}_image", item.imageUrl)
            editor.putString("sh_${i}_description", item.description)
            editor.putString("sh_${i}_symptoms", item.symptoms)
            editor.putString("sh_${i}_treatment", item.treatmentSteps)
            editor.putString("sh_${i}_care_tips", item.careTips)
            editor.putInt("sh_${i}_health_score", item.healthScore)
            editor.putString("sh_${i}_watering", item.wateringLevel)
            editor.putString("sh_${i}_sunlight", item.sunlight)
        }
        editor.apply()
    }

    fun addScanResult(result: com.example.data.Plant) {
        val uid = getUserId() ?: return
        val item = ScanHistoryItem(
            id = java.util.UUID.randomUUID().toString(),
            plantName = result.name,
            species = result.species,
            healthStatus = result.healthStatus,
            disease = result.disease,
            severityLevel = result.severityLevel,
            timestamp = System.currentTimeMillis(),
            imageUrl = result.imageUri,
            description = result.description,
            symptoms = result.symptoms,
            treatmentSteps = result.treatmentSteps,
            careTips = result.careTips,
            healthScore = result.healthScore,
            wateringLevel = result.wateringLevel,
            sunlight = result.sunlight
        )
        val list = _history.value.toMutableList()
        list.add(0, item)
        _history.value = list
        saveLocalHistory(uid, list)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = firestore ?: return@launch
                val map = mutableMapOf<String, Any>(
                    "id" to item.id,
                    "plantName" to item.plantName,
                    "species" to item.species,
                    "healthStatus" to item.healthStatus,
                    "timestamp" to item.timestamp,
                    "description" to item.description,
                    "symptoms" to item.symptoms,
                    "treatmentSteps" to item.treatmentSteps,
                    "careTips" to item.careTips,
                    "healthScore" to item.healthScore,
                    "wateringLevel" to item.wateringLevel,
                    "sunlight" to item.sunlight
                )
                item.disease?.let { map["disease"] = it }
                item.severityLevel?.let { map["severityLevel"] = it }
                item.imageUrl?.let { map["imageUrl"] = it }

                db.collection("users").document(uid).collection("scan_history")
                    .document(item.id).set(map).await()

                if (!item.disease.isNullOrBlank() && item.disease != "None") {
                    val diseaseLog = map + mapOf("logType" to "Disease Identification", "userId" to uid)
                    db.collection("users").document(uid).collection("disease_logs").document(item.id).set(diseaseLog).await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving scan history to firestore", e)
            }
        }
    }

    suspend fun deleteScanItem(item: ScanHistoryItem) {
        val uid = getUserId() ?: return
        val list = _history.value.toMutableList()
        list.removeAll { it.id == item.id }
        _history.value = list
        saveLocalHistory(uid, list)

        try {
            val db = firestore
            db?.collection("users")?.document(uid)?.collection("scan_history")?.document(item.id)?.delete()?.await()
            db?.collection("users")?.document(uid)?.collection("disease_logs")?.document(item.id)?.delete()?.await()

            // Keep images still referenced by the plant collection.

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting scan item", e)
        }
    }

    fun syncWithFirestore() {
        val uid = getUserId() ?: run {
            clearInMemory()
            return
        }
        val db = firestore ?: return

        snapshotListener?.remove()
        snapshotListener = db.collection("users").document(uid).collection("scan_history")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                
                val cloudItems = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getString("id") ?: doc.id
                        val name = doc.getString("plantName") ?: return@mapNotNull null
                        val species = doc.getString("species") ?: ""
                        val health = doc.getString("healthStatus") ?: "Unknown"
                        val disease = doc.getString("disease")
                        val severity = doc.getString("severityLevel")
                        val ts = doc.getLong("timestamp") ?: 0L
                        val imageUrl = doc.getString("imageUrl")
                        val description = doc.getString("description") ?: ""
                        val symptoms = doc.getString("symptoms") ?: ""
                        val treatmentSteps = doc.getString("treatmentSteps") ?: ""
                        val careTips = doc.getString("careTips") ?: ""
                        val healthScore = doc.getLong("healthScore")?.toInt() ?: 100
                        val wateringLevel = doc.getString("wateringLevel") ?: ""
                        val sunlight = doc.getString("sunlight") ?: ""
                        ScanHistoryItem(
                            id = id,
                            plantName = name,
                            species = species,
                            healthStatus = health,
                            disease = disease,
                            severityLevel = severity,
                            timestamp = ts,
                            imageUrl = imageUrl,
                            description = description,
                            symptoms = symptoms,
                            treatmentSteps = treatmentSteps,
                            careTips = careTips,
                            healthScore = healthScore,
                            wateringLevel = wateringLevel,
                            sunlight = sunlight
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.timestamp }

                val currentUidNow = getUserId()
                if (currentUidNow == uid) {
                    _history.value = cloudItems
                    saveLocalHistory(uid, cloudItems)
                }
            }
    }
}
