package com.solarrobo.feature.talk

import com.solarrobo.core.ai.AiResponse
import com.solarrobo.feature.talk.domain.MessageSender
import com.solarrobo.feature.talk.data.TalkRepositoryImpl
import com.solarrobo.feature.talk.mock.FakeAiEngine
import com.solarrobo.feature.talk.mock.MockAiEngine
import com.solarrobo.feature.talk.presentation.TalkViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TalkViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsEmptyAndIdle() = runTest(dispatcher) {
        val viewModel = TalkViewModel(TalkRepositoryImpl(MockAiEngine()))

        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals("", viewModel.uiState.value.inputText)
        assertFalse(viewModel.uiState.value.isSending)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun sendAddsUserAndMockResponseThenClearsLoading() = runTest(dispatcher) {
        val engine = FakeAiEngine { AiResponse("Hello! How can I help you with Solar Robo?") }
        val viewModel = TalkViewModel(TalkRepositoryImpl(engine))
        runCurrent()
        viewModel.updateInput("hello")
        viewModel.sendMessage()

        assertTrue(viewModel.uiState.value.isSending)
        assertEquals("", viewModel.uiState.value.inputText)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSending)
        assertEquals(1, engine.requests.size)
        assertEquals(listOf(MessageSender.USER, MessageSender.ROBO_AI), viewModel.uiState.value.messages.map { it.sender })
        assertEquals("Hello! How can I help you with Solar Robo?", viewModel.uiState.value.messages.last().text)
    }

    @Test
    fun blankMessageIsNotSent() = runTest(dispatcher) {
        val engine = FakeAiEngine()
        val viewModel = TalkViewModel(TalkRepositoryImpl(engine))
        viewModel.updateInput("   ")

        viewModel.sendMessage()
        advanceUntilIdle()

        assertTrue(engine.requests.isEmpty())
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun aiFailureLeavesConversationUsableAndReportsError() = runTest(dispatcher) {
        val engine = FakeAiEngine { throw IllegalStateException("AI unavailable") }
        val viewModel = TalkViewModel(TalkRepositoryImpl(engine))
        runCurrent()
        viewModel.updateInput("hello")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSending)
        assertEquals("AI unavailable", viewModel.uiState.value.error)
        assertEquals(listOf(MessageSender.USER), viewModel.uiState.value.messages.map { it.sender })
        viewModel.updateInput("try again")
        assertEquals("try again", viewModel.uiState.value.inputText)
    }

    @Test
    fun multipleMessagesRemainInOrder() = runTest(dispatcher) {
        val engine = FakeAiEngine { request ->
            if (request.message == "hello") AiResponse("Hello reply") else AiResponse("Battery is 78% (mock).")
        }
        val viewModel = TalkViewModel(TalkRepositoryImpl(engine))
        runCurrent()

        viewModel.updateInput("hello")
        viewModel.sendMessage()
        advanceUntilIdle()
        viewModel.updateInput("what is the battery level")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.messages.size)
        assertEquals(listOf("hello", "Hello reply", "what is the battery level", "Battery is 78% (mock)."), viewModel.uiState.value.messages.map { it.text })
    }

    @Test
    fun intentAndConfidenceAreExposedWithoutExecutingIntent() = runTest(dispatcher) {
        val engine = FakeAiEngine { AiResponse("I can help with that.", "SOME_TEST_INTENT", 0.9f) }
        val viewModel = TalkViewModel(TalkRepositoryImpl(engine))
        runCurrent()
        viewModel.updateInput("test intent")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertEquals("I can help with that.", viewModel.uiState.value.messages.last().text)
        assertEquals("SOME_TEST_INTENT", viewModel.uiState.value.lastIntent)
        assertEquals(0.9f, viewModel.uiState.value.lastConfidence!!, 0f)
        assertEquals(1, engine.requests.size)
    }

    @Test
    fun contextIsLimitedToRecentMessages() = runTest(dispatcher) {
        val engine = FakeAiEngine()
        val repository = TalkRepositoryImpl(engine)
        repeat(4) { index -> repository.sendMessage("message $index") }
        repository.sendMessage("x".repeat(1000))
        repository.sendMessage("latest")

        val recent = engine.requests.last().context["recentMessages"] as List<*>
        val oldestRecentText = (recent.first() as Map<*, *>)["text"] as String
        val boundedLongText = (recent[4] as Map<*, *>)["text"] as String

        assertEquals(6, recent.size)
        assertEquals("message 2", oldestRecentText)
        assertEquals(500, boundedLongText.length)
    }

    @Test
    fun repeatedSendWhileLoadingIsIgnored() = runTest(dispatcher) {
        val engine = FakeAiEngine { kotlinx.coroutines.yield(); AiResponse("done") }
        val viewModel = TalkViewModel(TalkRepositoryImpl(engine))
        runCurrent()
        viewModel.updateInput("first")
        viewModel.sendMessage()
        viewModel.updateInput("second")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertEquals(1, engine.requests.size)
        assertEquals(listOf("first", "done"), viewModel.uiState.value.messages.map { it.text })
    }

    @Test
    fun mockEngineReturnsDeterministicResponses() = runTest(dispatcher) {
        val engine = MockAiEngine()
        val battery = engine.generate(com.solarrobo.core.ai.AiRequest("what is the battery level", emptyMap()))
        val angle = engine.generate(com.solarrobo.core.ai.AiRequest("what is the panel angle", emptyMap()))
        val protecting = engine.generate(com.solarrobo.core.ai.AiRequest("why is the robo protecting the panel", emptyMap()))
        val unknown = engine.generate(com.solarrobo.core.ai.AiRequest("tell me something unknown", emptyMap()))

        assertEquals("The battery level is 78% (mock response; not live telemetry).", battery.text)
        assertTrue(angle.text.contains("45 degrees"))
        assertTrue(angle.text.contains("not live telemetry"))
        assertTrue(protecting.text.contains("protecting mode"))
        assertTrue(protecting.text.contains("not live telemetry"))
        assertEquals("I don't have enough information to answer that yet.", unknown.text)
    }
}