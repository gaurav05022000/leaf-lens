import re

with open('app/src/main/java/com/example/ui/ScanHistoryManager.kt', 'r') as f:
    content = f.read()

old_block = """                val missingInCloud = localItems.filter { !cloudIds.contains(it.id) }
                
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
                    
                    if (!item.disease.isNullOrBlank() && item.disease != "None") {
                        val diseaseLog = map + mapOf("logType" to "Disease Identificaton", "userId" to uid)
                        db.collection("users").document(uid).collection("disease_logs").document(item.id).set(diseaseLog)
                    }
                }"""
                
new_block = """                val missingInCloud = localItems.filter { !cloudIds.contains(it.id) }
                
                missingInCloud.forEach { item ->
                    try {
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
                        
                        if (!item.disease.isNullOrBlank() && item.disease != "None") {
                            val diseaseLog = map + mapOf("logType" to "Disease Identificaton", "userId" to uid)
                            db.collection("users").document(uid).collection("disease_logs").document(item.id).set(diseaseLog)
                        }
                    } catch(e: Exception) {
                        Log.e("ScanHistoryManager", "Error saving scan history to firestore", e)
                    }
                }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open('app/src/main/java/com/example/ui/ScanHistoryManager.kt', 'w') as f:
        f.write(content)
    print("Patched ScanHistoryManager missingInCloud")
else:
    print("Pattern not found in ScanHistoryManager missingInCloud")
