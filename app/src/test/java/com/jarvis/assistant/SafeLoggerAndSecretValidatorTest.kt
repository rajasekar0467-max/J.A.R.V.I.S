package com.jarvis.assistant

import com.jarvis.assistant.core.SafeLogger
import com.jarvis.assistant.security.SecretSlot
import com.jarvis.assistant.security.SecretSlotValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeLoggerAndSecretValidatorTest {

    @Test
    fun safeLoggerRedactsApiKeysAndBearerTokens() {
        val raw = "Using key AIzaSyD91234567890abcdefABCDEF and sk-proj1234567890abcdefghijkl"
        val redacted = SafeLogger.redactSecrets(raw)
        assertFalse(redacted.contains("AIzaSy"))
        assertFalse(redacted.contains("sk-proj"))
        assertTrue(redacted.contains("[REDACTED_SECRET]"))
    }

    @Test
    fun secretValidatorRejectsBlankAndNewlineInputs() {
        assertTrue(SecretSlotValidator.validateFormat(SecretSlot.GEMINI_API_KEY, "   ").isFailure)
        assertTrue(SecretSlotValidator.validateFormat(SecretSlot.GEMINI_API_KEY, "abc").isFailure)
        assertTrue(SecretSlotValidator.validateFormat(SecretSlot.GEMINI_API_KEY, "validKey12345").isSuccess)
        assertEquals("validKey12345", SecretSlotValidator.validateFormat(SecretSlot.GEMINI_API_KEY, "  validKey12345  ").getOrNull())
    }
}
