package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.OpenRouterContentPart
import com.example.api.OpenRouterImageUrl
import com.example.api.OpenRouterMessage
import com.example.api.OpenRouterRequest
import com.example.api.RetrofitClient
import com.example.data.Plant
import com.example.data.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import android.util.Base64
import kotlinx.coroutines.tasks.await

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlantRepository.getInstance(application)

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _scanResult = MutableStateFlow<Plant?>(null)
    val scanResult = _scanResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun scanPlant(bitmap: Bitmap) {
        if (_isScanning.value) return
        
        if (PointsManager.availablePoints.value < 10) {
            _error.value = "Not enough points to scan. You need 10 points."
            return
        }
        _isScanning.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val base64Image = bitmapToBase64(bitmap)
                
                val prompt = """
                    You are LeafLens AI, an expert and friendly plant assistant. Analyze this plant image and provide a comprehensive diagnostic.
                    Specifically evaluate its overall health status, provide actionable steps to improve its health (especially if it has issues), and list all important things to keep in mind for its daily care.
                    Maintain an encouraging and friendly tone.
                    Respond ONLY in JSON format matching this structure:
                    {
                      "plantName": "String (Common name)",
                      "species": "String (Scientific name)",
                      "disease": "String or null (Identify any diseases, pests, or deficiencies)",
                      "severityLevel": "String or null (e.g., Mild, Moderate, Severe)",
                      "symptoms": ["String", "String"] (List any observable symptoms),
                      "healthStatus": "String (e.g., Excellent, Good, Fair, Poor, Critical)",
                      "healthScore": 100 (Integer, 0-100),
                      "wateringLevel": "String (e.g., High, Medium, Low)",
                      "wateringScore": 100 (Integer, 0-100),
                      "wateringIntervalDays": 7 (Integer, how many days between watering),
                      "fertilizingIntervalDays": 30 (Integer, how many days between fertilizing),
                      "sunlight": "String (e.g., Full Sun, Partial Shade, Indirect)",
                      "sunlightScore": 100 (Integer, 0-100),
                      "description": "String (Detailed summary of the plant's current condition and what it needs. Use emojis to make it engaging!)",
                      "treatmentSteps": ["Step 1", "Step 2"] (Actionable steps to treat current issues or improve health),
                      "careTips": ["Tip 1", "Tip 2"] (Comprehensive ongoing care instructions including light, water, soil, and environment needs)
                    }
                """.trimIndent()

                val request = OpenRouterRequest(
                    model = "google/gemini-2.5-flash-lite",
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

                if (BuildConfig.OPENROUTER_API_KEY.isBlank()) {
                    _error.value = "API Key is missing. Please add it in Settings -> Secrets."
                    _isScanning.value = false
                    return@launch
                }

                val modelsToTry = listOf("google/gemini-2.5-flash-lite", "google/gemini-2.5-flash")
                var responseText: String? = null
                var lastError: Exception? = null

                for (model in modelsToTry) {
                    try {
                        val requestWithModel = request.copy(model = model)
                        val response = RetrofitClient.service.generateContent(
                            authorization = "Bearer ${BuildConfig.OPENROUTER_API_KEY}",
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
                    wateringIntervalDays = json.optInt("wateringIntervalDays", 7),
                    fertilizingIntervalDays = json.optInt("fertilizingIntervalDays", 30),
                    sunlight = json.optString("sunlight", "Unknown"),
                    sunlightScore = json.optInt("sunlightScore", 0),
                    description = json.optString("description", ""),
                    treatmentSteps = parseJsonArray(json, "treatmentSteps").joinToString(", "),
                    careTips = parseJsonArray(json, "careTips").joinToString(", "),
                    imageUri = imageUrl 
                )

                repository.insert(plant)
                ScanHistoryManager.addScanResult(plant)
                _scanResult.value = plant
                
                PointsManager.deductPoints(10)
                PointsManager.incrementScans()
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 400 || e.code() == 401 || e.code() == 403) {
                     _error.value = "API Key error. Ensure your API Key in Secrets is valid."
                } else if (e.code() == 404) {
                     _error.value = "Model not found (404). OpenRouter might have changed the model names."
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

    private suspend fun uploadImageToStorage(bitmap: Bitmap): String {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val data = outputStream.toByteArray()
            
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid ?: "anonymous"
            val filename = "scans_${System.currentTimeMillis()}.jpg"
            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("users/$uid/$filename")
            
            storageRef.putBytes(data).await()
            val uri = storageRef.downloadUrl.await()
            uri.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to Base64 data URI for Firestore sync
            try {
                val outputStream = ByteArrayOutputStream()
                val scale = Math.min(400f / bitmap.width, 400f / bitmap.height)
                val scaledBitmap = if (scale < 1) {
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                } else {
                    bitmap
                }
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
                val base64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
                "data:image/jpeg;base64,$base64"
            } catch (ex: Exception) {
                ex.printStackTrace()
                ""
            }
        }
    }

    fun savePlant(plant: Plant, existingPlantName: String? = null) {
        // Already saved during scan.
    }
    
    fun reportIssue(plant: Plant) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                db.collection("reports").add(mapOf(
                    "userId" to uid,
                    "plantName" to plant.name,
                    "timestamp" to System.currentTimeMillis()
                ))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
