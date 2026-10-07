package com.solarrobo.core.ai

data class AiRequest(
    val message: String,
    val context: Map<String, Any?>
)

data class AiResponse(
    val text: String,
    val intent: String? = null,
    val confidence: Float? = null
)

interface AiEngine {
    suspend fun generate(request: AiRequest): AiResponse
}