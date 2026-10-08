package com.solarrobo.core.ai

/**
 * Natural language reasoning response emitted by AiEngine.
 * Note: AI output never directly actuates physical hardware.
 */
data class AiResponse(
    val text: String,
    val intent: String? = null,
    val confidence: Float? = null
)
