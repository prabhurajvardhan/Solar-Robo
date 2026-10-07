package com.solarrobo.feature.talk.presentation

import com.solarrobo.feature.talk.domain.ChatMessage

data class TalkUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
    val lastIntent: String? = null,
    val lastConfidence: Float? = null
)