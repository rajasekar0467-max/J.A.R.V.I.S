package com.jarvis.assistant.voice

import com.jarvis.assistant.language.LanguageMode

interface SpeechOutput {
    fun isReady(): Boolean
    fun availableVoiceNames(): List<String>
    fun speak(
        text: String,
        language: LanguageMode,
        speechRate: Float = FridayVoiceProfile.DEFAULT_SPEECH_RATE,
        pitch: Float = FridayVoiceProfile.DEFAULT_PITCH,
        preferredVoiceName: String = "",
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    )
    fun stop()
    fun shutdown()
}
