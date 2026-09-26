package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import com.example.data.room.AppDatabase
import com.example.data.room.ChatMessageEntity
import com.example.data.room.ChatSessionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import android.graphics.BitmapFactory

data class ChatMessage(val content: String, val isUser: Boolean, val imageBitmap: Bitmap? = null, val imageBase64: String? = null)

class AiChatViewModel(application: Application) : AndroidViewModel(application) {
    private val chatDao = AppDatabase.getDatabase(application).chatDao()
    
    val chatSessions = chatDao.getAllSessions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("Hello! I'm LeafLens AI, your expert and friendly plant assistant. 🌿 Ask me any questions about your plants, diseases, or general care.", false))
    )
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun createNewSession() {
        _currentSessionId.value = null
        _messages.value = listOf(ChatMessage("Hello! I'm LeafLens AI, your expert and friendly plant assistant. 🌿 Ask me any questions about your plants, diseases, or general care.", false))
    }

    fun loadSession(sessionId: Long) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            val entities = chatDao.getMessagesForSession(sessionId).first()
            val loadedMessages = entities.map { 
                var bmp: Bitmap? = null
                if (it.imageBase64 != null) {
                    try {
                        val decodedString: ByteArray = Base64.decode(it.imageBase64, Base64.DEFAULT)
                        bmp = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                ChatMessage(it.content, it.isUser, bmp, it.imageBase64)
            }
            _messages.value = listOf(ChatMessage("Hello! I'm LeafLens AI, your expert and friendly plant assistant. 🌿 Ask me any questions about your plants, diseases, or general care.", false)) + loadedMessages
        }
    }

    fun deleteSession(session: ChatSessionEntity) {
        viewModelScope.launch {
            chatDao.deleteSession(session)
            if (_currentSessionId.value == session.id) {
                createNewSession()
            }
        }
    }

    fun sendMessage(userMessage: String, bitmap: Bitmap? = null) {
        if (userMessage.isBlank() && bitmap == null) return
        
        val base64Img = bitmap?.let { bitmapToBase64(it) }
        val msg = ChatMessage(userMessage, true, bitmap, base64Img)
        _messages.update { it + msg }
        
        viewModelScope.launch {
            _isLoading.value = true
            if (PointsManager.availablePoints.value < 2) {
                _messages.update { it + ChatMessage("Not enough points to send message. Please get more points.", false) }
                _isLoading.value = false
                return@launch
            }
            PointsManager.deductPoints(2)

            var sessionId = _currentSessionId.value
            if (sessionId == null) {
                val title = if (userMessage.isNotBlank()) {
                    userMessage.take(30) + if (userMessage.length > 30) "..." else ""
                } else {
                    "Image Upload"
                }
                sessionId = chatDao.insertSession(ChatSessionEntity(title = title, timestamp = System.currentTimeMillis()))
                _currentSessionId.value = sessionId
            } else {
                chatDao.updateSessionTimestamp(sessionId, System.currentTimeMillis())
            }
            
            chatDao.insertMessage(ChatMessageEntity(
                sessionId = sessionId,
                content = userMessage,
                isUser = true,
                timestamp = System.currentTimeMillis(),
                imageBase64 = base64Img
            ))

            try {
                if (BuildConfig.OPENROUTER_API_KEY.isBlank()) {
                    _messages.update { it + ChatMessage("Error: API Key is missing. Please add it in Settings.", false) }
                    return@launch
                }

                val prompt = """
                    You are LeafLens AI, an expert and friendly plant assistant.
                    CRITICAL RULE: Your primary function is to provide information related to plants, trees, gardening, botany, and recommendations for plants (e.g., indoor plants, outdoor plants).
                    If the user asks about a topic completely unrelated to plants or gardening (e.g., programming, politics, cars), you must politely refuse and state that you only provide information related to plants and trees.
                    
                    To show an image of a plant, you MUST use the markdown image syntax like this:
                    ![Image of plant](https://image.pollinations.ai/prompt/high_quality_photo_of_plant_name?width=400&height=300&nologo=true)
                    Replace "plant_name" with the actual name of the plant (use underscores instead of spaces). Always include at least one image when describing a specific plant or recommending plants.
                    
                    For plant identification or recommendation requests, follow this structure:
                    1. Start with the plant emoji and plant name.
                    2. Provide an image of the plant using the markdown syntax.
                    3. Provide a short description (2-3 lines).
                    4. Display information in sections using emojis:
                       📖 About
                       ☀️ Light
                       💧 Water
                       🌱 Soil
                       🌡️ Temperature
                       🐾 Pet Safety
                       ⚠️ Common Problems
                       ✨ Fun Fact
                       💡 AI Care Tip
                    5. Keep responses concise, visually appealing, and easy to scan.
                    6. Use encouraging and friendly language. YOU MUST USE EMOJIS abundantly in your text.
                    7. End with a question like: "Would you like care tips, propagation methods, or help diagnosing problems? 🌿"
                    
                    For general conversational questions about plants, be friendly, concise, and use emojis naturally.
                """.trimIndent()
                
                val apiMessages = mutableListOf<OpenRouterMessage>()
                
                apiMessages.add(
                    OpenRouterMessage(
                        role = "system",
                        content = listOf(OpenRouterContentPart(type = "text", text = prompt))
                    )
                )

                val history = _messages.value.drop(1)
                for (m in history) {
                    val role = if (m.isUser) "user" else "assistant"
                    val contentParts = mutableListOf<OpenRouterContentPart>()
                    if (m.content.isNotBlank()) {
                        contentParts.add(OpenRouterContentPart(type = "text", text = m.content))
                    }
                    if (m.imageBase64 != null) {
                        contentParts.add(OpenRouterContentPart(
                            type = "image_url",
                            image_url = OpenRouterImageUrl(url = "data:image/jpeg;base64,${m.imageBase64}")
                        ))
                    }
                    if (contentParts.isNotEmpty()) {
                        apiMessages.add(OpenRouterMessage(role = role, content = contentParts))
                    }
                }
                
                val request = OpenRouterRequest(
                    model = "google/gemini-2.5-flash-lite",
                    messages = apiMessages
                )

                val modelsToTry = listOf("google/gemini-2.5-flash", "google/gemini-flash-1.5", "google/gemini-pro-1.5", "google/gemini-2.5-pro")
                var replyText: String? = null
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

                        replyText = response.choices?.firstOrNull()?.message?.content
                        if (replyText != null) break
                    } catch (e: Exception) {
                        lastError = e
                        if (e is retrofit2.HttpException && (e.code() == 401 || e.code() == 403 || e.code() == 429)) {
                            break
                        }
                    }
                }

                if (replyText == null && lastError != null) {
                    throw lastError
                }

                val finalReply = replyText ?: "I am having trouble connecting to my knowledge base right now."
                val finalReplyTrimmed = finalReply.trim()

                _messages.update { it + ChatMessage(finalReplyTrimmed, false) }
                
                chatDao.insertMessage(ChatMessageEntity(
                    sessionId = sessionId,
                    content = finalReplyTrimmed,
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                ))

            } catch (e: retrofit2.HttpException) {
                if (e.code() == 400 || e.code() == 401 || e.code() == 403) {
                     _messages.update { it + ChatMessage("Error: Invalid or missing API Key.", false) }
                } else if (e.code() == 404) {
                     _messages.update { it + ChatMessage("Error 404: The selected AI model is not available. Please try again.", false) }
                } else if (e.code() == 429) {
                     _messages.update { it + ChatMessage("Rate Limit Reached (Error 429). The free tier API key has reached its quota. Please try again later or add a new key in Secrets.", false) }
                } else if (e.code() == 503) {
                     _messages.update { it + ChatMessage("Error 503: The AI servers are currently overloaded. Please try connecting again later.", false) }
                } else {
                     _messages.update { it + ChatMessage("Error ${e.code()}: ${e.message()}", false) }
                }
            } catch (e: Exception) {
                _messages.update { it + ChatMessage("Error: ${e.localizedMessage}", false) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
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
}
