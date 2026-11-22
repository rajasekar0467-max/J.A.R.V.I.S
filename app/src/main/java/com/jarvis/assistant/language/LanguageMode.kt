package com.jarvis.assistant.language

enum class LanguageMode(val label: String, val asrTag: String) {
    EN_IN("English (India)", "en-IN"),
    TA_IN("Tamil (தமிழ்)", "ta-IN"),
    TANGLISH("Tanglish", "en-IN");

    companion object {
        fun fromNameSafe(name: String?): LanguageMode =
            entries.firstOrNull { it.name == name } ?: EN_IN
    }
}
