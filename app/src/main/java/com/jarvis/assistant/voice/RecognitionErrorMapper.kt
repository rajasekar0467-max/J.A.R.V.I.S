package com.jarvis.assistant.voice

import android.speech.SpeechRecognizer

data class MappedRecognitionError(
    val message: String,
    val retryable: Boolean
)

object RecognitionErrorMapper {

    fun map(errorCode: Int): MappedRecognitionError = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO ->
            MappedRecognitionError("Microphone audio error. Check if another app is using the mic.", true)
        SpeechRecognizer.ERROR_CLIENT ->
            MappedRecognitionError("Speech recognizer was interrupted.", true)
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            MappedRecognitionError("Microphone permission is required for voice input.", false)
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            MappedRecognitionError("Speech recognition needs a stable network connection.", true)
        SpeechRecognizer.ERROR_NO_MATCH ->
            MappedRecognitionError("Didn't catch that. Tap the mic and try again.", true)
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            MappedRecognitionError("Speech service is busy. Try again in a second.", true)
        SpeechRecognizer.ERROR_SERVER ->
            MappedRecognitionError("Speech recognition service returned an error.", true)
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            MappedRecognitionError("No speech detected. Tap the mic when you're ready.", true)
        else ->
            MappedRecognitionError("Voice recognition error (code $errorCode).", true)
    }
}
