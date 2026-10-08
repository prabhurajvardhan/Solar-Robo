package com.solarrobo.core.ai

/**
 * Natural language reasoning request for conversational assistant.
 */
data class AiRequest(
    val message: String,
    val context: Map<String, Any?> = emptyMap()
)
