package com.jarvis.assistant.voice

import android.speech.tts.Voice
import com.jarvis.assistant.language.LanguageMode
import java.util.Locale

/**
 * Single FRIDAY-inspired voice profile for JARVIS.
 * Selects the highest-quality natural voice installed on the device for the active locale
 * without hardcoding a gender or vendor-specific voice name.
 */
object FridayVoiceProfile {

    const val PROFILE_NAME: String = "FRIDAY-Inspired Natural"
    const val DEFAULT_SPEECH_RATE: Float = 0.95f
    const val DEFAULT_PITCH: Float = 1.0f

    fun targetLocale(language: LanguageMode): Locale = when (language) {
        LanguageMode.EN_IN -> Locale("en", "IN")
        LanguageMode.TA_IN -> Locale("ta", "IN")
        LanguageMode.TANGLISH -> Locale("en", "IN")
    }

    fun selectBestVoice(
        voices: Collection<Voice>,
        language: LanguageMode,
        preferredVoiceName: String = ""
    ): Voice? {
        if (voices.isEmpty()) return null
        if (preferredVoiceName.isNotBlank()) {
            voices.firstOrNull { it.name == preferredVoiceName }?.let { return it }
        }

        val target = targetLocale(language)
        return voices.maxByOrNull { voice ->
            scoreVoice(
                voiceLocaleLanguage = voice.locale?.language.orEmpty(),
                voiceLocaleCountry = voice.locale?.country.orEmpty(),
                quality = voice.quality,
                latency = voice.latency,
                requiresNetwork = voice.isNetworkConnectionRequired,
                targetLanguage = target.language,
                targetCountry = target.country
            )
        }
    }

    fun scoreVoice(
        voiceLocaleLanguage: String,
        voiceLocaleCountry: String,
        quality: Int,
        latency: Int,
        requiresNetwork: Boolean,
        targetLanguage: String,
        targetCountry: String
    ): Int {
        var score = 0
        if (voiceLocaleLanguage.equals(targetLanguage, ignoreCase = true)) {
            score += 500
            if (voiceLocaleCountry.equals(targetCountry, ignoreCase = true)) {
                score += 250
            }
        } else if (voiceLocaleLanguage.equals("en", ignoreCase = true)) {
            score += 120
        }
        // Prefer high quality and lower latency; slight bonus for offline availability
        score += quality / 10
        score -= latency / 20
        if (!requiresNetwork) score += 35
        return score
    }
}
