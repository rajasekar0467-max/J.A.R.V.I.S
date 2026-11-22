package com.jarvis.assistant

import com.jarvis.assistant.ai.SseEvent
import com.jarvis.assistant.ai.SseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SseParserTest {

    @Test
    fun parsesDeltaEvent() {
        val line = "data: {\"choices\":[{\"delta\":{\"content\":\"Hello \"}}]}"
        assertEquals(SseEvent.Delta("Hello "), SseParser.parseLine(line))
    }

    @Test
    fun parsesDoneSentinel() {
        assertEquals(SseEvent.Done, SseParser.parseLine("data: [DONE]"))
    }

    @Test
    fun ignoresCommentsAndBlankLines() {
        assertNull(SseParser.parseLine(": keep-alive"))
        assertNull(SseParser.parseLine("   "))
    }

    @Test
    fun parsesErrorPayload() {
        val line = "data: {\"error\":{\"message\":\"Invalid key\"}}"
        val evt = SseParser.parseLine(line)
        assertTrue(evt is SseEvent.Error)
        assertEquals("Invalid key", (evt as SseEvent.Error).message)
    }
}
