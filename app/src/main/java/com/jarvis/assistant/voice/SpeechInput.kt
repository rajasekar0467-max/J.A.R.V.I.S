package com.jarvis.assistant.voice

interface SpeechInputCallbacks {
    fun onReady()
    fun onRmsChanged(rmsDb: Float)
    fun onPartial(text: String)
    fun onFinal(text: String)
    fun onError(code: Int, message: String)
}

interface SpeechInput {
    fun isAvailable(): Boolean
    fun startListening(languageTag: String, timeoutSec: Int, callbacks: SpeechInputCallbacks)
    fun stopListening()
    fun cancel()
    fun destroy()
}
