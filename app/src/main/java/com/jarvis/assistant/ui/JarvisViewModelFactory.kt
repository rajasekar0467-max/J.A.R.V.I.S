package com.jarvis.assistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jarvis.assistant.core.AppContainer

class JarvisViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AssistantViewModel::class.java) -> AssistantViewModel(
            conversationRepository = container.conversationRepository,
            memoryRepository = container.memoryRepository,
            settingsSource = container.settingsRepository,
            keyStore = container.secretStore,
            aiEngine = container.aiEngine,
            deviceActions = container.deviceActions,
            voiceStateMachine = container.voiceStateMachine,
            speechInput = container.speechInput,
            speechOutput = container.speechOutput,
            networkState = container.networkMonitor.state,
            errorLog = container.errorLog
        ) as T
        modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(
            conversationRepository = container.conversationRepository,
            exportManager = container.exportManager
        ) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
            settingsSource = container.settingsRepository,
            keyStore = container.secretStore,
            memoryRepository = container.memoryRepository,
            reminderRepository = container.reminderRepository,
            reminderAlarms = container.reminderAlarms,
            speechOutput = container.speechOutput,
            appDataManager = container.appDataManager
        ) as T
        modelClass.isAssignableFrom(DiagnosticsViewModel::class.java) -> DiagnosticsViewModel(
            collector = container.diagnosticsCollector,
            exportManager = container.exportManager,
            crashLogger = container.crashLogger,
            errorLog = container.errorLog
        ) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
