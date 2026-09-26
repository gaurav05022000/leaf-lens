    fun addScanResult(result: com.example.data.Plant) {
        val item = ScanHistoryItem(
            id = java.util.UUID.randomUUID().toString(),
            plantName = result.name,
            species = result.species,
            healthStatus = result.healthStatus,
            disease = result.disease,
            severityLevel = result.severityLevel,
            timestamp = System.currentTimeMillis(),
            imageUrl = result.imageUri
        )
        val list = _history.value.toMutableList()
        list.add(0, item)
        _history.value = list
        saveLocalHistory(list)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ScanHistoryManager", "Saving scan history to Firestore...")
                val db = firestore ?: return@launch
                val map = mutableMapOf<String, Any>(
                    "id" to item.id,
                    "plantName" to item.plantName,
                    "species" to item.species,
                    "healthStatus" to item.healthStatus,
                    "timestamp" to item.timestamp
                )
                item.disease?.let { map["disease"] = it }
                item.severityLevel?.let { map["severityLevel"] = it }
                item.imageUrl?.let { map["imageUrl"] = it }
                
                db.collection("users").document(getUserId()).collection("scan_history")
                    .document(item.id).set(map)
