package com.jarvis.assistant.device

sealed interface Command {
    data object Time : Command
    data object Date : Command
    data class OpenApp(val appQuery: String) : Command
    data class SetReminder(val text: String, val delayMinutes: Int) : Command
    data object ListReminders : Command
    data class SetTimer(val seconds: Int, val label: String) : Command
    data class OpenSettings(val section: String) : Command
    data class RememberFact(val fact: String) : Command
    data object ListMemory : Command
    data object Help : Command
    data object None : Command
}
