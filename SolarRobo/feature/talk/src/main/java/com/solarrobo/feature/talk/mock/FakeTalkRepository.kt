package com.solarrobo.feature.talk.mock

import com.solarrobo.feature.talk.data.TalkRepositoryImpl
import com.solarrobo.feature.talk.domain.ChatMessage
import com.solarrobo.feature.talk.domain.TalkRepository
import kotlinx.coroutines.flow.Flow

class FakeTalkRepository(
    aiEngine: FakeAiEngine = FakeAiEngine()
) : TalkRepository {
    private val delegate = TalkRepositoryImpl(aiEngine)

    override fun getMessageHistory(): Flow<List<ChatMessage>> = delegate.getMessageHistory()

    override suspend fun sendMessage(userText: String): ChatMessage = delegate.sendMessage(userText)
}