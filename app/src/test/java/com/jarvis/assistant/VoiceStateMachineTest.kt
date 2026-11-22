package com.jarvis.assistant

import com.jarvis.assistant.language.LanguageMode
import com.jarvis.assistant.voice.VoiceState
import com.jarvis.assistant.voice.VoiceStateMachine
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceStateMachineTest {

    @Test
    fun transitionsThroughFullVoiceLifecycle() {
        val sm = VoiceStateMachine()
        assertEquals(VoiceState.IDLE, sm.snapshot.value.state)

        sm.startListening(LanguageMode.EN_IN)
        assertEquals(VoiceState.LISTENING, sm.snapshot.value.state)

        sm.onPartialTranscript("What time")
        assertEquals("What time", sm.snapshot.value.partialTranscript)

        sm.onFinalTranscript("What time is it")
        assertEquals(VoiceState.PROCESSING, sm.snapshot.value.state)
        assertEquals("What time is it", sm.snapshot.value.finalTranscript)

        sm.onSpeakingStarted()
        assertEquals(VoiceState.SPEAKING, sm.snapshot.value.state)

        sm.onSpeakingFinished()
        assertEquals(VoiceState.IDLE, sm.snapshot.value.state)
    }

    @Test
    fun recordsErrorAndResetsCleanly() {
        val sm = VoiceStateMachine()
        sm.onError("Mic unavailable")
        assertEquals(VoiceState.ERROR, sm.snapshot.value.state)
        assertEquals("Mic unavailable", sm.snapshot.value.errorMessage)

        sm.resetToIdle()
        assertEquals(VoiceState.IDLE, sm.snapshot.value.state)
    }
}
