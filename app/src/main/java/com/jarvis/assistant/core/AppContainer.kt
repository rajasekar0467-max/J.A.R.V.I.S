package com.jarvis.assistant.core

import android.content.Context
import com.jarvis.assistant.BuildConfig
import com.jarvis.assistant.ai.AiEngine
import com.jarvis.assistant.ai.AiProvider
import com.jarvis.assistant.ai.OpenAiCompatibleProvider
import com.jarvis.assistant.conversation.ConversationRepository
import com.jarvis.assistant.conversation.DatabaseProvider
import com.jarvis.assistant.conversation.ExportManager
import com.jarvis.assistant.conversation.InMemoryConversationRepository
import com.jarvis.assistant.conversation.InMemoryMemoryRepository
import com.jarvis.assistant.conversation.InMemoryReminderRepository
import com.jarvis.assistant.conversation.RoomConversationRepository
import com.jarvis.assistant.device.AndroidDeviceActions
import com.jarvis.assistant.device.DeviceActions
import com.jarvis.assistant.device.NotificationHelper
import com.jarvis.assistant.device.ReminderAlarms
import com.jarvis.assistant.device.ReminderRepository
import com.jarvis.assistant.device.RoomReminderRepository
import com.jarvis.assistant.diagnostics.DiagnosticsCollector
import com.jarvis.assistant.memory.MemoryRepository
import com.jarvis.assistant.memory.RoomMemoryRepository
import com.jarvis.assistant.security.ApiKeyStore
import com.jarvis.assistant.security.LegacyApiKeyStore
import com.jarvis.assistant.settings.SettingsRepository
import com.jarvis.assistant.settings.SettingsSource
import com.jarvis.assistant.voice.SpeechInput
import com.jarvis.assistant.voice.SpeechOutput
import com.jarvis.assistant.voice.SpeechRecognizerController
import com.jarvis.assistant.voice.TtsController
import com.jarvis.assistant.voice.VoiceStateMachine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Manual dependency injection container for JARVIS.
 * No Hilt/Dagger/Koin code generation or reflection overhead.
 */
class AppContainer(
    private val appContext: Context,
    val crashLogger: CrashLogger?
) {

    val errorLog = ErrorLog()
    val startupTracker = StartupTracker(SharedPrefsKeyValueStore(appContext), errorLog)
    val networkMonitor = NetworkMonitor(appContext, errorLog)
    val secretStore: ApiKeyStore = LegacyApiKeyStore(appContext, errorLog)
    val settingsRepository: SettingsSource = SettingsRepository(appContext)
    val exportManager = ExportManager(appContext, errorLog)

    @Volatile
    var storageMode: String = "Initializing"
        private set

    private var _conversationRepository: ConversationRepository? = null
    val conversationRepository: ConversationRepository
        get() = _conversationRepository ?: synchronized(this) {
            _conversationRepository ?: initStorage().let { _conversationRepository!! }
        }

    private var _memoryRepository: MemoryRepository? = null
    val memoryRepository: MemoryRepository
        get() = _memoryRepository ?: synchronized(this) {
            _memoryRepository ?: initStorage().let { _memoryRepository!! }
        }

    private var _reminderRepository: ReminderRepository? = null
    val reminderRepository: ReminderRepository
        get() = _reminderRepository ?: synchronized(this) {
            _reminderRepository ?: initStorage().let { _reminderRepository!! }
        }

    val aiProvider: AiProvider = OpenAiCompatibleProvider(
        errorLog = errorLog,
        allowCleartext = BuildConfig.DEBUG
    )
    val aiEngine = AiEngine(aiProvider)

    val voiceStateMachine = VoiceStateMachine()
    val speechInput: SpeechInput = SpeechRecognizerController(appContext, errorLog)
    val speechOutput: SpeechOutput = TtsController(appContext, errorLog)

    val reminderAlarms = ReminderAlarms(appContext, errorLog)

    val deviceActions: DeviceActions by lazy {
        AndroidDeviceActions(
            context = appContext,
            reminderRepository = reminderRepository,
            reminderAlarms = reminderAlarms,
            memoryRepository = memoryRepository,
            errorLog = errorLog
        )
    }

    val appDataManager: AppDataManager by lazy {
        AppDataManager(
            context = appContext,
            keyStore = secretStore,
            conversationRepository = conversationRepository,
            memoryRepository = memoryRepository,
            reminderRepository = reminderRepository,
            settingsSource = settingsRepository,
            startupTracker = startupTracker,
            crashLogger = crashLogger,
            errorLog = errorLog
        )
    }

    private val _startupReport = MutableStateFlow<StartupReport?>(null)
    val startupReport: StateFlow<StartupReport?> = _startupReport.asStateFlow()

    private val _initialized = MutableStateFlow(false)
    val initialized: StateFlow<Boolean> = _initialized.asStateFlow()

    val diagnosticsCollector: DiagnosticsCollector by lazy {
        DiagnosticsCollector(
            context = appContext,
            storageModeProvider = { storageMode },
            keyStore = secretStore,
            settingsSource = settingsRepository,
            networkMonitor = networkMonitor,
            speechInput = speechInput,
            speechOutput = speechOutput,
            aiEngine = aiEngine,
            startupReportProvider = { _startupReport.value },
            crashLogger = crashLogger,
            errorLog = errorLog
        )
    }

    private fun initStorage() {
        runCatching {
            val db = DatabaseProvider.get(appContext)
            _conversationRepository = RoomConversationRepository(db.conversationDao(), db.messageDao())
            _memoryRepository = RoomMemoryRepository(db.memoryFactDao())
            _reminderRepository = RoomReminderRepository(db.reminderDao())
            storageMode = "Room (SQLite)"
        }.onFailure { err ->
            errorLog.record("Storage", "Room init failed; falling back to in-memory storage", err)
            _conversationRepository = InMemoryConversationRepository()
            _memoryRepository = InMemoryMemoryRepository()
            _reminderRepository = InMemoryReminderRepository()
            storageMode = "In-Memory Fallback"
        }
    }

    suspend fun initialize(): StartupReport = withContext(Dispatchers.IO) {
        if (_initialized.value && _startupReport.value != null) {
            return@withContext _startupReport.value!!
        }

        startupTracker.begin()
        startupTracker.step("NotificationChannel") {
            NotificationHelper.ensureChannel(appContext)
        }
        startupTracker.step("Database") {
            synchronized(this@AppContainer) {
                if (_conversationRepository == null) initStorage()
            }
            storageMode
        }
        startupTracker.step("Settings") {
            settingsRepository.settings.first()
        }
        startupTracker.step("KeyStore") {
            secretStore.probeStatus()
        }
        startupTracker.step("NetworkMonitor") {
            networkMonitor.start()
        }

        val report = startupTracker.buildReport(crashLogger?.lastCrash())
        startupTracker.markFinished()
        _startupReport.value = report
        _initialized.value = true
        report
    }
}
