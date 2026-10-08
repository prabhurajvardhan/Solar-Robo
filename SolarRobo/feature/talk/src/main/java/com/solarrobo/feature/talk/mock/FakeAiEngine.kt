package com.solarrobo.feature.talk.mock

import com.solarrobo.core.ai.AiEngine
import com.solarrobo.core.ai.AiRequest
import com.solarrobo.core.ai.AiResponse

class FakeAiEngine(
    private val respond: suspend (AiRequest) -> AiResponse = { AiResponse("Fake response") }
) : AiEngine {
    val requests = mutableListOf<AiRequest>()

    override suspend fun generate(request: AiRequest): AiResponse {
        requests += request
        return respond(request)
    }
}