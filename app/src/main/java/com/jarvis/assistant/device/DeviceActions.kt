package com.jarvis.assistant.device

data class CommandExecutionResult(
    val handled: Boolean,
    val reply: String,
    val actionTaken: Boolean = false
)

interface DeviceActions {
    suspend fun execute(command: Command): CommandExecutionResult
}
