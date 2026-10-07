package com.solarrobo.feature.talk.mock

import com.solarrobo.core.ai.AiEngine
import com.solarrobo.core.ai.AiRequest
import com.solarrobo.core.ai.AiResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAiEngine @Inject constructor() : AiEngine {
    override suspend fun generate(request: AiRequest): AiResponse {
        return when (request.message.trim().lowercase()) {
            "hello", "hi", "hey" -> AiResponse("Hello! How can I help you with Solar Robo?")
            "what is the battery level", "what is the battery level?" ->
                AiResponse("The battery level is 78% (mock response; not live telemetry).")
            "what is the panel angle", "what is the panel angle?" ->
                AiResponse("The panel is currently at 45 degrees (mock response; not live telemetry).")
            "why is the robo protecting the panel", "why is the robo protecting the panel?" ->
                AiResponse("The Robo is currently in protecting mode because a safety condition was detected (mock response; not live telemetry).")
            else -> AiResponse("I don't have enough information to answer that yet.")
        }
    }
}