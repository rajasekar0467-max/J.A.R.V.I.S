package com.jarvis.assistant.core

/**
 * Deliverable 8 Rule #3 & #4:
 * No logging of audio buffers or secrets. Redacts common API key patterns automatically.
 */
object SafeLogger {
    private val SECRET_PATTERNS = listOf(
        Regex("AIza[0-9A-Za-z\\-_]{20,}"),
        Regex("sk-[0-9A-Za-z\\-_]{16,}"),
        Regex("gsk_[0-9A-Za-z\\-_]{16,}"),
        Regex("(?i)bearer\\s+[0-9A-Za-z\\-._~+/]+=*")
    )

    fun redactSecrets(input: String): String {
        var sanitized = input
        for (pattern in SECRET_PATTERNS) {
            sanitized = pattern.replace(sanitized, "[REDACTED_SECRET]")
        }
        return sanitized
    }
}
