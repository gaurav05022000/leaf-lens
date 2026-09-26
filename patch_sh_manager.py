import re

with open('app/src/main/java/com/example/ui/ScanHistoryManager.kt', 'r') as f:
    content = f.read()

old_block = """                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("plantName") ?: return@mapNotNull null
                    val species = doc.getString("species") ?: ""
                    val health = doc.getString("healthStatus") ?: "Unknown"
                    val disease = doc.getString("disease")
                    val severity = doc.getString("severityLevel")
                    val ts = doc.getLong("timestamp") ?: 0L
                    val imageUrl = doc.getString("imageUrl")
                    ScanHistoryItem(id, name, species, health, disease, severity, ts, imageUrl)"""
                    
new_block = """                    try {
                        val id = doc.getString("id") ?: doc.id
                        val name = doc.getString("plantName") ?: return@mapNotNull null
                        val species = doc.getString("species") ?: ""
                        val health = doc.getString("healthStatus") ?: "Unknown"
                        val disease = doc.getString("disease")
                        val severity = doc.getString("severityLevel")
                        val ts = doc.getLong("timestamp") ?: 0L
                        val imageUrl = doc.getString("imageUrl")
                        ScanHistoryItem(id, name, species, health, disease, severity, ts, imageUrl)
                    } catch (e: Exception) {
                        null
                    }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open('app/src/main/java/com/example/ui/ScanHistoryManager.kt', 'w') as f:
        f.write(content)
    print("Patched ScanHistoryManager")
else:
    print("Pattern not found in ScanHistoryManager")
