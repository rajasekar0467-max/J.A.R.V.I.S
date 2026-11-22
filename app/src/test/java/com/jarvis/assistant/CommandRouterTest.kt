package com.jarvis.assistant

import com.jarvis.assistant.device.Command
import com.jarvis.assistant.device.CommandRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandRouterTest {

    @Test
    fun parsesTimeAndDateInEnglishAndTanglish() {
        assertEquals(Command.Time, CommandRouter.parse("What time is it?"))
        assertEquals(Command.Time, CommandRouter.parse("ippo time enna"))
        assertEquals(Command.Date, CommandRouter.parse("Today's date"))
    }

    @Test
    fun parsesReminderWithDelay() {
        val cmd = CommandRouter.parse("Remind me in 15 minutes to call mom")
        assertTrue(cmd is Command.SetReminder)
        val rem = cmd as Command.SetReminder
        assertEquals(15, rem.delayMinutes)
        assertEquals("call mom", rem.text)
    }

    @Test
    fun parsesTimer() {
        val cmd = CommandRouter.parse("Set a timer for 5 minutes")
        assertEquals(Command.SetTimer(300, "JARVIS Timer"), cmd)
    }

    @Test
    fun parsesSettingsAndApps() {
        assertEquals(Command.OpenSettings("wifi"), CommandRouter.parse("Open Wi-Fi settings"))
        assertEquals(Command.OpenApp("WhatsApp"), CommandRouter.parse("Open WhatsApp"))
    }

    @Test
    fun parsesMemoryFact() {
        assertEquals(Command.RememberFact("I use Realme 9i 5G"), CommandRouter.parse("Remember that I use Realme 9i 5G"))
    }

    @Test
    fun fallsThroughToNoneForGeneralChat() {
        assertEquals(Command.None, CommandRouter.parse("Explain quantum entanglement simply"))
    }
}
