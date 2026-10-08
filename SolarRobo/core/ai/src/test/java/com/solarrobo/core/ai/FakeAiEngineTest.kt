package com.solarrobo.core.ai

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAiEngineTest {

    private val aiEngine = FakeAiEngine()

    @Test
    fun testGenerateStatusResponse() = runTest {
        val request = AiRequest("How is the Robo?")
        val response = aiEngine.generate(request)

        assertNotNull(response.text)
        assertTrue(response.text.contains("operating normally"))
        assertEquals("STATUS_QUERY", response.intent)
        assertTrue((response.confidence ?: 0f) > 0.9f)
    }

    @Test
    fun testGenerateEnergyResponse() = runTest {
        val request = AiRequest("What is the current solar power generation?")
        val response = aiEngine.generate(request)

        assertTrue(response.text.contains("Watts"))
        assertEquals("ENERGY_QUERY", response.intent)
    }

    @Test
    fun testGenerateFallbackResponse() = runTest {
        val request = AiRequest("Hello there!")
        val response = aiEngine.generate(request)

        assertTrue(response.text.contains("Solar Robo assistant"))
        assertEquals("GENERAL_CONVERSATION", response.intent)
    }
}
