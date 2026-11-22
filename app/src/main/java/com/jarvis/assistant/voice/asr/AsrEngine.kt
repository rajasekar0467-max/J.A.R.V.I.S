package com.jarvis.assistant.voice.asr

sealed interface AsrEvent {
    data object Ready : AsrEvent
    data object SpeechStarted : AsrEvent
    data class RmsChanged(val rmsDb: Float) : AsrEvent
    data class PartialResult(val text: String) : AsrEvent
    data class FinalResult(val text: String, val latencyMs: Long) : AsrEvent
    data class Error(val code: Int, val message: String) : AsrEvent
}

interface AsrEngine {
    val engineName: String
    fun isAvailable(): Boolean
    fun startListening(languageTag: String)
    fun stopListening()
    fun destroy()
}
