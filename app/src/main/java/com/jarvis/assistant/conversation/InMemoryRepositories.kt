package com.jarvis.assistant.conversation

import com.jarvis.assistant.device.ReminderRepository
import com.jarvis.assistant.memory.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

class InMemoryConversationRepository(
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ConversationRepository {
    private val nextConvId = AtomicLong(1L)
    private val nextMsgId = AtomicLong(1L)
    private val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val messages = MutableStateFlow<List<Message>>(emptyList())

    override fun observeConversations(query: String): Flow<List<Conversation>> {
        val q = query.trim().lowercase()
        return conversations.map { list ->
            val allMsgs = messages.value
            list.filter { c ->
                q.isEmpty() ||
                    c.title.lowercase().contains(q) ||
                    allMsgs.any { it.conversationId == c.id && it.content.lowercase().contains(q) }
            }.sortedByDescending { it.updatedAt }
        }
    }

    override fun observeMessages(conversationId: Long): Flow<List<Message>> =
        messages.map { list -> list.filter { it.conversationId == conversationId }.sortedWith(compareBy({ it.createdAt }, { it.id })) }

    override suspend fun getMessages(conversationId: Long): List<Message> =
        messages.value.filter { it.conversationId == conversationId }.sortedWith(compareBy({ it.createdAt }, { it.id }))

    override suspend fun createConversation(title: String): Long {
        val now = clock()
        val id = nextConvId.getAndIncrement()
        conversations.value = conversations.value + Conversation(id, title.ifBlank { "New Chat" }, now, now)
        return id
    }

    override suspend fun renameConversation(id: Long, title: String) {
        val now = clock()
        conversations.value = conversations.value.map {
            if (it.id == id) it.copy(title = title.trim().ifBlank { "Untitled" }, updatedAt = now) else it
        }
    }

    override suspend fun deleteConversation(id: Long) {
        conversations.value = conversations.value.filterNot { it.id == id }
        messages.value = messages.value.filterNot { it.conversationId == id }
    }

    override suspend fun addMessage(conversationId: Long, role: String, content: String, source: String): Long {
        val now = clock()
        val id = nextMsgId.getAndIncrement()
        messages.value = messages.value + Message(id, conversationId, role, content, now, source)
        conversations.value = conversations.value.map {
            if (it.id == conversationId) it.copy(updatedAt = now) else it
        }
        return id
    }

    override suspend fun updateMessage(id: Long, content: String) {
        messages.value = messages.value.map { if (it.id == id) it.copy(content = content) else it }
    }

    override suspend fun deleteMessage(id: Long) {
        messages.value = messages.value.filterNot { it.id == id }
    }

    override suspend fun exportConversation(id: Long): String {
        val conv = conversations.value.firstOrNull { it.id == id } ?: return "Conversation not found."
        return ConversationExporter.format(conv, getMessages(id))
    }

    override suspend fun exportAll(): String {
        val convs = conversations.value.sortedByDescending { it.updatedAt }
        if (convs.isEmpty()) return "No conversations to export."
        return convs.joinToString("\n========================================\n\n") { conv ->
            ConversationExporter.format(conv, messages.value.filter { it.conversationId == conv.id })
        }
    }

    override suspend fun clearAll() {
        conversations.value = emptyList()
        messages.value = emptyList()
    }
}

class InMemoryMemoryRepository(
    private val clock: () -> Long = { System.currentTimeMillis() }
) : MemoryRepository {
    private val nextId = AtomicLong(1L)
    private val facts = MutableStateFlow<List<MemoryFact>>(emptyList())

    override fun observeFacts(): Flow<List<MemoryFact>> = facts
    override suspend fun getFacts(): List<MemoryFact> = facts.value
    override suspend fun addFact(text: String): Long {
        val id = nextId.getAndIncrement()
        facts.value = listOf(MemoryFact(id, text.trim(), clock())) + facts.value
        return id
    }
    override suspend fun deleteFact(id: Long) { facts.value = facts.value.filterNot { it.id == id } }
    override suspend fun clear() { facts.value = emptyList() }
}

class InMemoryReminderRepository(
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ReminderRepository {
    private val nextId = AtomicLong(1L)
    private val items = MutableStateFlow<List<Reminder>>(emptyList())

    override suspend fun add(text: String, triggerAt: Long): Long {
        val id = nextId.getAndIncrement()
        items.value = (items.value + Reminder(id, text.trim(), triggerAt, false, clock())).sortedBy { it.triggerAt }
        return id
    }
    override fun observeAll(): Flow<List<Reminder>> = items
    override suspend fun pending(now: Long): List<Reminder> = items.value.filter { !it.fired && it.triggerAt > now }
    override suspend fun markFired(id: Long) {
        items.value = items.value.map { if (it.id == id) it.copy(fired = true) else it }
    }
    override suspend fun delete(id: Long) { items.value = items.value.filterNot { it.id == id } }
    override suspend fun clear() { items.value = emptyList() }
}
