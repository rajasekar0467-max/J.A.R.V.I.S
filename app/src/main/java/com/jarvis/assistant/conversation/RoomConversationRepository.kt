package com.jarvis.assistant.conversation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomConversationRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ConversationRepository {

    override fun observeConversations(query: String): Flow<List<Conversation>> =
        conversationDao.observeConversations(query.trim()).map { list -> list.map { it.toModel() } }

    override fun observeMessages(conversationId: Long): Flow<List<Message>> =
        messageDao.observeMessages(conversationId).map { list -> list.map { it.toModel() } }

    override suspend fun getMessages(conversationId: Long): List<Message> =
        messageDao.getMessages(conversationId).map { it.toModel() }

    override suspend fun createConversation(title: String): Long {
        val now = clock()
        return conversationDao.insertConversation(
            ConversationEntity(title = title.ifBlank { "New Chat" }, createdAt = now, updatedAt = now)
        )
    }

    override suspend fun renameConversation(id: Long, title: String) {
        conversationDao.renameConversation(id, title.trim().ifBlank { "Untitled" }, clock())
    }

    override suspend fun deleteConversation(id: Long) {
        conversationDao.deleteConversation(id)
    }

    override suspend fun addMessage(conversationId: Long, role: String, content: String, source: String): Long {
        val now = clock()
        val id = messageDao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = role,
                content = content,
                createdAt = now,
                source = source
            )
        )
        conversationDao.touchConversation(conversationId, now)
        return id
    }

    override suspend fun updateMessage(id: Long, content: String) {
        messageDao.updateMessage(id, content)
    }

    override suspend fun deleteMessage(id: Long) {
        messageDao.deleteMessage(id)
    }

    override suspend fun exportConversation(id: Long): String {
        val conv = conversationDao.getConversation(id)?.toModel() ?: return "Conversation not found."
        val msgs = getMessages(id)
        return ConversationExporter.format(conv, msgs)
    }

    override suspend fun exportAll(): String {
        val convs = conversationDao.getAllConversations().map { it.toModel() }
        if (convs.isEmpty()) return "No conversations to export."
        val exports = mutableListOf<String>()
        for (conv in convs) {
            val msgs = messageDao.getMessages(conv.id).map { it.toModel() }
            exports += ConversationExporter.format(conv, msgs)
        }
        return exports.joinToString("\n========================================\n\n")
    }

    override suspend fun clearAll() {
        conversationDao.clearAllConversations()
    }

    private fun ConversationEntity.toModel() = Conversation(id, title, createdAt, updatedAt)
    private fun MessageEntity.toModel() = Message(id, conversationId, role, content, createdAt, source)
}
