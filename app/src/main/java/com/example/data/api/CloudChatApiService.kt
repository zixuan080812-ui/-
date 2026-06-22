package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class CloudMessageDto(
    val id: String = "",
    val senderName: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MemberStatusDto(
    val userId: String,
    val name: String,
    val status: String, // "ONLINE", "AWAY", "BUSY"
    val lastSeen: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class RoomDataDto(
    val messages: Map<String, CloudMessageDto>? = null,
    val members: Map<String, MemberStatusDto>? = null
)

interface CloudChatApiService {
    @GET("rooms/{roomCode}.json")
    suspend fun getRoomData(@Path("roomCode") roomCode: String): RoomDataDto?

    @PUT("rooms/{roomCode}/messages/{msgId}.json")
    suspend fun uploadMessage(
        @Path("roomCode") roomCode: String,
        @Path("msgId") msgId: String,
        @Body message: CloudMessageDto
    ): CloudMessageDto

    @PATCH("rooms/{roomCode}/messages/{msgId}.json")
    suspend fun updateMessageReadStatus(
        @Path("roomCode") roomCode: String,
        @Path("msgId") msgId: String,
        @Body updates: Map<String, Boolean>
    ): Map<String, Boolean>

    @PUT("rooms/{roomCode}/members/{userId}.json")
    suspend fun updateMemberStatus(
        @Path("roomCode") roomCode: String,
        @Path("userId") userId: String,
        @Body status: MemberStatusDto
    ): MemberStatusDto
}

object CloudChatRetrofitClient {
    private const val BASE_URL = "https://chat-companion-8f8bb-default-rtdb.firebaseio.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: CloudChatApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(CloudChatApiService::class.java)
    }
}
