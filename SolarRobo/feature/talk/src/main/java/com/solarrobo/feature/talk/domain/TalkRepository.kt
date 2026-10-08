package com.solarrobo.feature.talk.domain

import kotlinx.coroutines.flow.Flow

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long,
    val intent: String? = null,
    val confidence: Float? = null
)

enum class MessageSender {
    USER,
    ROBO_AI
}

interface TalkRepository {
    fun getMessageHistory(): Flow<List<ChatMessage>>
    suspend fun sendMessage(userText: String): ChatMessage
}