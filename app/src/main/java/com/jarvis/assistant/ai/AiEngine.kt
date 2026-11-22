package com.jarvis.assistant.ai

import kotlinx.coroutines.delay

class AiEngine(
    private val provider: AiProvider,
    private val maxRetriesBeforeFirstDelta: Int = 1,
    private val retryDelayMs: Long = 900L,
    private val sleeper: suspend (Long) -> Unit = { delay(it) }
) {

    suspend fun generateReply(
        request: AiRequest,
        onDelta: suspend (String) -> Unit
    ): AiResult {
        var attempt = 0
        while (true) {
            var emittedAnyDelta = false
            val result = provider.streamReply(request) { chunk ->
                if (chunk.isNotEmpty()) {
                    emittedAnyDelta = true
                    onDelta(chunk)
                }
            }
            if (result is AiResult.Success) return result
            val failure = result as AiResult.Failure
            val retryable = failure.kind == AiErrorKind.TIMEOUT ||
                failure.kind == AiErrorKind.SERVER_ERROR ||
                failure.kind == AiErrorKind.RATE_LIMITED
            if (!emittedAnyDelta && retryable && attempt < maxRetriesBeforeFirstDelta) {
                attempt++
                sleeper(retryDelayMs * attempt)
                continue
            }
            return failure
        }
    }

    suspend fun diagnose(baseUrl: String, apiKey: String, model: String): AiDiagnostics =
        provider.diagnose(baseUrl, apiKey, model)
}
