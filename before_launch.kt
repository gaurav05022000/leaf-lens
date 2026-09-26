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
            _isLoading.value = true
            if (!PointsManager.deductPoints(2)) {
                _messages.update { it + ChatMessage("Not enough points to send message. Please get more points.", false) }
                _isLoading.value = false
                return@launch
            }

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
            _isLoading.value = true
            if (!PointsManager.deductPoints(2)) {
                _messages.update { it + ChatMessage("Not enough points to send message. Please get more points.", false) }
                _isLoading.value = false
                return@launch
            }

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
        
                }

        
        viewModelScope.launch {
            _isLoading.value = true
            if (!PointsManager.deductPoints(2)) {
                _messages.update { it + ChatMessage("Not enough points to send message. Please get more points.", false) }
                _isLoading.value = false
                return@launch
            }

            var sessionId = _currentSessionId.value
            if (sessionId == null) {
                val title = if (userMessage.isNotBlank()) {
                    userMessage.take(30) + if (userMessage.length > 30) "..." else ""
                } else {
                    "Image Upload"
