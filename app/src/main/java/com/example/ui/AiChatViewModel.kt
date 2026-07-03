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

data class ChatMessage(val content: String, val isUser: Boolean, val imageBitmap: Bitmap? = null)

class AiChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("Hello! I'm your AI Botanist. Ask me any questions about your plants, diseases, or general care.", false))
    )
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun sendMessage(userMessage: String, bitmap: Bitmap? = null) {
        if (userMessage.isBlank() && bitmap == null) return

        _messages.update { it + ChatMessage(userMessage, true, bitmap) }
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val historyText = _messages.value.takeLast(6).joinToString("\n") { 
                    (if (it.isUser) "User" else "Botanist") + ": " + it.content 
                }
                
                if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                    _messages.update { it + ChatMessage("Error: Gemini API Key is missing. Please add it in Settings.", false) }
                    _isLoading.value = false
                    return@launch
                }

                val prompt = "You are an expert AI Botanist. Help the user with plant diagnostics, care tips, or recommendations.\n\nConversation so far:\n$historyText\nBotanist: "
                
                val parts = mutableListOf<Part>()
                parts.add(Part(text = prompt))
                if (bitmap != null) {
                    parts.add(Part(inlineData = InlineData("image/jpeg", bitmapToBase64(bitmap))))
                }
                
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            role = "user",
                            parts = parts
                        )
                    )
                )

                val modelsToTry = listOf("gemini-3.5-flash", "gemini-1.5-flash-latest", "gemini-1.5-flash")
                var replyText: String? = null
                var lastError: Exception? = null

                for (model in modelsToTry) {
                    try {
                        val response = RetrofitClient.service.generateContent(model, BuildConfig.GEMINI_API_KEY, request)
                        replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (replyText != null) break
                    } catch (e: Exception) {
                        lastError = e
                        if (e is retrofit2.HttpException && (e.code() == 400 || e.code() == 401 || e.code() == 403 || e.code() == 429)) {
                            break
                        }
                    }
                }

                if (replyText == null && lastError != null) {
                    throw lastError
                }

                val finalReply = replyText ?: "I am having trouble connecting to my knowledge base right now."
                
                if (replyText != null) {
                    PointsManager.deductPoints(2)
                }

                _messages.update { it + ChatMessage(finalReply.trim(), false) }
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 400 || e.code() == 401 || e.code() == 403) {
                     _messages.update { it + ChatMessage("Error: Invalid or missing API Key.", false) }
                } else if (e.code() == 429) {
                     _messages.update { it + ChatMessage("Rate Limit Reached (Error 429). The free tier API key has reached its quota. Please try again later or add a new key in Secrets.", false) }
                } else if (e.code() == 503) {
                     _messages.update { it + ChatMessage("Offline Fallback: The AI servers are currently overloaded (503). Here is a general botany tip: Keep a consistent watering schedule and ensure adequate drainage to prevent root rot. Please try connecting again later.", false) }
                } else {
                     _messages.update { it + ChatMessage("Error ${e.code()}: ${e.message}", false) }
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

