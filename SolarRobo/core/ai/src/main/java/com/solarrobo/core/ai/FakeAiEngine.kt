package com.solarrobo.core.ai

/**
 * Deterministic fake AI engine providing canned conversational responses for testing and previews.
 */
class FakeAiEngine : AiEngine {

    override suspend fun generate(request: AiRequest): AiResponse {
        val query = request.message.trim().lowercase()
        return when {
            query.contains("how is the robo") || query.contains("status") -> {
                AiResponse(
                    text = "The Robo is operating normally. Battery is healthy and solar generation is stable.",
                    intent = "STATUS_QUERY",
                    confidence = 0.99f
                )
            }
            query.contains("generation") || query.contains("solar") || query.contains("power") -> {
                AiResponse(
                    text = "Current solar generation is yielding approximately 320 Watts with optimal panel tilt.",
                    intent = "ENERGY_QUERY",
                    confidence = 0.95f
                )
            }
            query.contains("battery") -> {
                AiResponse(
                    text = "Battery storage is currently at 94% with nominal charge rate.",
                    intent = "BATTERY_QUERY",
                    confidence = 0.95f
                )
            }
            query.contains("safety") || query.contains("wind") -> {
                AiResponse(
                    text = "Wind velocity is within safe operating parameters (under 15 m/s). Safety gate is set to NORMAL.",
                    intent = "SAFETY_QUERY",
                    confidence = 0.95f
                )
            }
            else -> {
                AiResponse(
                    text = "I am Solar Robo assistant. I monitor tracker telemetry, energy generation, and safety status.",
                    intent = "GENERAL_CONVERSATION",
                    confidence = 0.90f
                )
            }
        }
    }
}
