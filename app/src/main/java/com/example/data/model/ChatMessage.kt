package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderName: String,
    val senderId: String, // "user1", "user2", "ai" Or custom UUID
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val chatMode: String, // "LOCAL", "FACE_TO_FACE", "AI", "CLOUD"
    val personaId: String? = null, // ID of the AI persona, if applicable
    val isPending: Boolean = false, // If the message is currently sending/generating
    val isRead: Boolean = false, // Read status for sync and visual checkmarks
    val cloudId: String? = null // Unique identifier for syncing from cloud DB
)
