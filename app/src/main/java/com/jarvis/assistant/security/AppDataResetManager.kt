package com.jarvis.assistant.security

import android.content.Context
import com.jarvis.assistant.settings.SettingsRepository
import java.io.File

/**
 * Mandatory Correction #2:
 * Uses a supported Android app-data reset approach (clearing secret vault, deleting Keystore alias,
 * clearing all SharedPreferences/DataStore files, deleting local databases, and clearing internal
 * files/cache directories) instead of ActivityManager.clearApplicationUserData(), keeping
 * "Clear Stored Secrets" strictly separate from "Full Application Data Reset".
 */
class AppDataResetManager(
    private val context: Context,
    private val secretStore: SecretStore,
    private val settingsRepository: SettingsRepository,
    private val auditLog: AuditLog
) {
    fun performFullApplicationDataReset(): Boolean {
        return try {
            // 1. Wipe all encrypted secrets & delete AndroidKeyStore key alias
            secretStore.clearAllSecrets()

            // 2. Reset all user preferences
            settingsRepository.resetAllSettings()

            // 3. Clear audit log
            auditLog.clearAllLogs()

            // 4. Delete any local SQLite databases
            for (dbName in context.databaseList()) {
                context.deleteDatabase(dbName)
            }

            // 5. Clear internal filesDir and cacheDir contents
            deleteDirectoryContents(context.filesDir)
            deleteDirectoryContents(context.cacheDir)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun deleteDirectoryContents(dir: File?) {
        if (dir == null || !dir.exists()) return
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                deleteDirectoryContents(child)
            }
            child.delete()
        }
    }
}
