package com.jarvis.assistant.settings

import com.jarvis.assistant.language.LanguageMode

data class AppSettings(
    val language: LanguageMode = LanguageMode.EN_IN,
    val providerPresetId: String = "gemini",
    val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta/openai/",
    val model: String = "gemini-2.0-flash",
    val temperature: Float = 0.7f,
    val maxContextMessages: Int = 20,
    val autoSpeak: Boolean = true,
    val speechRate: Float = 0.95f,
    val pitch: Float = 1.0f,
    val ttsVoiceName: String = "",
    val reducedMotion: Boolean = false,
    val listenTimeoutSec: Int = 8
)
