package com.jarvis.assistant.core

/**
 * Deliverable 4 & 8 Non-Negotiable Rule #2:
 * Every tool returns a typed result. Spoken and written replies may only claim
 * what the result type proves. Never say an action succeeded unless it did.
 */
sealed interface ToolResult {
    val toolName: String

    /** Action completed and outcome was programmatically verified on-device or via API. */
    data class Success(
        override val toolName: String,
        val verifiedSummary: String,
        val verifiedData: Map<String, String> = emptyMap()
    ) : ToolResult

    /**
     * Action was handed off to an external app (e.g., WhatsApp wa.me / ACTION_SEND).
     * JARVIS must state that the app was opened with the payload ready, and NEVER claim delivery.
     */
    data class HandedOff(
        override val toolName: String,
        val targetPackageOrApp: String,
        val userInstruction: String
    ) : ToolResult

    /**
     * Action requires explicit user interaction in a system UI (e.g., Android 13+ Bluetooth/Wi-Fi panel).
     */
    data class NeedsUserAction(
        override val toolName: String,
        val requiredActionDescription: String,
        val settingsIntentAction: String? = null
    ) : ToolResult

    /** Operation is not supported on this Android version or device hardware. */
    data class Unsupported(
        override val toolName: String,
        val reason: String,
        val realisticAlternative: String
    ) : ToolResult

    /** Operation failed with an explicit error. */
    data class Failed(
        override val toolName: String,
        val errorReason: String,
        val recoverable: Boolean
    ) : ToolResult
}

object TruthfulnessValidator {
    fun formatHonestReply(result: ToolResult): String {
        return when (result) {
            is ToolResult.Success -> "Verified: ${result.verifiedSummary}"
            is ToolResult.HandedOff ->
                "Handed off to ${result.targetPackageOrApp}: ${result.userInstruction} (Delivery is not verified by JARVIS)."
            is ToolResult.NeedsUserAction ->
                "Action required by user: ${result.requiredActionDescription}"
            is ToolResult.Unsupported ->
                "Unsupported on this device/OS: ${result.reason}. Alternative: ${result.realisticAlternative}"
            is ToolResult.Failed ->
                "Failed (${result.toolName}): ${result.errorReason}"
        }
    }

    fun isClaimPermitted(result: ToolResult, proposedReply: String): Boolean {
        val lower = proposedReply.lowercase()
        return when (result) {
            is ToolResult.Success -> true
            is ToolResult.HandedOff -> {
                // Must NEVER claim message was sent/delivered automatically
                !lower.contains("message sent") &&
                    !lower.contains("delivered successfully") &&
                    !lower.contains("has been sent")
            }
            is ToolResult.NeedsUserAction -> {
                !lower.contains("toggled bluetooth on") &&
                    !lower.contains("turned on wifi")
            }
            is ToolResult.Unsupported, is ToolResult.Failed -> {
                !lower.contains("succeeded") && !lower.contains("completed successfully")
            }
        }
    }
}
