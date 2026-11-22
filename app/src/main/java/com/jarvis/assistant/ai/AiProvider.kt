package com.jarvis.assistant.ai

interface AiProvider {
    suspend fun streamReply(
        request: AiRequest,
        onDelta: suspend (String) -> Unit
    ): AiResult

    suspend fun diagnose(
        baseUrl: String,
        apiKey: String,
        model: String
    ): AiDiagnostics
}
