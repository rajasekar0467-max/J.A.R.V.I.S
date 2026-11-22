package com.jarvis.assistant.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jarvis.assistant.language.LanguageMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.jarvisSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jarvis_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

interface SettingsSource {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
    suspend fun clear()
}

class SettingsRepository(private val context: Context) : SettingsSource {

    override val settings: Flow<AppSettings> = context.jarvisSettingsDataStore.data.map { p ->
        AppSettings(
            language = LanguageMode.fromNameSafe(p[KEY_LANG]),
            providerPresetId = p[KEY_PRESET] ?: "gemini",
            baseUrl = p[KEY_BASE_URL] ?: "https://generativelanguage.googleapis.com/v1beta/openai/",
            model = p[KEY_MODEL] ?: "gemini-2.0-flash",
            temperature = (p[KEY_TEMP] ?: 0.7f).coerceIn(0.0f, 1.5f),
            maxContextMessages = (p[KEY_MAX_CTX] ?: 20).coerceIn(4, 50),
            autoSpeak = p[KEY_AUTO_SPEAK] ?: true,
            speechRate = (p[KEY_RATE] ?: 0.95f).coerceIn(0.5f, 2.0f),
            pitch = (p[KEY_PITCH] ?: 1.0f).coerceIn(0.5f, 2.0f),
            ttsVoiceName = p[KEY_TTS_VOICE] ?: "",
            reducedMotion = p[KEY_REDUCED_MOTION] ?: false,
            listenTimeoutSec = (p[KEY_LISTEN_TIMEOUT] ?: 8).coerceIn(3, 20)
        )
    }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.jarvisSettingsDataStore.edit { p ->
            val current = AppSettings(
                language = LanguageMode.fromNameSafe(p[KEY_LANG]),
                providerPresetId = p[KEY_PRESET] ?: "gemini",
                baseUrl = p[KEY_BASE_URL] ?: "https://generativelanguage.googleapis.com/v1beta/openai/",
                model = p[KEY_MODEL] ?: "gemini-2.0-flash",
                temperature = (p[KEY_TEMP] ?: 0.7f).coerceIn(0.0f, 1.5f),
                maxContextMessages = (p[KEY_MAX_CTX] ?: 20).coerceIn(4, 50),
                autoSpeak = p[KEY_AUTO_SPEAK] ?: true,
                speechRate = (p[KEY_RATE] ?: 0.95f).coerceIn(0.5f, 2.0f),
                pitch = (p[KEY_PITCH] ?: 1.0f).coerceIn(0.5f, 2.0f),
                ttsVoiceName = p[KEY_TTS_VOICE] ?: "",
                reducedMotion = p[KEY_REDUCED_MOTION] ?: false,
                listenTimeoutSec = (p[KEY_LISTEN_TIMEOUT] ?: 8).coerceIn(3, 20)
            )
            val next = transform(current)
            p[KEY_LANG] = next.language.name
            p[KEY_PRESET] = next.providerPresetId
            p[KEY_BASE_URL] = next.baseUrl
            p[KEY_MODEL] = next.model
            p[KEY_TEMP] = next.temperature.coerceIn(0.0f, 1.5f)
            p[KEY_MAX_CTX] = next.maxContextMessages.coerceIn(4, 50)
            p[KEY_AUTO_SPEAK] = next.autoSpeak
            p[KEY_RATE] = next.speechRate.coerceIn(0.5f, 2.0f)
            p[KEY_PITCH] = next.pitch.coerceIn(0.5f, 2.0f)
            p[KEY_TTS_VOICE] = next.ttsVoiceName
            p[KEY_REDUCED_MOTION] = next.reducedMotion
            p[KEY_LISTEN_TIMEOUT] = next.listenTimeoutSec.coerceIn(3, 20)
        }
    }

    override suspend fun clear() {
        context.jarvisSettingsDataStore.edit { it.clear() }
    }

    companion object {
        private val KEY_LANG = stringPreferencesKey("language")
        private val KEY_PRESET = stringPreferencesKey("provider_preset_id")
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_MODEL = stringPreferencesKey("model")
        private val KEY_TEMP = floatPreferencesKey("temperature")
        private val KEY_MAX_CTX = intPreferencesKey("max_context_messages")
        private val KEY_AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        private val KEY_RATE = floatPreferencesKey("speech_rate")
        private val KEY_PITCH = floatPreferencesKey("pitch")
        private val KEY_TTS_VOICE = stringPreferencesKey("tts_voice_name")
        private val KEY_REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        private val KEY_LISTEN_TIMEOUT = intPreferencesKey("listen_timeout_sec")
    }
}
