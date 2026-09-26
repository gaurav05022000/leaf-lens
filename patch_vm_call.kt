                var imageUrl = ""
                try {
                    imageUrl = uploadImageToStorage(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val plant = Plant(
                    name = json.optString("plantName", "Unknown Plant"),
                    species = json.optString("species", "Unknown Species"),
                    disease = json.optString("disease").takeIf { it != "null" && it.isNotBlank() },
                    severityLevel = json.optString("severityLevel").takeIf { it != "null" && it.isNotBlank() },
                    symptoms = parseJsonArray(json, "symptoms").joinToString(", "),
                    healthStatus = json.optString("healthStatus", "Unknown"),
                    healthScore = json.optInt("healthScore", 0),
                    wateringLevel = json.optString("wateringLevel", "Unknown"),
                    wateringScore = json.optInt("wateringScore", 0),
                    sunlight = json.optString("sunlight", "Unknown"),
                    sunlightScore = json.optInt("sunlightScore", 0),
                    description = json.optString("description", ""),
                    treatmentSteps = parseJsonArray(json, "treatmentSteps").joinToString(", "),
                    careTips = parseJsonArray(json, "careTips").joinToString(", "),
                    imageUri = imageUrl
                )
