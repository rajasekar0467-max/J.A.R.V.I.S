package com.jarvis.assistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.BuildConfig
import com.jarvis.assistant.ai.ProviderPresets
import com.jarvis.assistant.ai.UrlValidator
import com.jarvis.assistant.conversation.MemoryFact
import com.jarvis.assistant.conversation.Reminder
import com.jarvis.assistant.core.AppDataManager
import com.jarvis.assistant.device.ReminderAlarms
import com.jarvis.assistant.device.ReminderRepository
import com.jarvis.assistant.language.LanguageMode
import com.jarvis.assistant.memory.MemoryRepository
import com.jarvis.assistant.security.ApiKeyStore
import com.jarvis.assistant.security.ApiKeyValidator
import com.jarvis.assistant.settings.AppSettings
import com.jarvis.assistant.settings.SettingsSource
import com.jarvis.assistant.voice.SpeechOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsSource: SettingsSource,
    private val keyStore: ApiKeyStore,
    private val memoryRepository: MemoryRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderAlarms: ReminderAlarms,
    private val speechOutput: SpeechOutput,
    private val appDataManager: AppDataManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsSource.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val memoryFacts: StateFlow<List<MemoryFact>> = memoryRepository.observeFacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reminders: StateFlow<List<Reminder>> = reminderRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _hasApiKey = MutableStateFlow(keyStore.hasKey())
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    private val _keyStoreStatus = MutableStateFlow(keyStore.probeStatus())
    val keyStoreStatus: StateFlow<String> = _keyStoreStatus.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<String>>(emptyList())
    val availableVoices: StateFlow<List<String>> = _availableVoices.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun refreshHardwareInfo() {
        _hasApiKey.value = keyStore.hasKey()
        _keyStoreStatus.value = keyStore.probeStatus()
        _availableVoices.value = speechOutput.availableVoiceNames()
    }

    fun clearStatus() {
        _statusMessage.value = null
    }

    fun saveApiKey(rawKey: String) {
        val validated = ApiKeyValidator.validate(rawKey)
        if (validated.isFailure) {
            _statusMessage.value = validated.exceptionOrNull()?.message ?: "Invalid API key."
            return
        }
        val res = keyStore.saveKey(validated.getOrThrow())
        _hasApiKey.value = keyStore.hasKey()
        _keyStoreStatus.value = keyStore.probeStatus()
        _statusMessage.value = if (res.isSuccess) {
            "API key encrypted and saved in Android KeyStore."
        } else {
            res.exceptionOrNull()?.message ?: "Failed to save API key."
        }
    }

    fun clearStoredSecrets() {
        appDataManager.clearStoredSecrets()
        _hasApiKey.value = keyStore.hasKey()
        _statusMessage.value = "Stored API key cleared (app data preserved)."
    }

    fun clearAllAppData() {
        viewModelScope.launch {
            appDataManager.clearAllAppData(alsoClearSecrets = false)
            _statusMessage.value = "All conversations, memory, reminders, and settings cleared (stored secrets preserved)."
        }
    }

    fun applyPreset(presetId: String) {
        val preset = ProviderPresets.byId(presetId)
        viewModelScope.launch {
            settingsSource.update {
                it.copy(
                    providerPresetId = preset.id,
                    baseUrl = preset.baseUrl,
                    model = preset.defaultModel
                )
            }
            _statusMessage.value = "Applied ${preset.displayName} preset."
        }
    }

    fun updateEndpoint(baseUrl: String, model: String) {
        val validated = UrlValidator.validate(baseUrl, allowCleartext = BuildConfig.DEBUG)
        if (validated.isFailure) {
            _statusMessage.value = validated.exceptionOrNull()?.message ?: "Invalid Base URL."
            return
        }
        val cleanModel = model.trim()
        if (cleanModel.isEmpty()) {
            _statusMessage.value = "Model name cannot be empty."
            return
        }
        viewModelScope.launch {
            settingsSource.update {
                it.copy(baseUrl = validated.getOrThrow(), model = cleanModel)
            }
            _statusMessage.value = "AI endpoint updated."
        }
    }

    fun updateLanguage(mode: LanguageMode) {
        viewModelScope.launch { settingsSource.update { it.copy(language = mode) } }
    }

    fun updateAutoSpeak(enabled: Boolean) {
        viewModelScope.launch { settingsSource.update { it.copy(autoSpeak = enabled) } }
    }

    fun updateSpeechRate(rate: Float) {
        viewModelScope.launch { settingsSource.update { it.copy(speechRate = rate.coerceIn(0.6f, 1.5f)) } }
    }

    fun updatePitch(pitch: Float) {
        viewModelScope.launch { settingsSource.update { it.copy(pitch = pitch.coerceIn(0.75f, 1.35f)) } }
    }

    fun updateTtsVoiceName(voiceName: String) {
        viewModelScope.launch { settingsSource.update { it.copy(ttsVoiceName = voiceName) } }
    }

    fun updateReducedMotion(enabled: Boolean) {
        viewModelScope.launch { settingsSource.update { it.copy(reducedMotion = enabled) } }
    }

    fun updateTemperature(temp: Float) {
        viewModelScope.launch { settingsSource.update { it.copy(temperature = temp.coerceIn(0.0f, 1.5f)) } }
    }

    fun updateMaxContextMessages(count: Int) {
        viewModelScope.launch { settingsSource.update { it.copy(maxContextMessages = count.coerceIn(4, 40)) } }
    }

    fun updateListenTimeout(sec: Int) {
        viewModelScope.launch { settingsSource.update { it.copy(listenTimeoutSec = sec.coerceIn(4, 20)) } }
    }

    fun deleteMemoryFact(id: Long) {
        viewModelScope.launch { memoryRepository.deleteFact(id) }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            reminderAlarms.cancel(id)
            reminderRepository.delete(id)
        }
    }
}
