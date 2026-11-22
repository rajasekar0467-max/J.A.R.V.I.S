package com.jarvis.assistant.device

object CommandRouter {

    private val reminderInRegex = Regex(
        """(?:remind me in|set (?:a )?reminder in)\s+(\d+)\s*(min|mins|minute|minutes|hr|hrs|hour|hours)\s*(?:to\s+)?(.+)""",
        RegexOption.IGNORE_CASE
    )
    private val reminderSimpleRegex = Regex(
        """remind me to\s+(.+?)\s+in\s+(\d+)\s*(min|mins|minute|minutes|hr|hrs|hour|hours)""",
        RegexOption.IGNORE_CASE
    )
    private val timerRegex = Regex(
        """(?:set (?:a )?timer(?: for)?|timer)\s+(\d+)\s*(sec|secs|second|seconds|min|mins|minute|minutes|hr|hrs|hour|hours)(?:\s+(?:for|named)\s+(.+))?""",
        RegexOption.IGNORE_CASE
    )
    private val openAppRegex = Regex("""^(?:open|launch|start)\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val rememberRegex = Regex("""^(?:remember that|remember|save memory|note that)\s+(.+)$""", RegexOption.IGNORE_CASE)

    fun parse(rawInput: String): Command {
        val text = rawInput.trim()
        if (text.isEmpty()) return Command.None
        val lower = text.lowercase()

        if (lower in setOf("help", "commands", "what can you do", "enna panna mudiyum")) {
            return Command.Help
        }

        if (lower.matches(Regex("""^(?:what(?:'s| is) the time|current time|time now|time enna|ippo time enna)[?]?$"""))) {
            return Command.Time
        }

        if (lower.matches(Regex("""^(?:what(?:'s| is) (?:the |today's )?date|today's date|date enna|innaiku date enna)[?]?$"""))) {
            return Command.Date
        }

        if (lower.matches(Regex("""^(?:show|list|my|check)\s+reminders?[?]?$""")) || lower == "reminders") {
            return Command.ListReminders
        }

        if (lower.matches(Regex("""^(?:show|list|my|what do you)\s+(?:memory|memories|remember)[?]?$"""))) {
            return Command.ListMemory
        }

        reminderInRegex.find(text)?.let { m ->
            val amount = m.groupValues[1].toIntOrNull()?.coerceIn(1, 1440) ?: return@let
            val unit = m.groupValues[2].lowercase()
            val task = m.groupValues[3].trim()
            if (task.isNotEmpty()) {
                val mins = if (unit.startsWith("h")) (amount * 60).coerceAtMost(1440) else amount
                return Command.SetReminder(task, mins)
            }
        }

        reminderSimpleRegex.find(text)?.let { m ->
            val task = m.groupValues[1].trim()
            val amount = m.groupValues[2].toIntOrNull()?.coerceIn(1, 1440) ?: return@let
            val unit = m.groupValues[3].lowercase()
            if (task.isNotEmpty()) {
                val mins = if (unit.startsWith("h")) (amount * 60).coerceAtMost(1440) else amount
                return Command.SetReminder(task, mins)
            }
        }

        timerRegex.find(text)?.let { m ->
            val amount = m.groupValues[1].toIntOrNull()?.coerceIn(1, 86400) ?: return@let
            val unit = m.groupValues[2].lowercase()
            val label = m.groupValues.getOrElse(3) { "" }.trim().ifEmpty { "JARVIS Timer" }
            val seconds = when {
                unit.startsWith("h") -> (amount * 3600).coerceAtMost(86400)
                unit.startsWith("m") -> (amount * 60).coerceAtMost(86400)
                else -> amount
            }
            return Command.SetTimer(seconds, label)
        }

        rememberRegex.find(text)?.let { m ->
            val fact = m.groupValues[1].trim()
            if (fact.length >= 3) {
                return Command.RememberFact(fact)
            }
        }

        openAppRegex.find(text)?.let { m ->
            val target = m.groupValues[1].trim()
            val targetLower = target.lowercase()
            return when {
                targetLower.contains("wifi") || targetLower.contains("wi-fi") -> Command.OpenSettings("wifi")
                targetLower.contains("bluetooth") -> Command.OpenSettings("bluetooth")
                targetLower.contains("display") || targetLower.contains("brightness") -> Command.OpenSettings("display")
                targetLower.contains("sound") || targetLower.contains("volume") -> Command.OpenSettings("sound")
                targetLower == "settings" || targetLower == "device settings" -> Command.OpenSettings("main")
                else -> Command.OpenApp(target)
            }
        }

        return Command.None
    }
}
