package com.jarvis.assistant.conversation

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ConversationExporter {
    fun format(conversation: Conversation, messages: List<Message>): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return buildString {
            appendLine("JARVIS Conversation Export")
            appendLine("Title: ${conversation.title}")
            appendLine("Created: ${fmt.format(Date(conversation.createdAt))}")
            appendLine("Updated: ${fmt.format(Date(conversation.updatedAt))}")
            appendLine("----------------------------------------")
            for (m in messages) {
                val speaker = if (m.role == "user") "You" else "JARVIS (${m.source})"
                appendLine("[${fmt.format(Date(m.createdAt))}] $speaker:")
                appendLine(m.content)
                appendLine()
            }
        }
    }
}
