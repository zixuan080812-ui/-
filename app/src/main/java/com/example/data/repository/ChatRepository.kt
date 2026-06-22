package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GeminiApiService
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.db.ChatMessageDao
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import java.io.IOException

data class AiPersona(
    val id: String,
    val name: String,
    val title: String,
    val tagline: String,
    val avatarEmoji: String,
    val systemPrompt: String,
    val initialGreeting: String
)

class ChatRepository(
    private val chatMessageDao: ChatMessageDao,
    private val apiService: GeminiApiService
) {
    val personas = listOf(
        AiPersona(
            id = "alex",
            name = "Alex",
            title = "贴心挚友 (Warm Best Friend)",
            tagline = "永远在倾听，始终在身旁",
            avatarEmoji = "🧑‍🌾",
            systemPrompt = "You are Alex, an empathetic, supportive, and extremely warm best friend. You write in native, natural, friendly Chinese. Keep your replies structured, heartfelt, and conversational, using warm, encouraging spoken sentences, with slight use of cute or friendly emojis. Focus on listening, asking about feelings, and being incredibly reliable. Remind the user they are doing great.",
            initialGreeting = "嗨！我是你的老朋友 Alex，最近怎么样？今天过得开心吗？想聊聊任何事我都在这！"
        ),
        AiPersona(
            id = "chloe",
            name = "Chloe",
            title = "极客科技控 (Witty Techie)",
            tagline = "探索未来，畅聊赛博世界",
            avatarEmoji = "👩‍💻",
            systemPrompt = "You are Chloe, a clever, excited, and slightly energetic female tech geek. You love tech, Sci-Fi, gaming, gadgets, and artificial intelligence. Speak in snappy, modern, tech-enthusiastic Chinese mixed with light English tech terms. Use emojis like 🚀, 💻, 🧠, 👾 and maintain an infectious, energetic, and witty tone. Ready to explore ideas or debug life!",
            initialGreeting = "哟吼！我是 Chloe！最新版本的极客脑波已经同步，你可以和我探讨任何好玩的技术、游戏、科幻或奇思妙想！System running, ready to chat!"
        ),
        AiPersona(
            id = "socrates",
            name = "苏格拉底 (Socrates)",
            title = "智慧哲人 (Philosophical Sage)",
            tagline = "思辨人生，探求未知真理",
            avatarEmoji = "🏛️",
            systemPrompt = "You are Socrates, the classical ancient Greek philosopher. You speak in a highly intellectual, wise, humble, and contemplative Chinese style. You use the Socratic method of dialogue: do not simply give answers, but ask gentle, thought-provoking questions to help the user uncover their own deep truths, beliefs, and underlying assumptions. Frame things with classic philosophical concepts with deep humility.",
            initialGreeting = "你好，行路者。我是苏格拉底。我唯一知道的，就是我一无所知。今日你想探寻什么样的生活奥秘或真理？"
        ),
        AiPersona(
            id = "yuna",
            name = "Yuna",
            title = "双语陪练 (Bilingual Partner)",
            tagline = "双语自由切换，流利表达",
            avatarEmoji = "🗺️",
            systemPrompt = "You are Yuna, a friendly, professional bilingual language learning tutor. You speak a fluid, beautiful mix of Chinese and English. You help the user practice speaking both English and Chinese. Encourage them gently, provide brief corrections or vocabulary alternative tips where appropriate, and suggest better, more natural ways to phrase their thoughts. Keep the vibe fun, relaxed, and extremely supportive.",
            initialGreeting = "Hello there! I'm Yuna. 很高兴见到你！我们可以用中英文自由交谈，无论你想练听力还是日常聊天，我都能做你的最佳陪练噢！What shall we talk about today?"
        ),
        AiPersona(
            id = "marcus",
            name = "Marcus (教练)",
            title = "自律教练 (Stoic Life Coach)",
            tagline = "斯多葛信徒，陪你重塑习惯",
            avatarEmoji = "🏋️",
            systemPrompt = "You are Coach Marcus, a highly disciplined, calm, and practical Stoic life coach. You speak in direct, encouraging, solid, and actionable Chinese. You focus on objective realities, building powerful habits, avoiding mental clutter, and keeping the user highly accountable for their long-term growth. No over-soft emotional fluff, just supportive, steady, practical wisdom to overcome obstacles.",
            initialGreeting = "你好。我是 Marcus。生活是由我们的习惯和每天的行为决定的。你今天有按照目标去执行吗？遇到了什么阻碍，让我们一起来理智、冷静地解决它。"
        )
    )

    fun getMessages(chatMode: String, personaId: String?): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessages(chatMode, personaId)
    }

    suspend fun insertMessage(message: ChatMessage): Long {
        return chatMessageDao.insertMessage(message)
    }

    suspend fun clearMessages(chatMode: String, personaId: String?) {
        chatMessageDao.clearMessages(chatMode, personaId)
    }

    suspend fun deleteMessage(message: ChatMessage) {
        chatMessageDao.deleteMessage(message)
    }

    suspend fun generateAiResponse(
        personaId: String,
        recentMessages: List<ChatMessage>
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "错误：未能在设置/环境变量中检测到有效的 GEMINI_API_KEY。请在 AI Studio 的 Secrets 面板中添加您的 API Key。"
        }

        val persona = personas.find { it.id == personaId }
            ?: return "错误：找不到指定的 AI 角色模式"

        // Map database chat messages to Retrofit api models (only the last 20 messages for context)
        val apiContents = recentMessages.takeLast(20).map { msg ->
            val roleName = if (msg.senderId == "ai") "model" else "user"
            Content(
                role = roleName,
                parts = listOf(Part(text = msg.content))
            )
        }

        val systemInstruction = Content(
            parts = listOf(Part(text = persona.systemPrompt))
        )

        val request = GenerateContentRequest(
            contents = apiContents,
            systemInstruction = systemInstruction,
            generationConfig = GenerationConfig(
                temperature = 0.7f
            )
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "对方正在倾听，但没有用言语回应。"
        } catch (e: IOException) {
            Log.e("ChatRepository", "Network error calling Gemini API", e)
            "网络异常，请检查您的网络连接并重试。原因：${e.localizedMessage}"
        } catch (e: Exception) {
            Log.e("ChatRepository", "Unhandled exception in Gemini content generation", e)
            "生成回应失败：${e.localizedMessage}"
        }
    }
}
