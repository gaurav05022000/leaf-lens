package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class PlantRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "PlantRepository"
        @Volatile
        private var INSTANCE: PlantRepository? = null

        fun getInstance(context: Context): PlantRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlantRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val firestore get() = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    private val auth get() = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    private val storage get() = try { FirebaseStorage.getInstance() } catch (e: Exception) { null }

    private val _allPlants = MutableStateFlow<List<Plant>>(emptyList())
    val allPlants: Flow<List<Plant>> = _allPlants.asStateFlow()

    private var snapshotListener: ListenerRegistration? = null

    init {
        val initialUid = auth?.currentUser?.uid
        if (initialUid != null) {
            _allPlants.value = loadLocalPlants(initialUid)
            startSync()
        }

        auth?.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                snapshotListener?.remove()
                snapshotListener = null
                _allPlants.value = emptyList()
            } else {
                _allPlants.value = loadLocalPlants(uid)
                startSync()
            }
        }
    }

    private fun getPrefsForUser(uid: String?): SharedPreferences? {
        if (uid.isNullOrBlank()) return null
        val safeUid = uid.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return context.getSharedPreferences("local_plants_$safeUid", Context.MODE_PRIVATE)
    }

    private fun loadLocalPlants(uid: String): List<Plant> {
        return try {
            val prefs = getPrefsForUser(uid) ?: return emptyList()
            val jsonStr = prefs.getString("plants_json", null) ?: return emptyList()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Plant>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Plant(
                        id = obj.optInt("id", i + 1),
                        name = obj.optString("name", "Unknown"),
                        species = obj.optString("species", ""),
                        healthStatus = obj.optString("healthStatus", "Healthy"),
                        healthScore = obj.optInt("healthScore", 100),
                        wateringLevel = obj.optString("wateringLevel", "Medium"),
                        wateringScore = obj.optInt("wateringScore", 100),
                        sunlight = obj.optString("sunlight", "Indirect"),
                        sunlightScore = obj.optInt("sunlightScore", 100),
                        nextWateringTimeMs = obj.optLong("nextWateringTimeMs", 0L),
                        nextFeedingTimeMs = obj.optLong("nextFeedingTimeMs", 0L),
                        disease = obj.optString("disease").takeIf { it.isNotBlank() },
                        severityLevel = obj.optString("severityLevel").takeIf { it.isNotBlank() },
                        symptoms = obj.optString("symptoms", ""),
                        treatmentSteps = obj.optString("treatmentSteps", ""),
                        careTips = obj.optString("careTips", ""),
                        imageUri = obj.optString("imageUri").takeIf { it.isNotBlank() },
                        description = obj.optString("description", ""),
                        similarImageUris = obj.optString("similarImageUris", ""),
                        wateringIntervalDays = obj.optInt("wateringIntervalDays", 7),
                        fertilizingIntervalDays = obj.optInt("fertilizingIntervalDays", 30),
                        isPremium = obj.optBoolean("isPremium", false),
                        firestoreId = obj.optString("firestoreId").takeIf { it.isNotBlank() }
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load local plants", e)
            emptyList()
        }
    }

    private fun saveLocalPlants(uid: String, list: List<Plant>) {
        try {
            val prefs = getPrefsForUser(uid) ?: return
            val jsonArray = JSONArray()
            for (plant in list) {
                val obj = JSONObject().apply {
                    put("id", plant.id)
                    put("name", plant.name)
                    put("species", plant.species)
                    put("healthStatus", plant.healthStatus)
                    put("healthScore", plant.healthScore)
                    put("wateringLevel", plant.wateringLevel)
                    put("wateringScore", plant.wateringScore)
                    put("sunlight", plant.sunlight)
                    put("sunlightScore", plant.sunlightScore)
                    put("nextWateringTimeMs", plant.nextWateringTimeMs)
                    put("nextFeedingTimeMs", plant.nextFeedingTimeMs)
                    put("disease", plant.disease ?: "")
                    put("severityLevel", plant.severityLevel ?: "")
                    put("symptoms", plant.symptoms)
                    put("treatmentSteps", plant.treatmentSteps)
                    put("careTips", plant.careTips)
                    put("imageUri", plant.imageUri ?: "")
                    put("description", plant.description)
                    put("similarImageUris", plant.similarImageUris)
                    put("wateringIntervalDays", plant.wateringIntervalDays)
                    put("fertilizingIntervalDays", plant.fertilizingIntervalDays)
                    put("isPremium", plant.isPremium)
                    put("firestoreId", plant.firestoreId ?: "")
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("plants_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save local plants", e)
        }
    }

    private fun startSync() {
        val user = auth?.currentUser ?: return
        val db = firestore ?: return
        val currentUid = user.uid

        snapshotListener?.remove()
        snapshotListener = db.collection("users").document(currentUid).collection("plants")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Plant listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val firestorePlants = snapshot.documents.mapNotNull { doc ->
                        try {
                            val p = doc.toObject(Plant::class.java)
                            val fallbackId = (doc.id.hashCode() and 0x7FFFFFFF).let { if (it <= 0) 1 else it }
                            val validId = if ((p?.id ?: 0) > 0) p!!.id else fallbackId
                            p?.copy(
                                id = validId,
                                firestoreId = doc.id,
                                name = p.name ?: "Unknown",
                                species = p.species ?: "",
                                healthStatus = p.healthStatus ?: "Unknown"
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse plant doc", e)
                            null
                        }
                    }
                    val nowUid = auth?.currentUser?.uid
                    if (nowUid == currentUid) {
                        _allPlants.value = firestorePlants
                        saveLocalPlants(currentUid, firestorePlants)
                    }
                }
            }
    }

    suspend fun insert(plant: Plant): Long {
        val uid = auth?.currentUser?.uid ?: "local_user"
        val assignedId = if (plant.id > 0) plant.id else ((System.currentTimeMillis() % 1_000_000_000L).toInt().let { if (it <= 0) kotlin.random.Random.nextInt(1, 999999999) else it })
        val plantWithId = plant.copy(id = assignedId)

        val currentList = _allPlants.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == assignedId || (it.firestoreId != null && it.firestoreId == plantWithId.firestoreId) }
        if (existingIndex >= 0) {
            currentList[existingIndex] = plantWithId
        } else {
            currentList.add(0, plantWithId)
        }
        _allPlants.value = currentList
        saveLocalPlants(uid, currentList)

        syncWithFirestore(plantWithId)
        NotificationHelper.schedulePlantReminders(context, plantWithId)
        return assignedId.toLong()
    }

    suspend fun update(plant: Plant) {
        val uid = auth?.currentUser?.uid ?: "local_user"
        val currentList = _allPlants.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == plant.id || (it.firestoreId != null && it.firestoreId == plant.firestoreId) }
        if (index >= 0) {
            currentList[index] = plant
            _allPlants.value = currentList
            saveLocalPlants(uid, currentList)
        }
        syncWithFirestore(plant)
        NotificationHelper.schedulePlantReminders(context, plant)
    }

    suspend fun delete(plant: Plant) {
        val uid = auth?.currentUser?.uid ?: "local_user"
        val currentList = _allPlants.value.toMutableList()
        currentList.removeAll { it.id == plant.id || (it.firestoreId != null && it.firestoreId == plant.firestoreId) }
        _allPlants.value = currentList
        saveLocalPlants(uid, currentList)
        NotificationHelper.cancelPlantReminders(context, plant.id)
        deleteFromFirestore(plant)
    }

    suspend fun deleteById(id: Int) {
        val plant = _allPlants.value.find { it.id == id }
        val uid = auth?.currentUser?.uid ?: "local_user"
        val currentList = _allPlants.value.toMutableList()
        currentList.removeAll { it.id == id }
        _allPlants.value = currentList
        saveLocalPlants(uid, currentList)
        if (plant != null) {
            deleteFromFirestore(plant)
        }
        NotificationHelper.cancelPlantReminders(context, id)
    }

    fun getPlantById(id: Int): Flow<Plant?> {
        return _allPlants.map { list -> list.find { it.id == id } ?: list.firstOrNull { id <= 0 } }
    }

    fun getPlantByName(name: String): Flow<Plant?> {
        return _allPlants.map { list -> list.find { it.name.equals(name, ignoreCase = true) } }
    }

    fun getPlantByNameSync(name: String): Plant? {
        return _allPlants.value.find { it.name.equals(name, ignoreCase = true) }
    }

    private suspend fun syncWithFirestore(plant: Plant) {
        try {
            val user = auth?.currentUser ?: return
            val db = firestore ?: return
            val documentId = plant.firestoreId ?: db.collection("users").document(user.uid).collection("plants").document().id
            val sanitizedPlant = plant.copy(
                name = plant.name.trim().take(100),
                species = plant.species.trim().take(100),
                description = plant.description.trim().take(3000),
                disease = plant.disease?.trim()?.take(100),
                firestoreId = documentId
            )
            db.collection("users").document(user.uid).collection("plants")
                .document(documentId).set(sanitizedPlant, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing with Firestore", e)
        }
    }

    private suspend fun deleteFromFirestore(plant: Plant) {
        try {
            val user = auth?.currentUser ?: return
            val db = firestore ?: return
            if (plant.firestoreId != null) {
                db.collection("users").document(user.uid).collection("plants")
                    .document(plant.firestoreId).delete().await()
            }
            // Also cleanup image from storage if user-owned
            plant.imageUri?.let { uri ->
                if (uri.contains("firebasestorage.googleapis.com") && uri.contains(user.uid)) {
                    try {
                        storage?.getReferenceFromUrl(uri)?.delete()?.await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete storage image: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting from Firestore", e)
        }
    }

    fun clearLocalData() {
        snapshotListener?.remove()
        snapshotListener = null
        _allPlants.value = emptyList()
    }

    suspend fun syncAllFromFirestore() {
        startSync()
    }
}
