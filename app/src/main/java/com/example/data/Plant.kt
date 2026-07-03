package com.example.data

data class Plant(
    val id: Int = 0,
    val name: String = "",
    val species: String = "",
    val healthStatus: String = "Healthy", // e.g., "Healthy", "Alert", "Powdery Mildew"
    val healthScore: Int = 100, // 0-100
    val wateringLevel: String = "Medium", // e.g., "High", "Medium", "Low"
    val wateringScore: Int = 100, // 0-100
    val sunlight: String = "Indirect", // e.g., "Bright Direct", "Indirect"
    val sunlightScore: Int = 100, // 0-100
    val nextWateringTimeMs: Long = 0L,
    val nextFeedingTimeMs: Long = 0L,
    val disease: String? = null,
    val severityLevel: String? = null,
    val symptoms: String = "", // Comma-separated or serialized
    val treatmentSteps: String = "", // Comma-separated or serialized
    val careTips: String = "", // Comma-separated or serialized
    val imageUri: String? = null,
    val description: String = "",
    val similarImageUris: String = "", // Comma separated list of images
    val isPremium: Boolean = false, // Demo purpose from UI map
    val firestoreId: String? = null
)
