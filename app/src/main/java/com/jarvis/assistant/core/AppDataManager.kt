package com.jarvis.assistant.core

import android.content.Context
import com.jarvis.assistant.conversation.ConversationRepository
import com.jarvis.assistant.device.ReminderRepository
import com.jarvis.assistant.memory.MemoryRepository
import com.jarvis.assistant.security.ApiKeyStore
import com.jarvis.assistant.settings.SettingsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Supported Android app-data reset manager.
 * Keeps Clear Stored Secrets strictly separate from Clear All App Data,
 * and never terminates the process via ActivityManager process-wipe APIs.
 */
class AppDataManager(
    private val context: Context,
    private val keyStore: ApiKeyStore,
    private val conversationRepository: ConversationRepository,
    private val memoryRepository: MemoryRepository,
    private val reminderRepository: ReminderRepository,
    private val settingsSource: SettingsSource,
    private val startupTracker: StartupTracker,
    private val crashLogger: CrashLogger?,
    private val errorLog: ErrorLog
) {

    fun clearStoredSecrets() {
        keyStore.clearKey()
    }

    suspend fun clearAllAppData(alsoClearSecrets: Boolean = false) = withContext(Dispatchers.IO) {
        runCatching { conversationRepository.clearAll() }
        runCatching { memoryRepository.clear() }
        runCatching { reminderRepository.clear() }
        runCatching { settingsSource.clear() }
        runCatching { startupTracker.clear() }
        runCatching { crashLogger?.clear() }
        runCatching { errorLog.clear() }
        runCatching { File(context.cacheDir, "exports").deleteRecursively() }
        if (alsoClearSecrets) {
            runCatching { keyStore.clearKey() }
        }
    }
}
