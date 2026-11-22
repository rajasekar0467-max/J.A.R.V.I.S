package com.jarvis.assistant

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvis.assistant.core.ErrorLog
import com.jarvis.assistant.security.LegacyApiKeyStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecretStoreInstrumentedTest {

    @Test
    fun roundTripsEncryptedKeyAndClearsSeparately() {
        val store = LegacyApiKeyStore(ApplicationProvider.getApplicationContext(), ErrorLog())
        store.clearKey()
        assertFalse(store.hasKey())

        val save = store.saveKey("JARVIS_FAKE_TEST_KEY_123456")
        assertTrue(save.isSuccess)
        assertTrue(store.hasKey())
        assertEquals("JARVIS_FAKE_TEST_KEY_123456", store.getKey())

        store.clearKey()
        assertFalse(store.hasKey())
    }
}
