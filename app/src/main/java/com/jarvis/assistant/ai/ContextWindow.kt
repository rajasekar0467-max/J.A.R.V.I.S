package com.jarvis.assistant.ai

import com.jarvis.assistant.conversation.Message

object ContextWindow {

    fun selectRecent(
        history: List<Message>,
        maxMessages: Int,
        maxTotalChars: Int = 14_000
    ): List<ChatMessage> {
        if (history.isEmpty() || maxMessages <= 0) return emptyList()
        val tail = history.takeLast(maxMessages.coerceIn(2, 60))
        val selected = ArrayList<ChatMessage>(tail.size)
        var chars = 0
        for (i in tail.lastIndex downTo 0) {
            val m = tail[i]
            val content = m.content.trim()
            if (content.isEmpty()) continue
            if (selected.isNotEmpty() && chars + content.length > maxTotalChars) {
                break
            }
            val role = if (m.role == "assistant") "assistant" else "user"
            selected.add(ChatMessage(role, content))
            chars += content.length
        }
        return selected.asReversed()
    }
}
