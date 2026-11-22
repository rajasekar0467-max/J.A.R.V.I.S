package com.jarvis.assistant.core

object Redactor {
    private val patterns = listOf(
        Regex("""sk-[A-Za-z0-9_\-]{8,}"""),
        Regex("""AIza[A-Za-z0-9_\-]{16,}"""),
        Regex("""gsk_[A-Za-z0-9_\-]{8,}"""),
        Regex("""(?i)Bearer\s+[A-Za-z0-9\-._~+/]+=*"""),
        Regex("""(?i)(api[_\-]?key|authorization|token)\s*[:=]\s*[^\s,;"']+""")
    )

    fun redact(text: String?): String {
        var out: String = text ?: return ""
        for (regex in patterns) {
            out = regex.replace(out, "[REDACTED]")
        }
        return out
    }
}
