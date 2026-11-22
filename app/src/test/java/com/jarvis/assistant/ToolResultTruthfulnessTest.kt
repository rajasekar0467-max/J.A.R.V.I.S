package com.jarvis.assistant

import com.jarvis.assistant.core.ToolResult
import com.jarvis.assistant.core.TruthfulnessValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolResultTruthfulnessTest {

    @Test
    fun whatsappHandedOffForbidsClaimingMessageSent() {
        val handoff = ToolResult.HandedOff(
            toolName = "WhatsAppHandoff",
            targetPackageOrApp = "com.whatsapp",
            userInstruction = "WhatsApp is open with your message ready. Please tap Send."
        )
        assertFalse(TruthfulnessValidator.isClaimPermitted(handoff, "Your message has been sent!"))
        assertTrue(TruthfulnessValidator.isClaimPermitted(handoff, "WhatsApp is open with your message ready. Please tap Send."))
    }

    @Test
    fun bluetoothNeedsUserActionForbidsClaimingDirectToggle() {
        val action = ToolResult.NeedsUserAction(
            toolName = "BluetoothSettingsTool",
            requiredActionDescription = "Opened Bluetooth settings panel.",
            settingsIntentAction = "android.settings.BLUETOOTH_SETTINGS"
        )
        assertFalse(TruthfulnessValidator.isClaimPermitted(action, "I toggled Bluetooth on for you."))
        assertTrue(TruthfulnessValidator.isClaimPermitted(action, "I opened Bluetooth settings so you can toggle it."))
    }
}
