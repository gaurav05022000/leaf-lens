package com.example.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Insert
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Query("UPDATE chat_sessions SET timestamp = :timestamp WHERE id = :sessionId")
    suspend fun updateSessionTimestamp(sessionId: Long, timestamp: Long)

    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)
    
    @Query("DELETE FROM chat_sessions")
    suspend fun clearAllSessions()
}
