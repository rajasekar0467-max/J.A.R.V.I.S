package com.jarvis.assistant.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.jarvis.assistant.core.ErrorLog
import com.jarvis.assistant.language.LanguageMode
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class TtsController(
    context: Context,
    private val errorLog: ErrorLog
) : SpeechOutput {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ready = AtomicBoolean(false)
    private val generation = AtomicLong(0L)

    @Volatile
    private var tts: TextToSpeech? = null

    @Volatile
    private var currentCallbacks: Callbacks? = null

    private data class Callbacks(
        val gen: Long,
        val lastUtteranceId: String,
        val onStart: () -> Unit,
        val onDone: () -> Unit,
        val onError: (String) -> Unit
    )

    init {
        runCatching {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ready.set(true)
                    tts?.setOnUtteranceProgressListener(progressListener)
                } else {
                    ready.set(false)
                    errorLog.record("VoiceTTS", "TextToSpeech init failed with status $status")
                }
            }
        }.onFailure { err ->
            ready.set(false)
            errorLog.record("VoiceTTS", "TextToSpeech constructor failed", err)
        }
    }

    override fun isReady(): Boolean = ready.get() && tts != null

    override fun availableVoiceNames(): List<String> {
        val engine = tts ?: return emptyList()
        if (!ready.get()) return emptyList()
        return runCatching {
            engine.voices.orEmpty()
                .filter { it.locale?.language in setOf("en", "ta") }
                .map { it.name }
                .sorted()
        }.getOrDefault(emptyList())
    }

    override fun speak(
        text: String,
        language: LanguageMode,
        speechRate: Float,
        pitch: Float,
        preferredVoiceName: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        val engine = tts
        if (!ready.get() || engine == null) {
            onError("Text-to-speech engine is not ready.")
            return
        }
        val chunks = SpeechTextSanitizer.chunkForTts(text)
        if (chunks.isEmpty()) {
            onDone()
            return
        }

        val gen = generation.incrementAndGet()
        val lastId = "jarvis_${gen}_${chunks.lastIndex}"
        currentCallbacks = Callbacks(gen, lastId, onStart, onDone, onError)

        runCatching {
            val voices = runCatching { engine.voices.orEmpty() }.getOrDefault(emptySet())
            val bestVoice = FridayVoiceProfile.selectBestVoice(voices, language, preferredVoiceName)
            if (bestVoice != null) {
                engine.voice = bestVoice
            } else {
                engine.language = FridayVoiceProfile.targetLocale(language)
            }
            engine.setSpeechRate(speechRate.coerceIn(0.6f, 1.5f))
            engine.setPitch(pitch.coerceIn(0.75f, 1.35f))

            chunks.forEachIndexed { index, chunk ->
                val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                val utteranceId = "jarvis_${gen}_$index"
                engine.speak(chunk, mode, null, utteranceId)
            }
        }.onFailure { err ->
            errorLog.record("VoiceTTS", "TTS speak failed", err)
            mainHandler.post { onError("Could not speak reply.") }
        }
    }

    override fun stop() {
        generation.incrementAndGet()
        currentCallbacks = null
        runCatching { tts?.stop() }
    }

    override fun shutdown() {
        generation.incrementAndGet()
        currentCallbacks = null
        ready.set(false)
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            val cb = currentCallbacks ?: return
            if (utteranceId == "jarvis_${cb.gen}_0") {
                mainHandler.post { cb.onStart() }
            }
        }

        override fun onDone(utteranceId: String?) {
            val cb = currentCallbacks ?: return
            if (utteranceId == cb.lastUtteranceId) {
                mainHandler.post { cb.onDone() }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            val cb = currentCallbacks ?: return
            if (utteranceId?.startsWith("jarvis_${cb.gen}_") == true) {
                mainHandler.post { cb.onError("Text-to-speech playback failed.") }
            }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            val cb = currentCallbacks ?: return
            if (utteranceId?.startsWith("jarvis_${cb.gen}_") == true) {
                mainHandler.post { cb.onError("Text-to-speech error ($errorCode).") }
            }
        }
    }
}
