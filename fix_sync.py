import re

with open('app/src/main/java/com/example/ui/PointsManager.kt', 'r') as f:
    points_content = f.read()

points_content = points_content.replace("val uid = getUserId()", "val uid = getUserId() ?: return")
points_content = points_content.replace("val uid = getUserId() ?: return@launch", "val uid = getUserId() ?: return@launch")

with open('app/src/main/java/com/example/ui/PointsManager.kt', 'w') as f:
    f.write(points_content)

with open('app/src/main/java/com/example/ui/ScanHistoryManager.kt', 'r') as f:
    scan_content = f.read()

scan_content = scan_content.replace("val uid = getUserId()", "val uid = getUserId() ?: return")

old_block = """                db.collection("users").document(getUserId()).collection("scan_history")
                    .document(item.id).set(map).await()
                
                // Store identified disease logs
                if (!item.disease.isNullOrBlank() && item.disease != "None") {
                    val diseaseLog = map + mapOf("logType" to "Disease Identificaton", "userId" to getUserId())
                    db.collection("users").document(getUserId()).collection("disease_logs").document(item.id).set(diseaseLog).await()
                }"""

new_block = """                val uid = getUserId() ?: return@launch
                db.collection("users").document(uid).collection("scan_history")
                    .document(item.id).set(map).await()
                
                // Store identified disease logs
                if (!item.disease.isNullOrBlank() && item.disease != "None") {
                    val diseaseLog = map + mapOf("logType" to "Disease Identificaton", "userId" to uid)
                    db.collection("users").document(uid).collection("disease_logs").document(item.id).set(diseaseLog).await()
                }"""

scan_content = scan_content.replace(old_block, new_block)

with open('app/src/main/java/com/example/ui/ScanHistoryManager.kt', 'w') as f:
    f.write(scan_content)

print("Patched sync models")
