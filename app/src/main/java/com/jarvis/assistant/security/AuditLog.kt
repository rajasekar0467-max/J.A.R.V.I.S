package com.jarvis.assistant.security

import android.content.Context
import com.jarvis.assistant.core.SafeLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuditLog(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_audit_log", Context.MODE_PRIVATE)

    fun recordEvent(action: String, detail: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val safeDetail = SafeLogger.redactSecrets(detail)
        val entry = "[$timestamp] $action: $safeDetail"
        val existing = getRecentEvents().toMutableList()
        existing.add(0, entry)
        val capped = existing.take(50)
        prefs.edit().putString("entries", capped.joinToString("\n")).apply()
    }

    fun getRecentEvents(): List<String> {
        val raw = prefs.getString("entries", "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split("\n")
    }

    fun clearAllLogs() {
        prefs.edit().clear().apply()
    }
}
