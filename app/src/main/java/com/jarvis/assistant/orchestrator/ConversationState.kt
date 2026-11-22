package com.jarvis.assistant.orchestrator

/**
 * Deliverable 4 Conversation State Machine enum.
 * Active state machine transitions connect in Phase 3 (Live Voice).
 */
enum class ConversationState {
    IDLE,
    WAKE_LISTENING,
    LISTENING,
    TRANSCRIBING,
    THINKING,
    TOOL_RUNNING,
    SPEAKING
}
