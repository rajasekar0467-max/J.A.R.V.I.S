package com.jarvis.assistant.voice

import com.jarvis.assistant.language.LanguageMode

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

data class VoiceSnapshot(
    val state: VoiceState = VoiceState.IDLE,
    val partialTranscript: String = "",
    val finalTranscript: String = "",
    val rmsDb: Float = 0f,
    val errorMessage: String? = null,
    val activeLanguage: LanguageMode = LanguageMode.EN_IN
)
