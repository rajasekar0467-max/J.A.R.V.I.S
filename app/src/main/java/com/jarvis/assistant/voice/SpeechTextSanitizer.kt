package com.jarvis.assistant.voice

object SpeechTextSanitizer {

    private val codeBlockRegex = Regex("""```[\s\S]*?```""")
    private val inlineCodeRegex = Regex("""`([^`]+)`""")
    private val linkRegex = Regex("""\[([^\]]+)\]\([^)]+\)""")
    private val markdownCharsRegex = Regex("""[*_~#>]+""")
    private val multiSpaceRegex = Regex("""\s+""")

    fun sanitize(raw: String): String {
        if (raw.isBlank()) return ""
        var text = codeBlockRegex.replace(raw, " Code block omitted. ")
        text = linkRegex.replace(text, "$1")
        text = inlineCodeRegex.replace(text, "$1")
        text = markdownCharsRegex.replace(text, " ")
        text = multiSpaceRegex.replace(text, " ").trim()
        return text
    }

    fun chunkForTts(raw: String, maxChunkChars: Int = 340): List<String> {
        val clean = sanitize(raw)
        if (clean.isEmpty()) return emptyList()
        if (clean.length <= maxChunkChars) return listOf(clean)

        val sentences = clean.split(Regex("""(?<=[.!?।])\s+"""))
        val chunks = mutableListOf<String>()
        val current = StringBuilder()

        for (sentence in sentences) {
            val s = sentence.trim()
            if (s.isEmpty()) continue
            if (s.length > maxChunkChars) {
                if (current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                    current.clear()
                }
                var idx = 0
                while (idx < s.length) {
                    val end = (idx + maxChunkChars).coerceAtMost(s.length)
                    chunks.add(s.substring(idx, end).trim())
                    idx = end
                }
            } else if (current.length + s.length + 1 > maxChunkChars) {
                chunks.add(current.toString().trim())
                current.clear()
                current.append(s)
            } else {
                if (current.isNotEmpty()) current.append(' ')
                current.append(s)
            }
        }
        if (current.isNotEmpty()) {
            chunks.add(current.toString().trim())
        }
        return chunks
    }
}
