package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.OpenRouterRequest
import com.example.api.OpenRouterMessage
import com.example.api.OpenRouterContentPart
import com.example.api.OpenRouterImageUrl
import com.example.api.RetrofitClient
import com.example.data.Plant
import com.example.data.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlantRepository
    
    init {
        repository = PlantRepository.getInstance(application)
    }

    private val _scanResult = MutableStateFlow<Plant?>(null)
    val scanResult = _scanResult.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun scanPlant(bitmap: Bitmap) {
        if (_isScanning.value) return
        _isScanning.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val base64Image = bitmapToBase64(bitmap)
                val prompt = """
                    You are an expert botanist and plant pathologist AI. 
                    Analyze the attached image and identify the plant species. Then, evaluate its health.
                    Return a JSON object EXACTLY matching this structure:
                    {
                      "plantName": "Common Name of the Plant",
                      "species": "Scientific name",
                      "disease": "Name of disease if present, or null if healthy",
                      "severityLevel": "Low, Medium, High, or None",
                      "symptoms": ["Symptom 1", "Symptom 2"], // empty list if healthy
                      "healthStatus": "e.g. Healthy, Alert, Action Required",
                      "healthScore": 92, // integer 0-100 indicating overall health
                      "wateringLevel": "High, Medium, or Low",
                      "wateringScore": 80, // integer 0-100
                      "sunlight": "e.g. Bright Direct, Indirect Light",
                      "sunlightScore": 95, // integer 0-100
                      "description": "Short description of the plant and its care needs",
                      "treatmentSteps": ["Step 1", "Step 2"], // empty list if healthy
                      "careTips": ["Tip 1", "Tip 2"] // Actionable care tips based on the identified plant disease
                    }
                """.trimIndent()

                val request = OpenRouterRequest(
                    model = "google/gemini-flash-1.5",
                    messages = listOf(
                        OpenRouterMessage(
                            role = "user",
                            content = listOf(
                                OpenRouterContentPart(type = "text", text = prompt),
                                OpenRouterContentPart(type = "image_url", image_url = OpenRouterImageUrl(url = "data:image/jpeg;base64,$base64Image"))
                            )
                        )
                    ),
                    response_format = com.example.api.OpenRouterResponseFormat("json_object")
                )

                if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                    _error.value = "API Key is missing. Please add it in Settings -> Secrets."
                    _isScanning.value = false
                    return@launch
                }

                val modelsToTry = listOf("google/gemini-flash-1.5", "google/gemini-pro-1.5")
                var responseText: String? = null
                var lastError: Exception? = null

                for (model in modelsToTry) {
                    try {
                        val requestWithModel = request.copy(model = model)
                        val response = RetrofitClient.service.generateContent(
                            authorization = "Bearer ${BuildConfig.GEMINI_API_KEY}",
                            referer = "https://ai.studio",
                            title = "LeafLens",
                            request = requestWithModel
                        )
                        responseText = response.choices?.firstOrNull()?.message?.content
                        if (responseText != null) break
                    } catch (e: Exception) {
                        lastError = e
                        if (e is retrofit2.HttpException && (e.code() == 400 || e.code() == 401 || e.code() == 403 || e.code() == 429)) {
                            break
                        }
                    }
                }

                if (responseText == null && lastError != null) {
                    throw lastError
                }

                val text = responseText ?: "{}"
                val jsonStartIndex = text.indexOf('{')
                val jsonEndIndex = text.lastIndexOf('}')
                if (jsonStartIndex == -1 || jsonEndIndex == -1) {
                     _error.value = "Could not parse API response. Please try again."
                     return@launch
                }
                
                val cleanJson = text.substring(jsonStartIndex, jsonEndIndex + 1)
                val json = JSONObject(cleanJson)

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
                    imageUri = "" 
                )

                repository.insert(plant)
                _scanResult.value = plant
                
                PointsManager.deductPoints(10)
                PointsManager.incrementScans()

            } catch (e: retrofit2.HttpException) {
                if (e.code() == 400 || e.code() == 401 || e.code() == 403) {
                     _error.value = "API Key error. Ensure your API Key in Secrets is valid."
                } else if (e.code() == 429) {
                     _error.value = "Rate limit reached. Please try again later."
                } else if (e.code() == 503) {
                     _error.value = "AI servers are overloaded. Please try again."
                } else {
                     _error.value = "Error ${e.code()}: ${e.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }
    
    fun clearResult() {
        _scanResult.value = null
        _error.value = null
    }

    private fun parseJsonArray(json: JSONObject, key: String): List<String> {
        val list = mutableListOf<String>()
        val arr = json.optJSONArray(key)
        if (arr != null) {
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        }
        return list
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        val maxDim = 800
        val scale = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
        val scaledBitmap = if (scale < 1) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun savePlant(plant: Plant, existingPlantName: String? = null) {
        // Already saved during scan.
    }
    
    fun reportIssue(plant: Plant) {
        // Dummy implementation
    }
}
