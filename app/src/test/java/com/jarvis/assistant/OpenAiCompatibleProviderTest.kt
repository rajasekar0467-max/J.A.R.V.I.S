package com.jarvis.assistant

import com.jarvis.assistant.ai.AiErrorKind
import com.jarvis.assistant.ai.AiRequest
import com.jarvis.assistant.ai.AiResult
import com.jarvis.assistant.ai.ChatMessage
import com.jarvis.assistant.ai.OpenAiCompatibleProvider
import com.jarvis.assistant.core.ErrorLog
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenAiCompatibleProviderTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: OpenAiCompatibleProvider

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        provider = OpenAiCompatibleProvider(
            errorLog = ErrorLog(),
            allowCleartext = true
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun streamsSseDeltasSuccessfully() = runBlocking {
        val sse = buildString {
            appendLine("data: {\"choices\":[{\"delta\":{\"content\":\"Vanakkam \"}}]}")
            appendLine()
            appendLine("data: {\"choices\":[{\"delta\":{\"content\":\"nanba!\"}}]}")
            appendLine()
            appendLine("data: [DONE]")
        }
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/event-stream")
                .setBody(sse)
        )

        val chunks = mutableListOf<String>()
        val result = provider.streamReply(
            AiRequest(
                baseUrl = server.url("/v1/").toString(),
                apiKey = "sk-test-1234567890abcdef",
                model = "test-model",
                messages = listOf(ChatMessage("user", "Hi"))
            )
        ) { chunks.add(it) }

        assertTrue(result is AiResult.Success)
        assertEquals("Vanakkam nanba!", (result as AiResult.Success).fullText)
        assertEquals(listOf("Vanakkam ", "nanba!"), chunks)
    }

    @Test
    fun maps401ToUnauthorized() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{\"error\":\"bad key\"}"))
        val result = provider.streamReply(
            AiRequest(
                baseUrl = server.url("/v1/").toString(),
                apiKey = "sk-bad-1234567890abcdef",
                model = "test-model",
                messages = listOf(ChatMessage("user", "Hi"))
            )
        ) {}
        assertTrue(result is AiResult.Failure)
        assertEquals(AiErrorKind.UNAUTHORIZED, (result as AiResult.Failure).kind)
    }
}
