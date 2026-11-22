package com.jarvis.assistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.conversation.Conversation
import com.jarvis.assistant.conversation.ConversationRepository
import com.jarvis.assistant.conversation.ExportManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val conversationRepository: ConversationRepository,
    private val exportManager: ExportManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val conversations: StateFlow<List<Conversation>> = _searchQuery
        .flatMapLatest { q -> conversationRepository.observeConversations(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearStatus() {
        _statusMessage.value = null
    }

    fun rename(id: Long, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            conversationRepository.renameConversation(id, trimmed)
            _statusMessage.value = "Conversation renamed."
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(id)
            _statusMessage.value = "Conversation deleted."
        }
    }

    fun exportConversation(id: Long) {
        viewModelScope.launch {
            val text = conversationRepository.exportConversation(id)
            if (text.isBlank()) {
                _statusMessage.value = "Nothing to export."
                return@launch
            }
            val res = exportManager.shareTextExport("jarvis_conversation_$id.txt", text)
            _statusMessage.value = if (res.isSuccess) "Share sheet opened." else "Could not share export."
        }
    }

    fun exportAll() {
        viewModelScope.launch {
            val text = conversationRepository.exportAll()
            if (text.isBlank()) {
                _statusMessage.value = "No conversations to export."
                return@launch
            }
            val res = exportManager.shareTextExport("jarvis_all_conversations.txt", text)
            _statusMessage.value = if (res.isSuccess) "Exported all conversations." else "Could not share export."
        }
    }
}
