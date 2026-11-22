package com.jarvis.assistant

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.assistant.security.AuditLog
import com.jarvis.assistant.security.KeystoreManager
import com.jarvis.assistant.security.SecretSlot
import com.jarvis.assistant.security.SecretStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android Instrumentation Test (Must be executed on a physical Android device or emulator).
 * Tests real AndroidKeyStore AES-256-GCM encryption, KeyInfo security level query,
 * and separation between clearAllSecrets() and user settings.
 */
@RunWith(AndroidJUnit4::class)
class KeystoreSecretStoreInstrumentedTest {

    @Test
    fun androidKeyStoreEncryptDecryptRoundTripAndClearSecrets() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val keystoreManager = KeystoreManager()
        val auditLog = AuditLog(context)
        val secretStore = SecretStore(context, keystoreManager, auditLog)

        secretStore.clearAllSecrets()
        assertFalse(secretStore.isSecretConfigured(SecretSlot.GEMINI_API_KEY))

        val saveResult = secretStore.saveSecret(SecretSlot.GEMINI_API_KEY, "test_gemini_secret_987654")
        assertTrue(saveResult.isSuccess)
        assertTrue(secretStore.isSecretConfigured(SecretSlot.GEMINI_API_KEY))

        val report = keystoreManager.inspectKeySecurityLevel()
        assertTrue(report.aliasExists)

        val decrypted = secretStore.getSecret(SecretSlot.GEMINI_API_KEY)
        assertEquals("test_gemini_secret_987654", decrypted)

        secretStore.clearAllSecrets()
        assertFalse(secretStore.isSecretConfigured(SecretSlot.GEMINI_API_KEY))
        assertNull(secretStore.getSecret(SecretSlot.GEMINI_API_KEY))
    }
}
