package com.jarvis.assistant.core

import java.util.ArrayDeque

data class ErrorEntry(
    val timestampMs: Long,
    val tag: String,
    val message: String
)

class ErrorLog(
    private val maxEntries: Int = 50,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val entries = ArrayDeque<ErrorEntry>(maxEntries)

    @Synchronized
    fun record(tag: String, message: String, throwable: Throwable? = null) {
        val raw = buildString {
            append(message)
            if (throwable != null) {
                append(" (")
                append(throwable.javaClass.simpleName)
                val tm = throwable.message
                if (!tm.isNullOrBlank()) {
                    append(": ")
                    append(tm)
                }
                append(")")
            }
        }
        if (entries.size >= maxEntries) {
            entries.removeFirst()
        }
        entries.addLast(ErrorEntry(clock(), tag, Redactor.redact(raw)))
    }

    @Synchronized
    fun snapshot(): List<ErrorEntry> = entries.toList()

    @Synchronized
    fun clear() {
        entries.clear()
    }
}
