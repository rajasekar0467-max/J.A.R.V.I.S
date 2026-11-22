package com.jarvis.assistant

import com.jarvis.assistant.core.ErrorLog
import com.jarvis.assistant.core.KeyValueStore
import com.jarvis.assistant.core.StartupTracker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupTrackerTest {

    private class FakeStore : KeyValueStore {
        private val bools = mutableMapOf<String, Boolean>()
        private val strings = mutableMapOf<String, String>()
        override fun getBoolean(key: String, default: Boolean): Boolean = bools[key] ?: default
        override fun putBoolean(key: String, value: Boolean) { bools[key] = value }
        override fun getString(key: String): String? = strings[key]
        override fun putString(key: String, value: String) { strings[key] = value }
        override fun clear() { bools.clear(); strings.clear() }
    }

    @Test
    fun detectsIncompletePreviousLaunchAndTracksSteps() = runBlocking {
        val store = FakeStore()
        val tracker1 = StartupTracker(store, ErrorLog())
        assertFalse(tracker1.begin()) // clean first launch
        // Simulate crash before markFinished()

        val tracker2 = StartupTracker(store, ErrorLog())
        assertTrue(tracker2.begin()) // previous launch did not finish
        tracker2.step("DB") { "OK" }
        val report = tracker2.buildReport(previousCrash = null)
        tracker2.markFinished()

        assertTrue(report.previousLaunchIncomplete)
        assertTrue(report.allOk)
    }
}
