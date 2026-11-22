package com.jarvis.assistant.language

object LanguageDetector {

    private val tanglishTokens = setOf(
        "vanakkam", "enna", "epdi", "eppadi", "irukkinga", "iruka", "iruku", "irukku",
        "machan", "macha", "nanba", "thala", "nalla", "sollu", "sollunga", "panna",
        "pannunga", "venum", "vendam", "illai", "illa", "seri", "sari", "romba",
        "konjam", "vaanga", "ponga", "kudunga", "theriyala", "puriyala", "aama",
        "ama", "inga", "anga", "ippo", "appo", "naalai", "innaiku", "naan", "nee",
        "neenga", "enakku", "unakku", "ungalukku", "nandri", "paaru", "paarunga",
        "kelu", "kelunga", "vachiko", "mudiyuma", "podhum", "thambi", "anna", "akka"
    )

    fun detect(text: String, fallback: LanguageMode = LanguageMode.EN_IN): LanguageMode {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return fallback

        var tamilChars = 0
        var latinChars = 0
        for (ch in trimmed) {
            when {
                ch in '\u0B80'..'\u0BFF' -> tamilChars++
                ch in 'a'..'z' || ch in 'A'..'Z' -> latinChars++
            }
        }
        if (tamilChars >= 2 && tamilChars * 3 >= (tamilChars + latinChars)) {
            return LanguageMode.TA_IN
        }
        if (tamilChars >= 1 && latinChars > 0) {
            return LanguageMode.TANGLISH
        }

        val words = trimmed.lowercase()
            .split(Regex("[^a-z]+"))
            .filter { it.isNotEmpty() }
        if (words.isEmpty()) return fallback

        val hits = words.count { it in tanglishTokens }
        if (hits >= 2 || (hits == 1 && words.size <= 4)) {
            return LanguageMode.TANGLISH
        }
        return if (fallback == LanguageMode.TA_IN && latinChars > 0) {
            LanguageMode.EN_IN
        } else {
            fallback
        }
    }

    fun effectiveReplyLanguage(userText: String, selected: LanguageMode): LanguageMode {
        val detected = detect(userText, selected)
        return when {
            detected == LanguageMode.TA_IN -> LanguageMode.TA_IN
            detected == LanguageMode.TANGLISH -> LanguageMode.TANGLISH
            else -> selected
        }
    }
}
