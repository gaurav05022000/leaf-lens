                val cloudItems = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("plantName") ?: return@mapNotNull null
                    val species = doc.getString("species") ?: ""
                    val health = doc.getString("healthStatus") ?: "Unknown"
                    val disease = doc.getString("disease")
                    val severity = doc.getString("severityLevel")
                    val ts = doc.getLong("timestamp") ?: 0L
                    val imageUrl = doc.getString("imageUrl")
                    ScanHistoryItem(id, name, species, health, disease, severity, ts, imageUrl)
                }.sortedByDescending { it.timestamp }
                val localItems = _history.value
                val merged = (cloudItems + localItems).distinctBy { it.id }.sortedByDescending { it.timestamp }
                
                _history.value = merged
                saveLocalHistory(merged)
                
                val cloudIds = cloudItems.map { it.id }.toSet()
                val missingInCloud = localItems.filter { !cloudIds.contains(it.id) }
                
                missingInCloud.forEach { item ->
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
                    
                    db.collection("users").document(uid).collection("scan_history").document(item.id).set(map)
                    db.collection("scan_history").document(item.id).set(map)
                    
                    if (!item.disease.isNullOrBlank() && item.disease != "None") {
                        val diseaseLog = map + mapOf("logType" to "Disease Identificaton", "userId" to uid)
                        db.collection("users").document(uid).collection("disease_logs").document(item.id).set(diseaseLog)
                        db.collection("disease_logs").document(item.id).set(diseaseLog)
                    }
                }
            }
