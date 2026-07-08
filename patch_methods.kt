    fun savePlant(result: ScanResult, customName: String? = null) {
        viewModelScope.launch {
            val finalName = if (!customName.isNullOrBlank()) customName else result.plantName
            
            // Generate a random ID for dummy data purposes, or use Room auto-generate if set up.
            val newPlant = Plant(
                name = finalName,
                species = result.species,
                healthStatus = result.healthStatus,
                healthScore = result.healthScore,
                wateringLevel = result.wateringLevel,
                wateringScore = result.wateringScore,
                sunlight = result.sunlight,
                sunlightScore = result.sunlightScore,
                disease = result.disease,
                severityLevel = result.severityLevel,
                symptoms = result.symptoms.joinToString(", "),
                treatmentSteps = result.treatmentSteps.joinToString(", "),
                careTips = result.careTips.joinToString(", "),
                description = result.description,
                imageUri = null
            )
            repository.insertPlant(newPlant)
            _scanResult.value = null
        }
    }
    
    fun reportIssue(result: ScanResult) {
        // Mock analytics / reporting
    }

    fun clearResult() {
        _scanResult.value = null
    }
