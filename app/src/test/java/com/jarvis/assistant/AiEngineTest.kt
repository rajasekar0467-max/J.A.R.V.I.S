package com.jarvis.assistant

import com.jarvis.assistant.ai.AiDiagnostics
import com.jarvis.assistant.ai.AiEngine
import com.jarvis.assistant.ai.AiErrorKind
import com.jarvis.assistant.ai.AiProvider
import com.jarvis.assistant.ai.AiRequest
import com.jarvis.assistant.ai.AiResult
import com.jarvis.assistant.ai.ChatMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiEngineTest {

    @Test
    fun retriesOnceBeforeFirstDeltaOnTransientFailure() = runBlocking {
        var calls = 0
        val fakeProvider = object : AiProvider {
            override suspend fun streamReply(request: AiRequest, onDelta: suspend (String) -> Unit): AiResult {
                calls++
                return if (calls == 1) {
                    AiResult.Failure(AiErrorKind.SERVER_ERROR, "Server busy", 503)
                } else {
                    onDelta("Recovered")
                    AiResult.Success("Recovered", 25L)
                }
            }

            override suspend fun diagnose(baseUrl: String, apiKey: String, model: String): AiDiagnostics =
                AiDiagnostics(true, 200, 10L, "OK")
        }

        val engine = AiEngine(fakeProvider, maxRetriesBeforeFirstDelta = 1, sleeper = {})
        val res = engine.generateReply(
            AiRequest("https://example.com/v1/", "sk-1234567890123456", "m", listOf(ChatMessage("user", "hi")))
        ) {}

        assertEquals(2, calls)
        assertTrue(res is AiResult.Success)
        assertEquals("Recovered", (res as AiResult.Success).fullText)
    }

    @Test
    fun doesNotRetryAfterFirstDeltaEmitted() = runBlocking {
        var calls = 0
        val fakeProvider = object : AiProvider {
            override suspend fun streamReply(request: AiRequest, onDelta: suspend (String) -> Unit): AiResult {
                calls++
                onDelta("Partial ")
                return AiResult.Failure(AiErrorKind.TIMEOUT, "Mid-stream timeout")
            }

            override suspend fun diagnose(baseUrl: String, apiKey: String, model: String): AiDiagnostics =
                AiDiagnostics(false, null, 10L, "Timeout")
        }

        val engine = AiEngine(fakeProvider, maxRetriesBeforeFirstDelta = 1, sleeper = {})
        val res = engine.generateReply(
            AiRequest("https://example.com/v1/", "sk-1234567890123456", "m", listOf(ChatMessage("user", "hi")))
        ) {}

        assertEquals(1, calls)
        assertTrue(res is AiResult.Failure)
    }
}
