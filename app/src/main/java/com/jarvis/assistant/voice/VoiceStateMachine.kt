package com.jarvis.assistant.voice

import com.jarvis.assistant.language.LanguageMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VoiceStateMachine {

    private val _snapshot = MutableStateFlow(VoiceSnapshot())
    val snapshot: StateFlow<VoiceSnapshot> = _snapshot.asStateFlow()

    fun startListening(language: LanguageMode) {
        _snapshot.value = VoiceSnapshot(
            state = VoiceState.LISTENING,
            partialTranscript = "",
            finalTranscript = "",
            rmsDb = 0f,
            errorMessage = null,
            activeLanguage = language
        )
    }

    fun onRmsChanged(rmsDb: Float) {
        _snapshot.update { current ->
            if (current.state == VoiceState.LISTENING) {
                current.copy(rmsDb = rmsDb.coerceIn(-2f, 12f))
            } else {
                current
            }
        }
    }

    fun onPartialTranscript(text: String) {
        _snapshot.update { current ->
            if (current.state == VoiceState.LISTENING) {
                current.copy(partialTranscript = text)
            } else {
                current
            }
        }
    }

    fun onFinalTranscript(text: String) {
        _snapshot.update { current ->
            current.copy(
                state = VoiceState.PROCESSING,
                finalTranscript = text,
                partialTranscript = text,
                rmsDb = 0f,
                errorMessage = null
            )
        }
    }

    fun onProcessing() {
        _snapshot.update { it.copy(state = VoiceState.PROCESSING, rmsDb = 0f, errorMessage = null) }
    }

    fun onSpeakingStarted() {
        _snapshot.update { it.copy(state = VoiceState.SPEAKING, rmsDb = 0f, errorMessage = null) }
    }

    fun onSpeakingFinished() {
        _snapshot.update { current ->
            if (current.state == VoiceState.SPEAKING) {
                current.copy(state = VoiceState.IDLE, rmsDb = 0f)
            } else {
                current
            }
        }
    }

    fun onError(message: String) {
        _snapshot.update {
            it.copy(
                state = VoiceState.ERROR,
                rmsDb = 0f,
                errorMessage = message
            )
        }
    }

    fun resetToIdle() {
        _snapshot.update {
            it.copy(
                state = VoiceState.IDLE,
                rmsDb = 0f,
                errorMessage = null
            )
        }
    }
}
