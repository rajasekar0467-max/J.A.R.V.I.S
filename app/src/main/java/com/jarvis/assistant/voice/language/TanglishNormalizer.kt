package com.jarvis.assistant.voice.language

/**
 * Deliverable 5 Module 3: Tamil Language Engine (Phase 1 Baseline).
 * Detects Tamil script, English, or Tanglish code-switching and normalizes common
 * Tanglish tokens for accurate Phase 1 voice test responses.
 */
enum class DetectedSpeechLanguage(val label: String, val ttsLocaleTag: String) {
    TAMIL("Tamil (தமிழ்)", "ta-IN"),
    ENGLISH("English (India)", "en-IN"),
    TANGLISH("Tanglish (Code-Switched)", "ta-IN")
}

object TanglishNormalizer {
    private val TAMIL_UNICODE_RANGE = Regex("[\\u0B80-\\u0BFF]")

    private val TANGLISH_LEXICON = mapOf(
        "evlo" to "எவ்வளவு",
        "epdi" to "எப்படி",
        "eppo" to "எப்போது",
        "enna" to "என்ன",
        "vanakkam" to "வணக்கம்",
        "nandri" to "நன்றி",
        "irukku" to "இருக்கு",
        "irukkinga" to "இருக்கீங்க",
        "sollu" to "சொல்லு",
        "pannu" to "பண்ணு"
    )

    fun detectLanguage(input: String): DetectedSpeechLanguage {
        val hasTamil = TAMIL_UNICODE_RANGE.containsMatchIn(input)
        val hasLatin = Regex("[A-Za-z]").containsMatchIn(input)
        if (hasTamil && hasLatin) return DetectedSpeechLanguage.TANGLISH
        if (hasTamil) return DetectedSpeechLanguage.TAMIL
        val tokens = input.lowercase().split(Regex("\\s+"))
        if (tokens.any { it in TANGLISH_LEXICON.keys }) {
            return DetectedSpeechLanguage.TANGLISH
        }
        return DetectedSpeechLanguage.ENGLISH
    }

    fun buildPhase1ControlledReply(recognizedText: String, selectedLang: String): Pair<String, String> {
        val detected = detectLanguage(recognizedText)
        val clean = recognizedText.trim()
        return when {
            selectedLang == "ta-IN" || detected == DetectedSpeechLanguage.TAMIL -> {
                "நான் உங்கள் குரலைத் தெளிவாகக் கேட்டேன்: $clean. நிலை 1 குரல் சோதனை வெற்றிகரமாக இயங்குகிறது." to "ta-IN"
            }
            detected == DetectedSpeechLanguage.TANGLISH -> {
                "உங்கள் Tanglish பேச்சு பதிவானது: $clean. நிலை 1 குரல் இயந்திரம் தயாராக உள்ளது." to "ta-IN"
            }
            else -> {
                "I heard you clearly: $clean. Phase 1 voice recognition and composed speech synthesis are operational." to "en-IN"
            }
        }
    }
}
