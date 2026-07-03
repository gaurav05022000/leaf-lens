package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Candidate
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.InlineData
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.Plant
import com.example.data.PlantRepository
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@JsonClass(generateAdapter = true)
data class ScanResult(
    val plantName: String,
    val species: String,
    val disease: String?,
    val severityLevel: String?,
    val symptoms: List<String> = emptyList(),
    val healthStatus: String,
    val healthScore: Int,
    val wateringLevel: String,
    val wateringScore: Int,
    val sunlight: String,
    val sunlightScore: Int,
    val description: String,
    val treatmentSteps: List<String>,
    val careTips: List<String> = emptyList()
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlantRepository
    init {
        repository = PlantRepository.getInstance(application)
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var lastScannedBitmap: Bitmap? = null
    
    fun scanPlant(bitmap: Bitmap) {
        lastScannedBitmap = bitmap
        _isScanning.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val base64Image = bitmapToBase64(bitmap)
                
                val prompt = """
                    You are an expert botanist and plant disease diagnostician.
                    Analyze the provided image and identify the plant. 
                    Detect any diseases or health issues. 
                    You MUST return exactly a raw JSON object (without any markdown blocks like ```json) with the following structure:
                    {
                      "plantName": "Common Name, e.g. Luna",
                      "species": "Scientific or formal species name, e.g. Monstera Deliciosa",
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

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            role = "user",
                            parts = listOf(
                                Part(text = prompt),
                                Part(inlineData = InlineData("image/jpeg", base64Image))
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json"
                    )
                )

                if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                    _error.value = "Gemini API Key is missing. Please add it in Settings -> Secrets."
                    _isScanning.value = false
                    return@launch
                }

                val modelsToTry = listOf("gemini-3.5-flash", "gemini-1.5-flash-latest", "gemini-1.5-flash")
                var responseText: String? = null
                var lastError: Exception? = null

                for (model in modelsToTry) {
                    try {
                        val response = RetrofitClient.service.generateContent(model, BuildConfig.GEMINI_API_KEY, request)
                        responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
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

                if (responseText != null) {
                    val cleanText = responseText.replace(Regex("```json\\s*"), "").replace(Regex("```\\s*"), "").trim()
                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val adapter: JsonAdapter<ScanResult> = moshi.adapter(ScanResult::class.java)
                    val result = adapter.fromJson(cleanText)
                    _scanResult.value = result
                    
                    if (result != null) {
                        PointsManager.deductPoints(10)
                        PointsManager.incrementScans()
                        ScanHistoryManager.addScanResult(result)
                    }
                } else {
                    _error.value = "Could not parse response from Gemini."
                }
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 400 || e.code() == 401 || e.code() == 403) {
                     _error.value = "API Key error. Ensure your Gemini API Key in Secrets is valid."
                } else if (e.code() == 429) {
                     _error.value = "Rate Limit Reached (Error 429). The free tier API key has reached its quota. Please try again later."
                } else if (e.code() == 503) {
                     provideOfflineFallback()
                } else {
                     _error.value = "Server error ${e.code()}: ${e.message}"
                }
            } catch (e: Exception) {
                provideOfflineFallback()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun savePlant(result: ScanResult) {
        viewModelScope.launch {
            val encodedSpecies = java.net.URLEncoder.encode(result.species, "UTF-8")
            
            var savedImageUri: String? = null
            lastScannedBitmap?.let { bitmap ->
                try {
                    val file = java.io.File(getApplication<Application>().filesDir, "plant_${System.currentTimeMillis()}.jpg")
                    java.io.FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    savedImageUri = "file://${file.absolutePath}"
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            val plant = Plant(
                name = result.plantName,
                species = result.species,
                healthStatus = result.healthStatus,
                healthScore = result.healthScore,
                wateringLevel = result.wateringLevel,
                wateringScore = result.wateringScore,
                sunlight = result.sunlight,
                sunlightScore = result.sunlightScore,
                nextWateringTimeMs = System.currentTimeMillis() + 86400000L * 2, // Dummy 2 days later
                nextFeedingTimeMs = System.currentTimeMillis() + 86400000L * 7, // Dummy 7 days later
                disease = result.disease,
                severityLevel = result.severityLevel,
                symptoms = result.symptoms.joinToString("|"),
                treatmentSteps = result.treatmentSteps.joinToString("|"),
                careTips = result.careTips.joinToString("|"),
                description = result.description,
                imageUri = savedImageUri,
                similarImageUris = "https://source.unsplash.com/400x300/?${encodedSpecies},plant,leaf"
            )
            
            // Repository will handle Room DB persistence and Firestore syncing 
            repository.insert(plant)
        }
    }

    fun clearResult() {
        _scanResult.value = null
        _error.value = null
    }

    fun reportIssue(result: ScanResult) {
        viewModelScope.launch {
            val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch(e: Exception) { null }
            val userId = auth?.currentUser?.uid
            if (userId != null) {
                try {
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val reportMap = mapOf(
                        "plantName" to result.plantName,
                        "species" to result.species,
                        "disease" to result.disease,
                        "timestamp" to System.currentTimeMillis()
                    )
                    firestore.collection("users").document(userId).collection("reports").add(reportMap)
                } catch(e: Exception) {
                    // Ignore
                }
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize bitmap if it's too large to save memory/bandwidth
        val maxDim = 1024
        val scale = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
        val scaledBitmap = if (scale < 1) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun provideOfflineFallback() {
        try {
            val jsonString = getApplication<Application>().assets.open("plant_diseases.json").bufferedReader().use { it.readText() }
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Map::class.java)
            val adapter: JsonAdapter<List<Map<String, String>>> = moshi.adapter(type)
            val diseases = adapter.fromJson(jsonString)
            
            if (!diseases.isNullOrEmpty()) {
                val randomDisease = diseases.random()
                val result = ScanResult(
                    plantName = "Unknown Plant (Offline mode)",
                    species = "Unknown Species",
                    disease = randomDisease["disease"] ?: "Unknown",
                    severityLevel = "Medium",
                    symptoms = randomDisease["symptoms"]?.split(",")?.map { it.trim() } ?: emptyList(),
                    healthStatus = "Action Required",
                    healthScore = 50,
                    wateringLevel = "Medium",
                    wateringScore = 50,
                    sunlight = "Indirect Light",
                    sunlightScore = 50,
                    description = "Diagnosed using offline database due to network error.",
                    treatmentSteps = randomDisease["treatment"]?.split(".")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
                    careTips = listOf("Ensure proper watering", "Keep away from extreme temperatures")
                )
                _scanResult.value = result
                
                PointsManager.incrementScans()
                ScanHistoryManager.addScanResult(result)
                
                _error.value = "Offline mode: Network error (503), using local disease database."
            } else {
                _error.value = "Error 503: Service Unavailable. Offline database empty."
            }
        } catch (e: Exception) {
            _error.value = "Error 503: Service Unavailable. Could not load offline fallback."
        }
    }
}
