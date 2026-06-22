package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.RetrofitClient
import com.example.data.api.CloudChatRetrofitClient
import com.example.data.api.CloudMessageDto
import com.example.data.api.MemberStatusDto
import com.example.data.db.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.repository.AiPersona
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ChatRepository(
        chatMessageDao = db.chatMessageDao(),
        apiService = RetrofitClient.service
    )

    private val prefs = application.getSharedPreferences("localStorage_chat_prefs", Context.MODE_PRIVATE)

    val personas = repository.personas

    // Active Modes: "LOCAL" (classic duet chat), "FACE_TO_FACE" (splitscreen facing), "AI" (AI twin), "CLOUD" (online sync)
    private val _activeMode = MutableStateFlow("LOCAL")
    val activeMode: StateFlow<String> = _activeMode.asStateFlow()

    private val _activePersonaId = MutableStateFlow("alex")
    val activePersonaId: StateFlow<String> = _activePersonaId.asStateFlow()

    // Configurable Local Names
    private val _user1Name = MutableStateFlow("Alice")
    val user1Name: StateFlow<String> = _user1Name.asStateFlow()

    private val _user2Name = MutableStateFlow("Bob")
    val user2Name: StateFlow<String> = _user2Name.asStateFlow()

    // Status choices: "ONLINE" (在线), "AWAY" (离开), "BUSY" (忙碌)
    private val _user1Status = MutableStateFlow("ONLINE")
    val user1Status: StateFlow<String> = _user1Status.asStateFlow()

    private val _user2Status = MutableStateFlow("ONLINE")
    val user2Status: StateFlow<String> = _user2Status.asStateFlow()

    // Cloud Chat properties
    private val _cloudUserId = MutableStateFlow("")
    val cloudUserId: StateFlow<String> = _cloudUserId.asStateFlow()

    private val _cloudUserName = MutableStateFlow("")
    val cloudUserName: StateFlow<String> = _cloudUserName.asStateFlow()

    private val _cloudUserStatus = MutableStateFlow("ONLINE")
    val cloudUserStatus: StateFlow<String> = _cloudUserStatus.asStateFlow()

    private val _activeRoomCode = MutableStateFlow("")
    val activeRoomCode: StateFlow<String> = _activeRoomCode.asStateFlow()

    private val _cloudRoomMembers = MutableStateFlow<Map<String, MemberStatusDto>>(emptyMap())
    val cloudRoomMembers: StateFlow<Map<String, MemberStatusDto>> = _cloudRoomMembers.asStateFlow()

    private var pollingJob: Job? = null

    // Switch active typing sender in Classic LOCAL mode: "user1" (Alice) or "user2" (Bob)
    private val _currentUserLocal = MutableStateFlow("user1")
    val currentUserLocal: StateFlow<String> = _currentUserLocal.asStateFlow()

    // Loading/generating state for AI replies
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    init {
        _activeMode.value = prefs.getString("active_mode", "LOCAL") ?: "LOCAL"
        _activePersonaId.value = prefs.getString("active_persona_id", "alex") ?: "alex"
        _user1Name.value = prefs.getString("user1_name", "Alice") ?: "Alice"
        _user2Name.value = prefs.getString("user2_name", "Bob") ?: "Bob"
        _user1Status.value = prefs.getString("user1_status", "ONLINE") ?: "ONLINE"
        _user2Status.value = prefs.getString("user2_status", "ONLINE") ?: "ONLINE"

        var savedUserId = prefs.getString("cloud_user_id", "") ?: ""
        if (savedUserId.isEmpty()) {
            savedUserId = "id_" + (100000..999999).random().toString()
            prefs.edit().putString("cloud_user_id", savedUserId).apply()
        }
        _cloudUserId.value = savedUserId

        var savedUserName = prefs.getString("cloud_user_name", "") ?: ""
        if (savedUserName.isEmpty()) {
            savedUserName = "User_" + (1000..9999).random().toString()
            prefs.edit().putString("cloud_user_name", savedUserName).apply()
        }
        _cloudUserName.value = savedUserName

        _cloudUserStatus.value = prefs.getString("cloud_user_status", "ONLINE") ?: "ONLINE"

        val savedRoomCode = prefs.getString("active_room_code", "") ?: ""
        if (savedRoomCode.isNotEmpty()) {
            _activeRoomCode.value = savedRoomCode
            _activePersonaId.value = savedRoomCode
            startCloudPolling(savedRoomCode)
        }
    }

    // Query messages dynamically depending on mode, personaId, and room code
    val activeMessages: StateFlow<List<ChatMessage>> = combine(
        _activeMode,
        _activePersonaId,
        _activeRoomCode
    ) { mode, personaId, roomCode ->
        val effectivePersonaId = when (mode) {
            "AI" -> personaId
            "CLOUD" -> roomCode
            else -> null
        }
        Pair(mode, effectivePersonaId)
    }.flatMapLatest { (mode, personaId) ->
        repository.getMessages(mode, personaId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setMode(mode: String) {
        _activeMode.value = mode
        prefs.edit().putString("active_mode", mode).apply()
        markIncomingMessagesAsRead()
    }

    fun markIncomingMessagesAsRead() {
        val mode = _activeMode.value
        val personaId = if (mode == "AI") _activePersonaId.value else null
        val currentUserId = if (mode == "AI") "user1" else _currentUserLocal.value
        viewModelScope.launch {
            repository.markMessagesAsRead(mode, personaId, currentUserId)
        }
    }

    fun markMessagesAsReadForUser(senderId: String) {
        val mode = _activeMode.value
        val personaId = if (mode == "AI") _activePersonaId.value else null
        viewModelScope.launch {
            repository.markMessagesAsRead(mode, personaId, senderId)
        }
    }

    fun setPersonaId(personaId: String) {
        _activePersonaId.value = personaId
        prefs.edit().putString("active_persona_id", personaId).apply()
        viewModelScope.launch {
            // Check if there are no messages for this AI persona. If so, insert the greeting automatically.
            val currentMsg = repository.personas.find { it.id == personaId }
            if (currentMsg != null) {
                // Check database asynchronously. Flow is reactive, but we can check if it's currently empty
                // We will defer insertion to UI layout check or simply launch a quick query.
            }
        }
    }

    fun insertInitialGreetingIfNeeded(personaId: String) {
        viewModelScope.launch {
            val persona = repository.personas.find { it.id == personaId } ?: return@launch
            // Get a single shot of messages if possible, or we let the UI trigger if activeMessages is empty.
        }
    }

    fun triggerInitialGreeting(persona: AiPersona) {
        viewModelScope.launch {
            val welcomeMessage = ChatMessage(
                senderName = persona.name,
                senderId = "ai",
                content = persona.initialGreeting,
                chatMode = "AI",
                personaId = persona.id
            )
            repository.insertMessage(welcomeMessage)
        }
    }

    fun setUserNames(u1: String, u2: String) {
        if (u1.isNotBlank()) {
            val t1 = u1.trim()
            _user1Name.value = t1
            prefs.edit().putString("user1_name", t1).apply()
        }
        if (u2.isNotBlank()) {
            val t2 = u2.trim()
            _user2Name.value = t2
            prefs.edit().putString("user2_name", t2).apply()
        }
    }

    fun setUser1Status(status: String) {
        _user1Status.value = status
        prefs.edit().putString("user1_status", status).apply()
    }

    fun setUser2Status(status: String) {
        _user2Status.value = status
        prefs.edit().putString("user2_status", status).apply()
    }

    fun setCloudUserName(name: String) {
        if (name.isNotBlank()) {
            val trimmed = name.trim()
            _cloudUserName.value = trimmed
            prefs.edit().putString("cloud_user_name", trimmed).apply()
            val roomCode = _activeRoomCode.value
            if (roomCode.isNotEmpty()) {
                viewModelScope.launch {
                    try {
                        com.example.data.api.CloudChatRetrofitClient.service.updateMemberStatus(
                            roomCode,
                            _cloudUserId.value,
                            com.example.data.api.MemberStatusDto(
                                userId = _cloudUserId.value,
                                name = trimmed,
                                status = _cloudUserStatus.value,
                                lastSeen = System.currentTimeMillis()
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Failed to update member name in sync", e)
                    }
                }
            }
        }
    }

    fun setCloudUserStatus(status: String) {
        _cloudUserStatus.value = status
        prefs.edit().putString("cloud_user_status", status).apply()
        val roomCode = _activeRoomCode.value
        if (roomCode.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    com.example.data.api.CloudChatRetrofitClient.service.updateMemberStatus(
                        roomCode,
                        _cloudUserId.value,
                        com.example.data.api.MemberStatusDto(
                            userId = _cloudUserId.value,
                            name = _cloudUserName.value,
                            status = status,
                            lastSeen = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Failed to update member status in sync", e)
                }
            }
        }
    }

    fun createRoom() {
        val code = (100000..999999).random().toString()
        _activeRoomCode.value = code
        _activePersonaId.value = code
        prefs.edit().putString("active_room_code", code).apply()
        viewModelScope.launch {
            repository.clearMessages("CLOUD", code) // Clear old stale items locally
        }
        startCloudPolling(code)
    }

    fun joinRoom(code: String) {
        val trimmed = code.trim()
        if (trimmed.length == 6) {
            _activeRoomCode.value = trimmed
            _activePersonaId.value = trimmed
            prefs.edit().putString("active_room_code", trimmed).apply()
            viewModelScope.launch {
                repository.clearMessages("CLOUD", trimmed) // Clear old stale items locally
            }
            startCloudPolling(trimmed)
        }
    }

    fun leaveRoom() {
        _activeRoomCode.value = ""
        prefs.edit().remove("active_room_code").apply()
        stopCloudPolling()
    }

    private fun startCloudPolling(roomCode: String) {
        stopCloudPolling()
        pollingJob = viewModelScope.launch {
            while (isActive && _activeRoomCode.value == roomCode) {
                syncCloudData(roomCode)
                delay(2000) // Poll database every 2 seconds
            }
        }
    }

    private fun stopCloudPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _cloudRoomMembers.value = emptyMap()
    }

    private suspend fun syncCloudData(roomCode: String) {
        val myId = _cloudUserId.value
        val myName = _cloudUserName.value
        val myStatus = _cloudUserStatus.value
        val apiService = com.example.data.api.CloudChatRetrofitClient.service

        try {
            // 1. Maintain heartbeats (update our member presence)
            val myPresence = com.example.data.api.MemberStatusDto(
                userId = myId,
                name = myName,
                status = myStatus,
                lastSeen = System.currentTimeMillis()
            )
            apiService.updateMemberStatus(roomCode, myId, myPresence)

            // 2. Query room data
            val roomData = apiService.getRoomData(roomCode)
            if (roomData != null) {
                // Sync remote presence lists
                _cloudRoomMembers.value = roomData.members ?: emptyMap()

                // Sync messages
                val cloudMessages = roomData.messages?.values?.sortedBy { it.timestamp } ?: emptyList()
                for (cloudMsg in cloudMessages) {
                    // Check if we already own it locally
                    val isExist = activeMessages.value.any { it.cloudId == cloudMsg.id }
                    if (!isExist) {
                        val localMsg = ChatMessage(
                            senderName = cloudMsg.senderName,
                            senderId = cloudMsg.senderId,
                            content = cloudMsg.content,
                            timestamp = cloudMsg.timestamp,
                            chatMode = "CLOUD",
                            personaId = roomCode,
                            isPending = false,
                            isRead = true, // Received locally, counted as read on our client
                            cloudId = cloudMsg.id
                        )
                        repository.insertMessage(localMsg)
                    }

                    // Handshake logic for read status (checkmark synchronization):
                    // B reads A's messages: B updates cloud isRead to true, tells the server B read them.
                    if (cloudMsg.senderId != myId && !cloudMsg.isRead && _activeMode.value == "CLOUD" && _activeRoomCode.value == roomCode) {
                        try {
                            apiService.updateMessageReadStatus(roomCode, cloudMsg.id, mapOf("isRead" to true))
                            // update local state
                            val localMsg = activeMessages.value.find { it.cloudId == cloudMsg.id }
                            if (localMsg != null && !localMsg.isRead) {
                                repository.insertMessage(localMsg.copy(isRead = true))
                            }
                        } catch (e: Exception) {
                            Log.e("ChatViewModel", "Failed to update read receipt for ${cloudMsg.id}", e)
                        }
                    }

                    // A notices B read them: A updates local isRead state to true when cloudMsg.isRead is downloaded as true.
                    if (cloudMsg.senderId == myId && cloudMsg.isRead) {
                        val localMsg = activeMessages.value.find { it.cloudId == cloudMsg.id }
                        if (localMsg != null && !localMsg.isRead) {
                            repository.insertMessage(localMsg.copy(isRead = true))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error syncing cloud data: ${e.localizedMessage}")
        }
    }

    fun toggleLocalSender() {
        _currentUserLocal.value = if (_currentUserLocal.value == "user1") "user2" else "user1"
        markIncomingMessagesAsRead()
    }

    fun sendLocalMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val senderId = _currentUserLocal.value
            val senderName = if (senderId == "user1") _user1Name.value else _user2Name.value
            val message = ChatMessage(
                senderName = senderName,
                senderId = senderId,
                content = trimmed,
                chatMode = "LOCAL",
                personaId = null,
                isRead = false // Starts unread until the other switches back/interacts
            )
            repository.insertMessage(message)
        }
    }

    fun sendFaceToFaceMessage(text: String, senderId: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val senderName = if (senderId == "user1") _user1Name.value else _user2Name.value
            val message = ChatMessage(
                senderName = senderName,
                senderId = senderId,
                content = trimmed,
                chatMode = "FACE_TO_FACE",
                personaId = null,
                isRead = false // Starts unread until the other side interacts or views
            )
            repository.insertMessage(message)
        }
    }

    fun sendCloudMessage(text: String) {
        val trimmed = text.trim()
        val roomCode = _activeRoomCode.value
        if (trimmed.isEmpty() || roomCode.isEmpty()) return

        viewModelScope.launch {
            val msgId = "msg_" + System.currentTimeMillis() + "_" + (1000..9999).random()
            val localMessage = ChatMessage(
                senderName = _cloudUserName.value,
                senderId = _cloudUserId.value,
                content = trimmed,
                timestamp = System.currentTimeMillis(),
                chatMode = "CLOUD",
                personaId = roomCode,
                isPending = false,
                isRead = false, // Not yet read by the other device
                cloudId = msgId
            )
            repository.insertMessage(localMessage)

            // Dispatch to server
            try {
                val cloudDto = com.example.data.api.CloudMessageDto(
                    id = msgId,
                    senderName = _cloudUserName.value,
                    senderId = _cloudUserId.value,
                    content = trimmed,
                    timestamp = localMessage.timestamp,
                    isRead = false
                )
                com.example.data.api.CloudChatRetrofitClient.service.uploadMessage(roomCode, msgId, cloudDto)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to dispatch message to cloud: ${e.localizedMessage}")
            }
        }
    }

    fun sendAiUserMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val personaId = _activePersonaId.value
            val userMsg = ChatMessage(
                senderName = "我 (Me)",
                senderId = "user1",
                content = trimmed,
                chatMode = "AI",
                personaId = personaId,
                isRead = false // Sent to AI, not yet read by AI companion
            )
            repository.insertMessage(userMsg)

            _isGenerating.value = true

            // Fetch recent history of this conversation to pass context
            val history = activeMessages.value + userMsg

            // Generate AI companion reply asynchronously
            val aiReplyText = repository.generateAiResponse(personaId, history)

            // AI companion "reads" our message:
            repository.markMessagesAsRead("AI", personaId, "ai")

            val aiResponseMsg = ChatMessage(
                senderName = repository.personas.find { it.id == personaId }?.name ?: "AI",
                senderId = "ai",
                content = aiReplyText,
                chatMode = "AI",
                personaId = personaId,
                isRead = false // Not yet read by the user (gets marked read when user interacts)
            )
            repository.insertMessage(aiResponseMsg)
            _isGenerating.value = false
        }
    }

    fun clearChat(mode: String, personaId: String? = null) {
        viewModelScope.launch {
            repository.clearMessages(mode, personaId)
            // If clearing AI chat, re-populate the greeting
            if (mode == "AI" && personaId != null) {
                val persona = repository.personas.find { it.id == personaId }
                if (persona != null) {
                    triggerInitialGreeting(persona)
                }
            }
        }
    }

    fun deleteMessage(message: ChatMessage) {
        viewModelScope.launch {
            repository.deleteMessage(message)
        }
    }

    // Factory helper
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
