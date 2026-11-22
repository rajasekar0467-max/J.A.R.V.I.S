package com.jarvis.assistant

import com.jarvis.assistant.language.LanguageMode
import com.jarvis.assistant.voice.FridayVoiceProfile
import com.jarvis.assistant.voice.MicPermissionResolver
import com.jarvis.assistant.voice.MicPermissionState
import com.jarvis.assistant.voice.RecognitionErrorMapper
import com.jarvis.assistant.voice.SpeechTextSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceHelpersTest {

    @Test
    fun sanitizesMarkdownAndChunksLongText() {
        val raw = "Here is **bold** text and [a link](https://example.com) and ```kotlin\nval x = 1\n```."
        val clean = SpeechTextSanitizer.sanitize(raw)
        assertFalse(clean.contains("**"))
        assertFalse(clean.contains("https://example.com"))
        assertTrue(clean.contains("Code block omitted."))

        val longText = List(12) { "Sentence number $it is clear and concise." }.joinToString(" ")
        val chunks = SpeechTextSanitizer.chunkForTts(longText, maxChunkChars = 120)
        assertTrue(chunks.size >= 3)
        assertTrue(chunks.all { it.length <= 120 })
    }

    @Test
    fun fridayVoiceScoringPrefersExactLocaleAndOffline() {
        val enInScore = FridayVoiceProfile.scoreVoice("en", "IN", 400, 200, false, "en", "IN")
        val enUsScore = FridayVoiceProfile.scoreVoice("en", "US", 400, 200, true, "en", "IN")
        assertTrue(enInScore > enUsScore)
        assertEquals("en", FridayVoiceProfile.targetLocale(LanguageMode.TANGLISH).language)
    }

    @Test
    fun resolvesMicPermissionStates() {
        assertEquals(MicPermissionState.GRANTED, MicPermissionResolver.resolve(true, true, false))
        assertEquals(MicPermissionState.DENIED_RETRYABLE, MicPermissionResolver.resolve(false, false, false))
        assertEquals(MicPermissionState.DENIED_RETRYABLE, MicPermissionResolver.resolve(false, true, true))
        assertEquals(MicPermissionState.DENIED_PERMANENT, MicPermissionResolver.resolve(false, true, false))
    }

    @Test
    fun mapsSpeechErrorCodes() {
        val mapped = RecognitionErrorMapper.map(9) // ERROR_INSUFFICIENT_PERMISSIONS
        assertFalse(mapped.retryable)
    }
}
