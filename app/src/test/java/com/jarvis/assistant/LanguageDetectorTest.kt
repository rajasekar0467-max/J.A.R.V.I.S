package com.jarvis.assistant

import com.jarvis.assistant.language.LanguageDetector
import com.jarvis.assistant.language.LanguageMode
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageDetectorTest {

    @Test
    fun detectsTamilScript() {
        assertEquals(LanguageMode.TA_IN, LanguageDetector.detect("வணக்கம் நண்பா எப்படி இருக்கீங்க"))
    }

    @Test
    fun detectsTanglishKeywords() {
        assertEquals(LanguageMode.TANGLISH, LanguageDetector.detect("Vanakkam machan epdi irukkinga"))
    }

    @Test
    fun defaultsToIndianEnglish() {
        assertEquals(LanguageMode.EN_IN, LanguageDetector.detect("Summarize my meeting notes for tomorrow"))
    }

    @Test
    fun preservesFallbackOnBlank() {
        assertEquals(LanguageMode.TA_IN, LanguageDetector.detect("   ", fallback = LanguageMode.TA_IN))
    }
}
