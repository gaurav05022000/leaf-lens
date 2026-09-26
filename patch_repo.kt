import java.io.File

fun main() {
    val f = File("app/src/main/java/com/example/ui/ScanHistoryManager.kt")
    val content = f.readText()
    
    val old = """
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("plantName") ?: return@mapNotNull null
                    val species = doc.getString("species") ?: ""
                    val health = doc.getString("healthStatus") ?: "Unknown"
                    val disease = doc.getString("disease")
                    val severity = doc.getString("severityLevel")
                    val ts = doc.getLong("timestamp") ?: 0L
                    val imageUrl = doc.getString("imageUrl")
                    ScanHistoryItem(id, name, species, health, disease, severity, ts, imageUrl)
"""
    val new = """
                    try {
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
                    }
"""
    f.writeText(content.replace(old.trimIndent(), new.trimIndent()))
}
