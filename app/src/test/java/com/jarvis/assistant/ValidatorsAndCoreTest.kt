package com.jarvis.assistant

import com.jarvis.assistant.ai.ContextWindow
import com.jarvis.assistant.ai.PromptBuilder
import com.jarvis.assistant.ai.UrlValidator
import com.jarvis.assistant.conversation.MemoryFact
import com.jarvis.assistant.conversation.Message
import com.jarvis.assistant.core.ErrorLog
import com.jarvis.assistant.core.Redactor
import com.jarvis.assistant.language.LanguageMode
import com.jarvis.assistant.security.ApiKeyValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsAndCoreTest {

    @Test
    fun urlValidatorRejectsHttpInReleaseAndNormalizesHttps() {
        assertTrue(UrlValidator.validate("http://example.com/v1", allowCleartext = false).isFailure)
        val ok = UrlValidator.validate("https://api.openai.com/v1", allowCleartext = false)
        assertTrue(ok.isSuccess)
        assertEquals("https://api.openai.com/v1/", ok.getOrThrow())
    }

    @Test
    fun apiKeyValidatorRejectsShortOrWhitespaceKeys() {
        assertTrue(ApiKeyValidator.validate("short").isFailure)
        assertTrue(ApiKeyValidator.validate("sk-has space in key 12345").isFailure)
        assertTrue(ApiKeyValidator.validate("sk-validkey1234567890").isSuccess)
    }

    @Test
    fun redactorMasksBearerAndProviderTokens() {
        val raw = "Authorization: Bearer sk-abc12345678901234567890 and AIzaSyD1234567890123456789012345"
        val clean = Redactor.redact(raw)
        assertFalse(clean.contains("sk-abc12345678901234567890"))
        assertFalse(clean.contains("AIzaSyD1234567890123456789012345"))
    }

    @Test
    fun errorLogCapsSizeAndRedacts() {
        val log = ErrorLog(maxEntries = 3, clock = { 100L })
        repeat(5) { idx -> log.record("T", "msg $idx sk-secret1234567890123456") }
        val snap = log.snapshot()
        assertEquals(3, snap.size)
        assertFalse(snap.last().message.contains("sk-secret1234567890123456"))
    }

    @Test
    fun contextWindowAndPromptBuilderIncludeMemoryAndSystemPrompt() {
        val msgs = listOf(
            Message(1, 1, "user", "Hello", 1L, "user"),
            Message(2, 1, "assistant", "Hi there", 2L, "ai")
        )
        val facts = listOf(MemoryFact(1, "User prefers concise replies", 1L))
        val built = PromptBuilder.buildMessages(msgs, facts, LanguageMode.TANGLISH, maxContextMessages = 10)
        assertEquals(3, built.size)
        assertEquals("system", built.first().role)
        assertTrue(built.first().content.contains("Tanglish"))
        assertTrue(built.first().content.contains("User prefers concise replies"))
        assertEquals(2, ContextWindow.selectRecent(msgs, 10).size)
    }
}
