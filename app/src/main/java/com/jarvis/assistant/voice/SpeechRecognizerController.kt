package com.jarvis.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.jarvis.assistant.core.ErrorLog

class SpeechRecognizerController(
    private val context: Context,
    private val errorLog: ErrorLog
) : SpeechInput {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var activeCallbacks: SpeechInputCallbacks? = null
    private var timeoutRunnable: Runnable? = null

    override fun isAvailable(): Boolean =
        runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

    override fun startListening(languageTag: String, timeoutSec: Int, callbacks: SpeechInputCallbacks) {
        mainHandler.post {
            clearTimeout()
            if (!isAvailable()) {
                callbacks.onError(
                    SpeechRecognizer.ERROR_CLIENT,
                    "Speech recognition service is unavailable on this device."
                )
                return@post
            }
            activeCallbacks = callbacks
            runCatching {
                if (recognizer == null) {
                    recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                    recognizer?.setRecognitionListener(listener)
                }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                val safeTimeoutMs = timeoutSec.coerceIn(4, 20) * 1000L
                val guard = Runnable {
                    runCatching { recognizer?.stopListening() }
                }
                timeoutRunnable = guard
                mainHandler.postDelayed(guard, safeTimeoutMs)
                recognizer?.startListening(intent)
            }.onFailure { err ->
                errorLog.record("VoiceASR", "Failed to start SpeechRecognizer", err)
                callbacks.onError(SpeechRecognizer.ERROR_CLIENT, "Could not start voice recognition.")
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            clearTimeout()
            runCatching { recognizer?.stopListening() }
        }
    }

    override fun cancel() {
        mainHandler.post {
            clearTimeout()
            activeCallbacks = null
            runCatching { recognizer?.cancel() }
        }
    }

    override fun destroy() {
        mainHandler.post {
            clearTimeout()
            activeCallbacks = null
            runCatching { recognizer?.destroy() }
            recognizer = null
        }
    }

    private fun clearTimeout() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            activeCallbacks?.onReady()
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            activeCallbacks?.onRmsChanged(rmsdB)
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            clearTimeout()
        }

        override fun onError(error: Int) {
            clearTimeout()
            val mapped = RecognitionErrorMapper.map(error)
            activeCallbacks?.onError(error, mapped.message)
        }

        override fun onResults(results: Bundle?) {
            clearTimeout()
            val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val best = list?.firstOrNull()?.trim().orEmpty()
            if (best.isEmpty()) {
                activeCallbacks?.onError(SpeechRecognizer.ERROR_NO_MATCH, "Didn't catch that. Try again.")
            } else {
                activeCallbacks?.onFinal(best)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = list?.firstOrNull()?.trim().orEmpty()
            if (partial.isNotEmpty()) {
                activeCallbacks?.onPartial(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }
}
