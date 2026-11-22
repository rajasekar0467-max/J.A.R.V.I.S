package com.jarvis.assistant.conversation

import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun observeConversations(query: String = ""): Flow<List<Conversation>>
    fun observeMessages(conversationId: Long): Flow<List<Message>>
    suspend fun getMessages(conversationId: Long): List<Message>
    suspend fun createConversation(title: String): Long
    suspend fun renameConversation(id: Long, title: String)
    suspend fun deleteConversation(id: Long)
    suspend fun addMessage(conversationId: Long, role: String, content: String, source: String): Long
    suspend fun updateMessage(id: Long, content: String)
    suspend fun deleteMessage(id: Long)
    suspend fun exportConversation(id: Long): String
    suspend fun exportAll(): String
    suspend fun clearAll()
}
