package com.jarvis.assistant.ai

data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class AiRequest(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f
)

enum class AiErrorKind {
    NO_API_KEY,
    INVALID_URL,
    UNAUTHORIZED,
    NOT_FOUND,
    RATE_LIMITED,
    SERVER_ERROR,
    NETWORK_OFFLINE,
    TIMEOUT,
    CANCELLED,
    PARSE_ERROR,
    UNKNOWN
}

sealed interface AiResult {
    data class Success(
        val fullText: String,
        val latencyMs: Long
    ) : AiResult

    data class Failure(
        val kind: AiErrorKind,
        val userMessage: String,
        val httpCode: Int? = null
    ) : AiResult
}

data class AiDiagnostics(
    val reachable: Boolean,
    val httpCode: Int?,
    val latencyMs: Long,
    val detail: String
)
