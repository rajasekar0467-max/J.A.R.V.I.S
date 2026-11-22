package com.jarvis.assistant.security

import android.content.Context
import android.util.Base64

enum class SecretSlot(val storageKey: String, val displayName: String, val phaseUsed: String) {
    GEMINI_API_KEY("gemini_api_key", "Google Gemini API Key", "Phase 2 (Primary AI Brain)"),
    OPENAI_API_KEY("openai_api_key", "OpenAI API Key", "Phase 1/2 (Fallback Brain & Whisper)"),
    ANTHROPIC_API_KEY("anthropic_api_key", "Anthropic Claude API Key", "Phase 2 (Fallback Brain)"),
    GROQ_API_KEY("groq_api_key", "Groq API Key", "Phase 2 (Fast Low-Latency Turns)"),
    AZURE_SPEECH_KEY("azure_speech_key", "Azure Speech Key (ta-IN / en-IN)", "Phase 1 (Streaming ASR & Neural TTS)"),
    AZURE_SPEECH_REGION("azure_speech_region", "Azure Speech Region (e.g. centralindia)", "Phase 1 (Streaming ASR & Neural TTS)"),
    SEARCH_API_KEY("search_api_key", "Web Search API Key (Brave / Tavily)", "Phase 5 (WebSearchTool)"),
    PORCUPINE_ACCESS_KEY("porcupine_access_key", "Picovoice Porcupine AccessKey", "Phase 7 ('Hey JARVIS' Wake Word)")
}

object SecretSlotValidator {
    fun validateFormat(slot: SecretSlot, rawInput: String): Result<String> {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("${slot.displayName} cannot be empty or whitespace."))
        }
        if (trimmed.length < 4) {
            return Result.failure(IllegalArgumentException("${slot.displayName} is too short to be valid."))
        }
        if (trimmed.contains("\n") || trimmed.contains("\r")) {
            return Result.failure(IllegalArgumentException("${slot.displayName} must not contain newline characters."))
        }
        return Result.success(trimmed)
    }
}

/**
 * Stores user-entered API keys encrypted via KeystoreManager (AES-256-GCM) in a dedicated
 * private SharedPreferences vault (`jarvis_secrets_vault`).
 * Strictly separated from general app settings and full app-data reset.
 */
class SecretStore(
    context: Context,
    private val keystoreManager: KeystoreManager,
    private val auditLog: AuditLog
) {
    private val prefs = context.getSharedPreferences(VAULT_PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val VAULT_PREFS_NAME = "jarvis_secrets_vault"
    }

    fun saveSecret(slot: SecretSlot, rawValue: String): Result<Unit> {
        val validated = SecretSlotValidator.validateFormat(slot, rawValue).getOrElse {
            return Result.failure(it)
        }
        return try {
            val payload = keystoreManager.encrypt(validated.toByteArray(Charsets.UTF_8))
            val encodedIv = Base64.encodeToString(payload.iv, Base64.NO_WRAP)
            val encodedCipher = Base64.encodeToString(payload.ciphertext, Base64.NO_WRAP)
            prefs.edit()
                .putString(slot.storageKey, "$encodedIv:$encodedCipher")
                .apply()
            auditLog.recordEvent("SECRET_SAVED", "Saved encrypted key for slot ${slot.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSecret(slot: SecretSlot): String? {
        val combined = prefs.getString(slot.storageKey, null) ?: return null
        val parts = combined.split(":", limit = 2)
        if (parts.size != 2) return null
        return try {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipher = Base64.decode(parts[1], Base64.NO_WRAP)
            val decrypted = keystoreManager.decrypt(KeystoreManager.EncryptedPayload(iv, cipher))
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun isSecretConfigured(slot: SecretSlot): Boolean {
        return prefs.contains(slot.storageKey)
    }

    fun deleteSecret(slot: SecretSlot) {
        prefs.edit().remove(slot.storageKey).apply()
        auditLog.recordEvent("SECRET_DELETED", "Deleted secret slot ${slot.name}")
    }

    /**
     * Clears ONLY stored API secrets and rotates/deletes the master keystore key.
     * Preserves non-secret application settings and audit logs.
     */
    fun clearAllSecrets() {
        prefs.edit().clear().apply()
        keystoreManager.deleteMasterKey()
        auditLog.recordEvent("ALL_SECRETS_CLEARED", "Cleared all API secrets and deleted master Keystore alias")
    }
}
