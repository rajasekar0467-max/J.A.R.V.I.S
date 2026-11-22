package com.jarvis.assistant.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the AES-256-GCM master key in AndroidKeyStore and queries the actual
 * runtime security level reported by the device (never assuming hardware-backed without verification).
 */
class KeystoreManager {

    companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val MASTER_KEY_ALIAS = "jarvis_secret_store_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }

    data class EncryptedPayload(
        val iv: ByteArray,
        val ciphertext: ByteArray
    )

    data class KeySecurityReport(
        val aliasExists: Boolean,
        val algorithm: String,
        val securityLevelLabel: String,
        val rawSecurityLevelCode: Int
    )

    @Synchronized
    fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) {
            return existing.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun inspectKeySecurityLevel(): KeySecurityReport {
        return try {
            val secretKey = getOrCreateSecretKey()
            val factory = SecretKeyFactory.getInstance(secretKey.algorithm, ANDROID_KEYSTORE)
            val keyInfo = factory.getKeySpec(secretKey, KeyInfo::class.java) as KeyInfo

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val level = keyInfo.securityLevel
                val label = when (level) {
                    KeyProperties.SECURITY_LEVEL_STRONGBOX ->
                        "SECURITY_LEVEL_STRONGBOX (Hardware StrongBox)"
                    KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT ->
                        "SECURITY_LEVEL_TRUSTED_ENVIRONMENT (Hardware TEE)"
                    KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE ->
                        "SECURITY_LEVEL_UNKNOWN_SECURE"
                    KeyProperties.SECURITY_LEVEL_SOFTWARE ->
                        "SECURITY_LEVEL_SOFTWARE (Software Keystore)"
                    else -> "SECURITY_LEVEL_OTHER ($level)"
                }
                KeySecurityReport(true, secretKey.algorithm, label, level)
            } else {
                @Suppress("DEPRECATION")
                val inHardware = keyInfo.isInsideSecureHardware
                KeySecurityReport(
                    true,
                    secretKey.algorithm,
                    if (inHardware) "INSIDE_SECURE_HARDWARE (Legacy API <31)" else "SOFTWARE_ONLY (Legacy API <31)",
                    if (inHardware) 1 else 0
                )
            }
        } catch (e: Exception) {
            KeySecurityReport(false, "AES", "INSPECTION_ERROR: ${e.javaClass.simpleName}", -1)
        }
    }

    fun encrypt(plaintext: ByteArray): EncryptedPayload {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedPayload(iv = iv, ciphertext = ciphertext)
    }

    fun decrypt(payload: EncryptedPayload): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_BITS, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
        return cipher.doFinal(payload.ciphertext)
    }

    @Synchronized
    fun deleteMasterKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            keyStore.deleteEntry(MASTER_KEY_ALIAS)
        }
    }
}
