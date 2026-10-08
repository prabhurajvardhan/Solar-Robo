package com.solarrobo.core.ai

/**
 * Model-agnostic AI reasoning engine interface.
 * Hardware commands must NEVER be directly dispatched by this layer.
 */
interface AiEngine {
    suspend fun generate(request: AiRequest): AiResponse
}
