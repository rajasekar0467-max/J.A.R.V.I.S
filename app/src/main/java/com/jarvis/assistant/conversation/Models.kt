package com.jarvis.assistant.conversation

data class Conversation(
    val id: Long,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class Message(
    val id: Long,
    val conversationId: Long,
    val role: String, // "user" or "assistant"
    val content: String,
    val createdAt: Long,
    val source: String // "ai" or "local"
)

data class MemoryFact(
    val id: Long,
    val text: String,
    val createdAt: Long
)

data class Reminder(
    val id: Long,
    val text: String,
    val triggerAt: Long,
    val fired: Boolean,
    val createdAt: Long
)
