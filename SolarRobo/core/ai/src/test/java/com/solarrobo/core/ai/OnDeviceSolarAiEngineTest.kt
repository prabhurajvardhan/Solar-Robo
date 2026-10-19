package com.solarrobo.core.ai

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceSolarAiEngineTest {
    private val engine = OnDeviceSolarAiEngine()

    @Test
    fun respondsToSolarTopicsWithoutNetwork() = runTest {
        val response = engine.generate(AiRequest("What should I do if there is a storm?"))

        assertEquals("WEATHER_SAFETY", response.intent)
        assertTrue(response.text.contains("Safety Gate"))
    }

    @Test
    fun unknownQuestionsRemainBoundedAndNonActuating() = runTest {
        val response = engine.generate(AiRequest("Tell me a story about the moon"))

        assertEquals("UNKNOWN", response.intent)
        assertTrue(response.text.contains("solar puzzle"))
    }
}
