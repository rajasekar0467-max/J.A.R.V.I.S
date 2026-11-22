package com.jarvis.assistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.ai.AiEngine
import com.jarvis.assistant.ai.AiRequest
import com.jarvis.assistant.ai.AiResult
import com.jarvis.assistant.ai.PromptBuilder
import com.jarvis.assistant.conversation.Conversation
import com.jarvis.assistant.conversation.ConversationRepository
import com.jarvis.assistant.conversation.Message
import com.jarvis.assistant.core.ErrorLog
import com.jarvis.assistant.core.NetworkState
import com.jarvis.assistant.device.Command
import com.jarvis.assistant.device.CommandRouter
import com.jarvis.assistant.device.DeviceActions
import com.jarvis.assistant.language.LanguageDetector
import com.jarvis.assistant.language.LanguageMode
import com.jarvis.assistant.memory.MemoryRepository
import com.jarvis.assistant.security.ApiKeyStore
import com.jarvis.assistant.settings.AppSettings
import com.jarvis.assistant.settings.SettingsSource
import com.jarvis.assistant.voice.SpeechInput
import com.jarvis.assistant.voice.SpeechInputCallbacks
import com.jarvis.assistant.voice.SpeechOutput
import com.jarvis.assistant.voice.VoiceSnapshot
import com.jarvis.assistant.voice.VoiceState
import com.jarvis.assistant.voice.VoiceStateMachine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AssistantUiState(
    val activeConversationId: Long? = null,
    val isStreaming: Boolean = false,
    val streamingDraft: String = "",
    val statusBanner: String? = null,
    val hasApiKey: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModel(
    private val conversationRepository: ConversationRepository,
    private val memoryRepository: MemoryRepository,
    private val settingsSource: SettingsSource,
    private val keyStore: ApiKeyStore,
    private val aiEngine: AiEngine,
    private val deviceActions: DeviceActions,
    private val voiceStateMachine: VoiceStateMachine,
    private val speechInput: SpeechInput,
    private val speechOutput: SpeechOutput,
    val networkState: StateFlow<NetworkState>,
    private val errorLog: ErrorLog
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState(hasApiKey = keyStore.hasKey()))
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsSource.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val recentConversations: StateFlow<List<Conversation>> = conversationRepository
        .observeConversations("")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val activeIdFlow = MutableStateFlow<Long?>(null)

    val messages: StateFlow<List<Message>> = activeIdFlow
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else conversationRepository.observeMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val voiceSnapshot: StateFlow<VoiceSnapshot> = voiceStateMachine.snapshot

    private var activeGenerationJob: Job? = null

    fun refreshKeyStatus() {
        _uiState.value = _uiState.value.copy(hasApiKey = keyStore.hasKey())
    }

    fun dismissBanner() {
        _uiState.value = _uiState.value.copy(statusBanner = null)
    }

    fun startNewConversation() {
        stopGeneration()
        speechOutput.stop()
        activeIdFlow.value = null
        _uiState.value = _uiState.value.copy(
            activeConversationId = null,
            streamingDraft = "",
            statusBanner = null
        )
    }

    fun openConversation(conversationId: Long) {
        stopGeneration()
        activeIdFlow.value = conversationId
        _uiState.value = _uiState.value.copy(
            activeConversationId = conversationId,
            streamingDraft = "",
            statusBanner = null
        )
    }

    fun setLanguageMode(mode: LanguageMode) {
        viewModelScope.launch {
            settingsSource.update { it.copy(language = mode) }
        }
    }

    fun sendMessage(rawText: String, fromVoice: Boolean = false) {
        val text = rawText.trim()
        if (text.isEmpty() || _uiState.value.isStreaming) return

        speechOutput.stop()
        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            try {
                val convId = ensureConversation(text)
                conversationRepository.addMessage(convId, "user", text, if (fromVoice) "voice" else "user")

                val currentSettings = settings.value
                val replyLang = LanguageDetector.effectiveReplyLanguage(text, currentSettings.language)

                val command = CommandRouter.parse(text)
                if (command != Command.None) {
                    val exec = deviceActions.execute(command)
                    if (exec.handled) {
                        conversationRepository.addMessage(convId, "assistant", exec.reply, "local")
                        if (fromVoice || currentSettings.autoSpeak) {
                            speakInternal(exec.reply, replyLang, currentSettings)
                        } else {
                            voiceStateMachine.resetToIdle()
                        }
                        return@launch
                    }
                }

                val apiKey = keyStore.getKey().orEmpty()
                if (apiKey.isBlank()) {
                    val msg = "No API key configured yet. Open Settings to add your API key, or try local commands like \"What time is it?\" or \"Help\"."
                    conversationRepository.addMessage(convId, "assistant", msg, "local")
                    _uiState.value = _uiState.value.copy(hasApiKey = false, statusBanner = "API key required for AI replies.")
                    if (fromVoice) speakInternal(msg, replyLang, currentSettings) else voiceStateMachine.resetToIdle()
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isStreaming = true,
                    streamingDraft = "",
                    statusBanner = null,
                    hasApiKey = true
                )
                voiceStateMachine.onProcessing()

                val history = conversationRepository.getMessages(convId)
                val facts = memoryRepository.getFacts()
                val promptMessages = PromptBuilder.buildMessages(
                    history = history,
                    memoryFacts = facts,
                    languageMode = replyLang,
                    maxContextMessages = currentSettings.maxContextMessages
                )

                val assistantMsgId = conversationRepository.addMessage(convId, "assistant", "", "ai")
                val buffer = StringBuilder()

                val result = aiEngine.generateReply(
                    request = AiRequest(
                        baseUrl = currentSettings.baseUrl,
                        apiKey = apiKey,
                        model = currentSettings.model,
                        messages = promptMessages,
                        temperature = currentSettings.temperature
                    )
                ) { delta ->
                    buffer.append(delta)
                    _uiState.value = _uiState.value.copy(streamingDraft = buffer.toString())
                }

                when (result) {
                    is AiResult.Success -> {
                        conversationRepository.updateMessage(assistantMsgId, result.fullText)
                        _uiState.value = _uiState.value.copy(isStreaming = false, streamingDraft = "")
                        if (fromVoice || currentSettings.autoSpeak) {
                            speakInternal(result.fullText, replyLang, currentSettings)
                        } else {
                            voiceStateMachine.resetToIdle()
                        }
                    }
                    is AiResult.Failure -> {
                        val partial = buffer.toString().trim()
                        if (partial.isNotEmpty()) {
                            conversationRepository.updateMessage(assistantMsgId, partial)
                        } else {
                            conversationRepository.deleteMessage(assistantMsgId)
                        }
                        _uiState.value = _uiState.value.copy(
                            isStreaming = false,
                            streamingDraft = "",
                            statusBanner = result.userMessage
                        )
                        voiceStateMachine.onError(result.userMessage)
                    }
                }
            } catch (ce: CancellationException) {
                _uiState.value = _uiState.value.copy(isStreaming = false, streamingDraft = "")
                voiceStateMachine.resetToIdle()
                throw ce
            } catch (t: Throwable) {
                errorLog.record("AssistantVM", "Failed handling message", t)
                _uiState.value = _uiState.value.copy(
                    isStreaming = false,
                    streamingDraft = "",
                    statusBanner = "Something went wrong processing that request."
                )
                voiceStateMachine.onError("Something went wrong processing that request.")
            }
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _uiState.value = _uiState.value.copy(isStreaming = false, streamingDraft = "")
        if (voiceStateMachine.snapshot.value.state == VoiceState.PROCESSING) {
            voiceStateMachine.resetToIdle()
        }
    }

    fun startVoiceListening() {
        speechOutput.stop()
        val currentSettings = settings.value
        voiceStateMachine.startListening(currentSettings.language)
        speechInput.startListening(
            languageTag = currentSettings.language.asrTag,
            timeoutSec = currentSettings.listenTimeoutSec,
            callbacks = object : SpeechInputCallbacks {
                override fun onReady() = Unit

                override fun onRmsChanged(rmsDb: Float) {
                    voiceStateMachine.onRmsChanged(rmsDb)
                }

                override fun onPartial(text: String) {
                    voiceStateMachine.onPartialTranscript(text)
                }

                override fun onFinal(text: String) {
                    voiceStateMachine.onFinalTranscript(text)
                    sendMessage(text, fromVoice = true)
                }

                override fun onError(code: Int, message: String) {
                    voiceStateMachine.onError(message)
                }
            }
        )
    }

    fun stopVoiceListening() {
        speechInput.stopListening()
    }

    fun cancelVoice() {
        speechInput.cancel()
        speechOutput.stop()
        voiceStateMachine.resetToIdle()
    }

    fun speakMessage(text: String) {
        val currentSettings = settings.value
        val lang = LanguageDetector.detect(text, currentSettings.language)
        speakInternal(text, lang, currentSettings)
    }

    fun stopSpeaking() {
        speechOutput.stop()
        voiceStateMachine.onSpeakingFinished()
    }

    private fun speakInternal(text: String, language: LanguageMode, appSettings: AppSettings) {
        if (!speechOutput.isReady()) {
            voiceStateMachine.resetToIdle()
            return
        }
        speechOutput.speak(
            text = text,
            language = language,
            speechRate = appSettings.speechRate,
            pitch = appSettings.pitch,
            preferredVoiceName = appSettings.ttsVoiceName,
            onStart = { voiceStateMachine.onSpeakingStarted() },
            onDone = { voiceStateMachine.onSpeakingFinished() },
            onError = { msg ->
                errorLog.record("VoiceTTS", msg)
                voiceStateMachine.resetToIdle()
            }
        )
    }

    private suspend fun ensureConversation(seedText: String): Long {
        val existing = activeIdFlow.value
        if (existing != null) return existing
        val title = seedText.take(42).ifBlank { "New Conversation" }
        val newId = conversationRepository.createConversation(title)
        activeIdFlow.value = newId
        _uiState.value = _uiState.value.copy(activeConversationId = newId)
        return newId
    }

    override fun onCleared() {
        super.onCleared()
        activeGenerationJob?.cancel()
        speechInput.cancel()
    }
}
