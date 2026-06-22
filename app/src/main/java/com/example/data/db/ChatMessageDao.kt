package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE chatMode = :chatMode AND (personaId = :personaId OR (personaId IS NULL AND :personaId IS NULL)) ORDER BY timestamp ASC")
    fun getMessages(chatMode: String, personaId: String?): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Delete
    suspend fun deleteMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE chatMode = :chatMode AND (personaId = :personaId OR (personaId IS NULL AND :personaId IS NULL))")
    suspend fun clearMessages(chatMode: String, personaId: String?)

    @Query("UPDATE chat_messages SET isRead = 1 WHERE chatMode = :chatMode AND (personaId = :personaId OR (personaId IS NULL AND :personaId IS NULL)) AND senderId != :currentUserId AND isRead = 0")
    suspend fun markMessagesAsRead(chatMode: String, personaId: String?, currentUserId: String)
}
