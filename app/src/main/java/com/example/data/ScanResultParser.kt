package com.example.data

import org.json.JSONException
import org.json.JSONObject

object ScanResultParser {

    fun parse(rawText: String): Plant {
        val jsonStartIndex = rawText.indexOf('{')
        val jsonEndIndex = rawText.lastIndexOf('}')
        if (jsonStartIndex == -1 || jsonEndIndex <= jsonStartIndex) {
            throw IllegalArgumentException("Could not parse AI diagnosis. Please try again.")
        }

        val cleanJson = rawText.substring(jsonStartIndex, jsonEndIndex + 1)
        val json = try {
            JSONObject(cleanJson)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Could not parse AI diagnosis. Please try again.", e)
        }

        if (!json.optBoolean("isPlant", true)) {
            throw IllegalArgumentException("No plant detected in this photo. Please capture a clear photo of a plant or leaf.")
        }

        val plantName = optNullableString(json, "plantName")
            ?: throw IllegalArgumentException("Could not identify the plant. Please try again with a clearer photo.")

        val species = optNullableString(json, "species") ?: "Unknown Species"
        val disease = optNullableString(json, "disease")
        val severityLevel = optNullableString(json, "severityLevel")
        val symptoms = parseJsonArray(json, "symptoms").joinToString("\n")
        val healthStatus = optNullableString(json, "healthStatus") ?: "Unknown"
        val healthScore = optBoundedInt(json, "healthScore", 85, 0, 100)
        val wateringLevel = optNullableString(json, "wateringLevel") ?: "Medium"
        val wateringScore = optBoundedInt(json, "wateringScore", 85, 0, 100)
        val wateringIntervalDays = optBoundedInt(json, "wateringIntervalDays", 7, 1, 365)
        val fertilizingIntervalDays = optBoundedInt(json, "fertilizingIntervalDays", 30, 1, 365)
        val sunlight = optNullableString(json, "sunlight") ?: "Indirect"
        val sunlightScore = optBoundedInt(json, "sunlightScore", 85, 0, 100)
        val description = optNullableString(json, "description") ?: ""
        val treatmentSteps = parseJsonArray(json, "treatmentSteps").joinToString("\n")
        val careTips = parseJsonArray(json, "careTips").joinToString("\n")

        return Plant(
            name = plantName,
            species = species,
            disease = disease,
            severityLevel = severityLevel,
            symptoms = symptoms,
            healthStatus = healthStatus,
            healthScore = healthScore,
            wateringLevel = wateringLevel,
            wateringScore = wateringScore,
            wateringIntervalDays = wateringIntervalDays,
            fertilizingIntervalDays = fertilizingIntervalDays,
            sunlight = sunlight,
            sunlightScore = sunlightScore,
            description = description,
            treatmentSteps = treatmentSteps,
            careTips = careTips
        )
    }

    private fun optNullableString(json: JSONObject, key: String): String? {
        if (!json.has(key) || json.isNull(key)) return null
        val value = json.optString(key, "").trim()
        return value.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    }

    private fun optBoundedInt(
        json: JSONObject,
        key: String,
        defaultValue: Int,
        minValue: Int,
        maxValue: Int
    ): Int {
        if (!json.has(key) || json.isNull(key)) return defaultValue
        val raw = when (val value = json.opt(key)) {
            is Number -> value.toLong()
            is String -> value.trim().toDoubleOrNull()?.toLong() ?: return defaultValue
            else -> return defaultValue
        }
        return raw.coerceIn(minValue.toLong(), maxValue.toLong()).toInt()
    }

    private fun parseJsonArray(json: JSONObject, key: String): List<String> {
        val list = mutableListOf<String>()
        val arr = json.optJSONArray(key) ?: return list
        for (i in 0 until arr.length()) {
            val item = arr.optString(i, "").trim()
            if (item.isNotEmpty() && !item.equals("null", ignoreCase = true)) {
                list.add(item)
            }
        }
        return list
    }
}
