package com.jarvis.assistant.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.jarvis.assistant.core.ErrorLog
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface ApiKeyStore {
    fun hasKey(): Boolean
    fun getKey(): String?
    fun saveKey(key: String): Result<Unit>
    fun clearKey()
    fun probeStatus(): String
}

class SecretStore(
    context: Context,
    private val errorLog: ErrorLog
) : ApiKeyStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun hasKey(): Boolean = prefs.contains(KEY_ENTRY)

    override fun getKey(): String? {
        val combined = prefs.getString(KEY_ENTRY, null) ?: return null
        val parts = combined.split(":", limit = 2)
        if (parts.size != 2) {
            clearEntryOnly()
            return null
        }
        return runCatching {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrElse {
            errorLog.record("SecretStore", "Decrypt failed; cleared corrupted key entry", it)
            clearEntryOnly()
            null
        }
    }

    override fun saveKey(key: String): Result<Unit> {
        val valid = ApiKeyValidator.validate(key).getOrElse { return Result.failure(it) }
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
            val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val ct = Base64.encodeToString(cipher.doFinal(valid.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
            prefs.edit().putString(KEY_ENTRY, "$iv:$ct").apply()
        }.onFailure {
            errorLog.record("SecretStore", "Failed to encrypt and save API key", it)
        }
    }

    override fun clearKey() {
        clearEntryOnly()
        runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS)
        }.onFailure {
            errorLog.record("SecretStore", "Failed to delete Keystore alias", it)
        }
    }

    override fun probeStatus(): String {
        return runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val hasAlias = ks.containsAlias(KEY_ALIAS)
            "AndroidKeyStore OK (aliasPresent=$hasAlias, keyStored=${hasKey()})"
        }.getOrElse {
            errorLog.record("SecretStore", "Keystore probe failed", it)
            "Keystore probe error: ${it.javaClass.simpleName}"
        }
    }

    private fun clearEntryOnly() {
        prefs.edit().remove(KEY_ENTRY).apply()
    }

    @Synchronized
    private fun getOrCreateMasterKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        gen.init(spec)
        return gen.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "jarvis_api_key_v1"
        private const val PREFS_NAME = "jarvis_secret_store"
        private const val KEY_ENTRY = "encrypted_api_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
