package com.jarvis.assistant.voice.tts

data class VoiceProfileConfig(
    val engineId: String = "android_system_tts",
    val languageTag: String = "ta-IN",
    val pitch: Float = 0.85f,
    val speechRate: Float = 0.95f
)

interface TtsEngine {
    val engineName: String
    fun speak(text: String, config: VoiceProfileConfig)
    fun stop()
    fun shutdown()
}
