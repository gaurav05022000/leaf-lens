package com.example.data

import android.content.Context
import android.util.Log
import com.example.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class PlantRepository private constructor(private val context: Context) {
    
    companion object {
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

    private val _allPlants = MutableStateFlow<List<Plant>>(emptyList())
    val allPlants: Flow<List<Plant>> = _allPlants.asStateFlow()
    
    private var snapshotListener: ListenerRegistration? = null

    init {
        startSync()
        auth?.addAuthStateListener {
            startSync()
        }
    }

    private fun startSync() {
        val user = auth?.currentUser
        val db = firestore
        if (user != null && db != null) {
            snapshotListener?.remove()
            snapshotListener = db.collection("users").document(user.uid).collection("plants")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("PlantRepository", "Listen failed.", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val plants = snapshot.documents.mapNotNull { doc ->
                            val p = doc.toObject(Plant::class.java)
                            p?.copy(firestoreId = doc.id)
                        }
                        _allPlants.value = plants
                    }
                }
        }
    }

    suspend fun insert(plant: Plant): Long {
        val id = System.currentTimeMillis() // fake ID for compat with old code where expected
        val plantWithFakeId = plant.copy(id = id.toInt())
        syncWithFirestore(plantWithFakeId)
        NotificationHelper.schedulePlantReminders(context, plantWithFakeId)
        return id
    }

    suspend fun update(plant: Plant) {
        syncWithFirestore(plant)
        NotificationHelper.schedulePlantReminders(context, plant)
    }

    suspend fun delete(plant: Plant) {
        NotificationHelper.cancelPlantReminders(context, plant.id)
        deleteFromFirestore(plant)
    }

    suspend fun deleteById(id: Int) {
        val plant = _allPlants.value.find { it.id == id }
        if (plant != null) {
            deleteFromFirestore(plant)
        }
        NotificationHelper.cancelPlantReminders(context, id)
    }

    fun getPlantById(id: Int): Flow<Plant?> {
        return _allPlants.map { list -> list.find { it.id == id } }
    }

    fun getPlantByName(name: String): Flow<Plant?> {
        return _allPlants.map { list -> list.find { it.name.equals(name, ignoreCase = true) } }
    }

    fun getPlantByNameSync(name: String): Plant? {
        return _allPlants.value.find { it.name.equals(name, ignoreCase = true) }
    }

    private suspend fun syncWithFirestore(plant: Plant) {
        try {
            Log.d("PlantRepository", "Syncing plant to Firestore: ${plant.name}")
            val user = auth?.currentUser
            val db = firestore
            if (user != null && db != null) {
                val documentId = plant.firestoreId ?: db.collection("users").document(user.uid).collection("plants").document().id
                val plantWithId = plant.copy(firestoreId = documentId)
                db.collection("users").document(user.uid).collection("plants")
                    .document(documentId).set(plantWithId, SetOptions.merge())
                Log.d("PlantRepository", "Successfully synced plant to Firestore with id=$documentId")
            } else {
                Log.w("PlantRepository", "Cannot sync plant to Firestore: user or db is null")
            }
        } catch (e: Exception) {
            Log.e("PlantRepository", "Error syncing with Firestore", e)
        }
    }

    private suspend fun deleteFromFirestore(plant: Plant) {
        try {
            val user = auth?.currentUser
            val db = firestore
            if (user != null && db != null && plant.firestoreId != null) {
                db.collection("users").document(user.uid).collection("plants")
                    .document(plant.firestoreId).delete()
            }
        } catch (e: Exception) {
            Log.e("PlantRepository", "Error deleting from Firestore", e)
        }
    }

    suspend fun syncAllFromFirestore() {
        startSync()
    }
}

