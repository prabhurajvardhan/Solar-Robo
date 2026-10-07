package com.solarrobo.feature.talk.data

import com.solarrobo.core.ai.AiEngine
import com.solarrobo.core.ai.AiRequest
import com.solarrobo.feature.talk.domain.ChatMessage
import com.solarrobo.feature.talk.domain.MessageSender
import com.solarrobo.feature.talk.domain.TalkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class TalkRepositoryImpl @Inject constructor(
    private val aiEngine: AiEngine
) : TalkRepository {

    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    override fun getMessageHistory(): Flow<List<ChatMessage>> = messages.asStateFlow()

    override suspend fun sendMessage(userText: String): ChatMessage {
        require(userText.isNotBlank()) { "Message must not be blank." }

        val priorMessages = messages.value
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = userText,
            timestamp = System.currentTimeMillis()
        )
        messages.update { it + userMessage }

        val response = aiEngine.generate(buildRequest(userText, priorMessages))
        val assistantMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.ROBO_AI,
            text = response.text,
            timestamp = System.currentTimeMillis(),
            intent = response.intent,
            confidence = response.confidence
        )
        messages.update { it + assistantMessage }
        return assistantMessage
    }

    private fun buildRequest(message: String, history: List<ChatMessage>): AiRequest {
        val recentMessages = history.takeLast(MAX_CONTEXT_MESSAGES).map { item ->
            mapOf(
                "role" to if (item.sender == MessageSender.USER) "user" else "assistant",
                "text" to item.text.take(MAX_CONTEXT_MESSAGE_LENGTH)
            )
        }
        return AiRequest(message = message, context = mapOf("recentMessages" to recentMessages))
    }

    private companion object {
        const val MAX_CONTEXT_MESSAGES = 6
        const val MAX_CONTEXT_MESSAGE_LENGTH = 500
    }
}