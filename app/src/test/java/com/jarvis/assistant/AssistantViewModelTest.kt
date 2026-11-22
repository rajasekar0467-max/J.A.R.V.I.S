package com.jarvis.assistant

import com.jarvis.assistant.ai.AiDiagnostics
import com.jarvis.assistant.ai.AiEngine
import com.jarvis.assistant.ai.AiProvider
import com.jarvis.assistant.ai.AiRequest
import com.jarvis.assistant.ai.AiResult
import com.jarvis.assistant.conversation.InMemoryConversationRepository
import com.jarvis.assistant.conversation.InMemoryMemoryRepository
import com.jarvis.assistant.core.ErrorLog
import com.jarvis.assistant.core.NetworkState
import com.jarvis.assistant.device.Command
import com.jarvis.assistant.device.CommandExecutionResult
import com.jarvis.assistant.device.DeviceActions
import com.jarvis.assistant.language.LanguageMode
import com.jarvis.assistant.security.ApiKeyStore
import com.jarvis.assistant.settings.AppSettings
import com.jarvis.assistant.settings.SettingsSource
import com.jarvis.assistant.ui.AssistantViewModel
import com.jarvis.assistant.voice.SpeechInput
import com.jarvis.assistant.voice.SpeechInputCallbacks
import com.jarvis.assistant.voice.SpeechOutput
import com.jarvis.assistant.voice.VoiceStateMachine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun routesLocalCommandWithoutCallingAiProvider() = runTest {
        val convRepo = InMemoryConversationRepository()
        val memRepo = InMemoryMemoryRepository()
        val settingsFlow = MutableStateFlow(AppSettings(autoSpeak = false))
        val settingsSource = object : SettingsSource {
            override val settings: Flow<AppSettings> = settingsFlow
            override suspend fun update(transform: (AppSettings) -> AppSettings) {
                settingsFlow.value = transform(settingsFlow.value)
            }
            override suspend fun clear() { settingsFlow.value = AppSettings() }
        }
        val keyStore = object : ApiKeyStore {
            override fun hasKey() = false
            override fun getKey(): String? = null
            override fun saveKey(key: String) = Result.success(Unit)
            override fun clearKey() = Unit
            override fun probeStatus() = "Test"
        }
        val aiProvider = object : AiProvider {
            override suspend fun streamReply(request: AiRequest, onDelta: suspend (String) -> Unit): AiResult {
                throw AssertionError("AI provider should not be called for local time command")
            }
            override suspend fun diagnose(baseUrl: String, apiKey: String, model: String) =
                AiDiagnostics(true, 200, 1L, "OK")
        }
        val actions = object : DeviceActions {
            override suspend fun execute(command: Command): CommandExecutionResult =
                if (command == Command.Time) CommandExecutionResult(true, "It's 10:00 AM.", true)
                else CommandExecutionResult(false, "")
        }
        val speechIn = object : SpeechInput {
            override fun isAvailable() = false
            override fun startListening(languageTag: String, timeoutSec: Int, callbacks: SpeechInputCallbacks) = Unit
            override fun stopListening() = Unit
            override fun cancel() = Unit
            override fun destroy() = Unit
        }
        val speechOut = object : SpeechOutput {
            override fun isReady() = false
            override fun availableVoiceNames() = emptyList<String>()
            override fun speak(
                text: String,
                language: LanguageMode,
                speechRate: Float,
                pitch: Float,
                preferredVoiceName: String,
                onStart: () -> Unit,
                onDone: () -> Unit,
                onError: (String) -> Unit
            ) = Unit
            override fun stop() = Unit
            override fun shutdown() = Unit
        }

        val vm = AssistantViewModel(
            conversationRepository = convRepo,
            memoryRepository = memRepo,
            settingsSource = settingsSource,
            keyStore = keyStore,
            aiEngine = AiEngine(aiProvider),
            deviceActions = actions,
            voiceStateMachine = VoiceStateMachine(),
            speechInput = speechIn,
            speechOutput = speechOut,
            networkState = MutableStateFlow(NetworkState(connected = true, validated = true, transport = "Wi-Fi")),
            errorLog = ErrorLog()
        )

        vm.sendMessage("What time is it?")
        advanceUntilIdle()

        val convId = vm.uiState.value.activeConversationId
        assertTrue(convId != null)
        val saved = convRepo.getMessages(convId!!)
        assertEquals(2, saved.size)
        assertEquals("It's 10:00 AM.", saved[1].content)
    }
}
