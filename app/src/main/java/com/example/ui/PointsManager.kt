package com.example.ui

import android.content.Context
import android.util.Log
import com.example.data.room.AppDatabase
import com.example.data.room.LocalStateEntity
import com.example.data.room.PointTransactionEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object PointsManager {
    private val _availablePoints = MutableStateFlow(0)
    val availablePoints = _availablePoints.asStateFlow()

    private val _totalScans = MutableStateFlow(0)
    val totalScans = _totalScans.asStateFlow()

    data class PointTransaction(val title: String, val amount: Int, val timestamp: Long, val isEarned: Boolean)

    private val _transactions = MutableStateFlow<List<PointTransaction>>(emptyList())
    val transactions = _transactions.asStateFlow()

    private val auth get() = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    private val firestore get() = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    private lateinit var database: AppDatabase

    private var deviceId: String = ""
    private fun getUserId(): String = auth?.currentUser?.uid ?: deviceId

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("flora_points_prefs", Context.MODE_PRIVATE)
        deviceId = prefs.getString("device_id", null) ?: run {
            val newId = "device_" + java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
        database = AppDatabase.getDatabase(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            var localState = database.localStateDao().getLocalStateSync()
            if (localState == null || !localState.firstRunGranted) {
                localState = LocalStateEntity(id = 1, availablePoints = 50, totalScans = 0, firstRunGranted = true)
                database.localStateDao().updateLocalState(localState)
            }
            
            _availablePoints.value = localState.availablePoints
            _totalScans.value = localState.totalScans
            
            val dbTransactions = database.pointTransactionDao().getAllTransactionsSync()
            _transactions.value = dbTransactions.map { 
                PointTransaction(it.title, it.amount, it.timestamp, it.isEarned) 
            }

            // Sync with Firestore
            syncFromFirestore()
        }
        
        auth?.addAuthStateListener { firebaseAuth ->
            syncFromFirestore()
        }
    }

    private fun saveTransactionLocal(trx: PointTransaction) {
        val list = _transactions.value.toMutableList()
        list.add(0, trx)
        _transactions.value = list
        
        CoroutineScope(Dispatchers.IO).launch {
            database.pointTransactionDao().insertTransaction(
                PointTransactionEntity(title = trx.title, amount = trx.amount, timestamp = trx.timestamp, isEarned = trx.isEarned)
            )
        }
    }

    private fun saveTransaction(trx: PointTransaction) {
        saveTransactionLocal(trx)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("PointsManager", "Saving point transaction to Firestore...")
                val uid = getUserId()
                val db = firestore ?: return@launch
                val map = mapOf(
                    "title" to trx.title,
                    "amount" to trx.amount,
                    "timestamp" to trx.timestamp,
                    "isEarned" to trx.isEarned
                )
                db.collection("users").document(uid).collection("point_transactions").add(map).await()
                Log.d("PointsManager", "Successfully saved transaction to Firestore")
            } catch (e: Exception) {
                Log.e("PointsManager", "Error saving transaction to firestore", e)
            }
        }
    }

    private var pointsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var trxListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun clearLocalData() {
        pointsListener?.remove()
        pointsListener = null
        trxListener?.remove()
        trxListener = null
        
        CoroutineScope(Dispatchers.IO).launch {
            database.pointTransactionDao().clearAll()
            database.localStateDao().updateLocalState(LocalStateEntity(id = 1, availablePoints = 50, totalScans = 0, firstRunGranted = true))
            
            _availablePoints.value = 50
            _totalScans.value = 0
            _transactions.value = emptyList()
        }
    }

    fun syncFromFirestore() {
        val uid = getUserId()
        val db = firestore ?: return
        
        // Listen for points changes
        pointsListener?.remove()
        pointsListener = db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("PointsManager", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists() && snapshot.contains("availablePoints") && snapshot.contains("totalScans")) {
                val cloudPoints = snapshot.getLong("availablePoints")?.toInt() ?: 0
                val cloudScans = snapshot.getLong("totalScans")?.toInt() ?: 0
                
                // If cloud has advanced, pull
                if (cloudScans >= _totalScans.value || cloudPoints != _availablePoints.value) {
                    CoroutineScope(Dispatchers.IO).launch {
                        database.localStateDao().updateLocalState(
                            LocalStateEntity(id = 1, availablePoints = cloudPoints, totalScans = cloudScans, firstRunGranted = true)
                        )
                    }
                    
                    _availablePoints.value = cloudPoints
                    _totalScans.value = cloudScans
                }
            } else if (snapshot != null && !snapshot.exists()) {
                 if (!snapshot.metadata.isFromCache) {
                     // initialize document if not exists yet
                     syncToFirestore()
                 }
            }
        }

        // Listen for transactions changes
        trxListener?.remove()
        trxListener = db.collection("users").document(uid).collection("point_transactions")
            .addSnapshotListener { trxSnapshot, error ->
                 if (error != null || trxSnapshot == null) return@addSnapshotListener
                 
                 if (!trxSnapshot.isEmpty || _transactions.value.isNotEmpty()) {
                    val cloudTrx = trxSnapshot.documents.mapNotNull { doc ->
                       val title = doc.getString("title") ?: ""
                       val amount = doc.getLong("amount")?.toInt() ?: 0
                       val ts = doc.getLong("timestamp") ?: 0L
                       val isEarned = doc.getBoolean("isEarned") ?: false
                       PointTransaction(title, amount, ts, isEarned)
                    }
                    
                    val localTrx = _transactions.value
                    val merged = (cloudTrx + localTrx).distinctBy { it.timestamp }.sortedByDescending { it.timestamp }
                    
                    _transactions.value = merged
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        database.pointTransactionDao().clearAll()
                        database.pointTransactionDao().insertTransactions(
                            merged.map { PointTransactionEntity(title = it.title, amount = it.amount, timestamp = it.timestamp, isEarned = it.isEarned) }
                        )
                    }
                    
                    val cloudTs = cloudTrx.map { it.timestamp }.toSet()
                    val missingInCloud = localTrx.filter { !cloudTs.contains(it.timestamp) }
                    missingInCloud.forEach { trx ->
                        db.collection("users").document(uid).collection("point_transactions").add(
                            mapOf(
                                "title" to trx.title,
                                "amount" to trx.amount,
                                "timestamp" to trx.timestamp,
                                "isEarned" to trx.isEarned
                            )
                        )
                    }
                }
            }
    }

    private fun syncToFirestore() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("PointsManager", "Syncing points to Firestore...")
                val uid = getUserId()
                val db = firestore ?: return@launch
                val data = mapOf(
                    "availablePoints" to _availablePoints.value,
                    "totalScans" to _totalScans.value
                )
                db.collection("users").document(uid).set(data, SetOptions.merge()).await()
                Log.d("PointsManager", "Successfully synced points to Firestore")
            } catch (e: Exception) {
                Log.e("PointsManager", "Error updating points", e)
            }
        }
    }

    fun deductPoints(amount: Int): Boolean {
        val current = _availablePoints.value
        if (current >= amount) {
            val newPoints = current - amount
            _availablePoints.value = newPoints
            CoroutineScope(Dispatchers.IO).launch {
                database.localStateDao().updateLocalState(LocalStateEntity(id = 1, availablePoints = newPoints, totalScans = _totalScans.value, firstRunGranted = true))
            }
            saveTransaction(PointTransaction("Used AI / Scan", amount, System.currentTimeMillis(), false))
            syncToFirestore()
            return true
        }
        return false
    }

    fun addPoints(amount: Int, reason: String = "Purchase") {
        val newPoints = _availablePoints.value + amount
        _availablePoints.value = newPoints
        CoroutineScope(Dispatchers.IO).launch {
            database.localStateDao().updateLocalState(LocalStateEntity(id = 1, availablePoints = newPoints, totalScans = _totalScans.value, firstRunGranted = true))
        }
        saveTransaction(PointTransaction(reason, amount, System.currentTimeMillis(), true))
        syncToFirestore()
    }

    fun incrementScans() {
        val newScans = _totalScans.value + 1
        _totalScans.value = newScans
        CoroutineScope(Dispatchers.IO).launch {
            database.localStateDao().updateLocalState(LocalStateEntity(id = 1, availablePoints = _availablePoints.value, totalScans = newScans, firstRunGranted = true))
        }
        syncToFirestore()
    }
}
