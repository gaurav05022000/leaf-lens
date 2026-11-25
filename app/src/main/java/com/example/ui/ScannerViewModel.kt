package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Plant
import com.example.data.PlantRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlantRepository.getInstance(application)

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _scanResult = MutableStateFlow<Plant?>(null)
    val scanResult = _scanResult.asStateFlow()
    private var resultUid: String? = null

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun scanPlant(bitmap: Bitmap) {
        if (_isScanning.value) return

        _isScanning.value = true
        _error.value = null
        _scanResult.value = null
        val scanUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (scanUid == null) {
            _error.value = "Please sign in to scan a plant."
            _isScanning.value = false
            return
        }

        viewModelScope.launch {
            try {
                // Verify scan eligibility before calling AI without decrementing scan quota
                val allowed = ServerUsageManager.canScan()
                if (!allowed) {
                    _error.value = "DAILY_LIMIT_REACHED"
                    _isScanning.value = false
                    return@launch
                }

                // Downscale bitmap to max 1200px to prevent OOM
                val resizedBitmap = resizeBitmap(bitmap, 1200)

                val prompt = """
                    You are LeafLens AI, an expert and friendly plant assistant. Analyze this plant image and provide a comprehensive diagnostic.
                    Specifically evaluate its overall health status, provide actionable steps to improve its health (especially if it has issues), and list all important things to keep in mind for its daily care.
                    Maintain an encouraging and friendly tone.
                    Respond ONLY in JSON format matching this structure:
                    {
                      "isPlant": true (Boolean; false if the image does not show a plant),
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

                // Firebase AI Logic is called from the client. App Check and backend
                // authorization are separate; this client quota check is not a security boundary.
                val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(modelName = "gemini-2.5-flash-lite")

                val inputContent = content {
                    image(resizedBitmap)
                    text(prompt)
                }

                val response = kotlinx.coroutines.withTimeout(60_000L) {
                    generativeModel.generateContent(inputContent)
                }
                val text = response.text ?: "{}"

                val diagnosis = com.example.data.ScanResultParser.parse(text)
                check(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == scanUid) {
                    "Your account changed. Please scan again."
                }

                val localImageUri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { saveImageLocally(resizedBitmap, scanUid) }
                var imageUrl = localImageUri
                try {
                    val uploaded = uploadImageToStorage(resizedBitmap, scanUid)
                    if (uploaded.isNotBlank()) {
                        imageUrl = uploaded
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("ScannerViewModel", "Storage upload fallback to local: ${e.message}")
                }

                val generatedId = (System.currentTimeMillis() % 1_000_000_000L).toInt().let { if (it <= 0) kotlin.random.Random.nextInt(1, 999999999) else it }
                val plant = diagnosis.copy(id = generatedId, imageUri = imageUrl)
                check(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == scanUid) {
                    "Your account changed. Please scan again."
                }

                // Scan successful: deliver result to user
                resultUid = scanUid
                _scanResult.value = plant

                // ONLY NOW: reduce the user's free trial scan count after scan success
                ServerUsageManager.recordSuccessfulScan(scanUid)

                com.example.util.AnalyticsHelper.logPlantScanned(
                    diseaseOrHealth = plant.disease ?: plant.healthStatus,
                    confidence = (plant.healthScore / 100f)
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _error.value = "The scan timed out. Check your connection and try again."
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ScannerViewModel", "Scan error", e)
                _error.value = "Error: ${e.localizedMessage ?: "Unable to complete scan. Please try again."}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearResult() {
        resultUid = null
        _scanResult.value = null
        _error.value = null
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val scale = Math.min(maxDim.toFloat() / width, maxDim.toFloat() / height)
        val newWidth = (width * scale).toInt().coerceAtLeast(1)
        val newHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private suspend fun uploadImageToStorage(bitmap: Bitmap, uid: String): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val data = outputStream.toByteArray()

        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        check(auth.currentUser?.uid == uid) { "Your account changed. Please scan again." }
        val filename = "scan_${System.currentTimeMillis()}.jpg"
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("users/$uid/$filename")

        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType("image/jpeg").build()
        storageRef.putBytes(data, metadata).await()
        return storageRef.downloadUrl.await().toString()
    }

    private fun saveImageLocally(bitmap: Bitmap, uid: String): String {
        return try {
            val safeUid = uid.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val imagesDir = File(getApplication<Application>().filesDir, "scans/$safeUid").apply { mkdirs() }
            val file = File(imagesDir, "scan_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            Log.e("ScannerViewModel", "Failed to save local image", e)
            ""
        }
    }

    fun savePlant(plant: Plant, existingPlantName: String? = null) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || uid != resultUid) {
            _scanResult.value = null
            _error.value = "Your account changed. Please scan again."
            return
        }
        viewModelScope.launch {
            try {
                if (existingPlantName != null) {
                    val existing = repository.getPlantByNameSync(existingPlantName)
                    if (existing != null) {
                        val updated = existing.copy(
                            healthStatus = plant.healthStatus,
                            healthScore = plant.healthScore,
                            disease = plant.disease,
                            severityLevel = plant.severityLevel,
                            symptoms = plant.symptoms,
                            treatmentSteps = plant.treatmentSteps,
                            careTips = plant.careTips,
                            description = plant.description,
                            imageUri = plant.imageUri ?: existing.imageUri
                        )
                        repository.update(updated)
                        ScanHistoryManager.addScanResult(updated)
                        clearResult()
                        return@launch
                    }
                }
                repository.insert(plant)
                ScanHistoryManager.addScanResult(plant)
                clearResult()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Could not save the plant. Please sign in and try again."
            }
        }
    }
}
