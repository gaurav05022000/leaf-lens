data class ScanHistoryItem(
    val id: String,
    val plantName: String,
    val species: String,
    val healthStatus: String,
    val disease: String?,
    val severityLevel: String?,
    val timestamp: Long,
    val imageUrl: String? = null
)
