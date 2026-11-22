package com.jarvis.assistant.ai

import com.jarvis.assistant.core.ErrorLog
import com.jarvis.assistant.core.Redactor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class OpenAiCompatibleProvider(
    private val errorLog: ErrorLog,
    private val allowCleartext: Boolean = false,
    private val client: OkHttpClient = defaultClient()
) : AiProvider {

    override suspend fun streamReply(
        request: AiRequest,
        onDelta: suspend (String) -> Unit
    ): AiResult = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        if (request.apiKey.isBlank()) {
            return@withContext AiResult.Failure(
                kind = AiErrorKind.NO_API_KEY,
                userMessage = "API key is not configured. Open Settings to add your API key."
            )
        }
        val baseUrl = UrlValidator.validate(request.baseUrl, allowCleartext).getOrElse { err ->
            return@withContext AiResult.Failure(
                kind = AiErrorKind.INVALID_URL,
                userMessage = err.message ?: "Invalid AI endpoint URL."
            )
        }

        val endpoint = "${baseUrl}chat/completions"
        val payload = buildJsonObject {
            put("model", request.model.ifBlank { "gemini-2.0-flash" })
            put("stream", true)
            put("temperature", request.temperature.toDouble().coerceIn(0.0, 2.0))
            put("messages", buildJsonArray {
                request.messages.forEach { msg ->
                    add(buildJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            })
        }.toString()

        val httpReq = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer ${request.apiKey}")
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val call = client.newCall(httpReq)
        try {
            call.execute().use { response ->
                val code = response.code
                if (!response.isSuccessful) {
                    val errBody = runCatching { response.body?.string().orEmpty().take(300) }.getOrDefault("")
                    errorLog.record("AI", "HTTP $code from provider: ${Redactor.redact(errBody)}")
                    return@withContext mapHttpFailure(code)
                }

                val body = response.body ?: return@withContext AiResult.Failure(
                    kind = AiErrorKind.PARSE_ERROR,
                    userMessage = "Empty response from AI provider.",
                    httpCode = code
                )

                val contentType = response.header("Content-Type").orEmpty().lowercase()
                if (contentType.contains("application/json")) {
                    val raw = body.string()
                    val text = SseParser.parseNonStreamingBody(raw)
                    return@withContext if (!text.isNullOrEmpty()) {
                        onDelta(text)
                        AiResult.Success(text, System.currentTimeMillis() - startMs)
                    } else {
                        AiResult.Failure(AiErrorKind.PARSE_ERROR, "Could not read provider response.", code)
                    }
                }

                val full = StringBuilder()
                val source = body.source()
                while (coroutineContext.isActive && !source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    when (val event = SseParser.parseLine(line)) {
                        is SseEvent.Delta -> {
                            full.append(event.text)
                            onDelta(event.text)
                        }
                        is SseEvent.Done -> break
                        is SseEvent.Error -> {
                            errorLog.record("AI", "SSE error: ${Redactor.redact(event.message)}")
                            if (full.isEmpty()) {
                                return@withContext AiResult.Failure(
                                    kind = AiErrorKind.PARSE_ERROR,
                                    userMessage = Redactor.redact(event.message),
                                    httpCode = code
                                )
                            } else {
                                break
                            }
                        }
                        null -> Unit
                    }
                }

                if (!coroutineContext.isActive) {
                    call.cancel()
                    return@withContext AiResult.Failure(AiErrorKind.CANCELLED, "Generation stopped.")
                }

                val resultText = full.toString().trim()
                if (resultText.isEmpty()) {
                    AiResult.Failure(AiErrorKind.PARSE_ERROR, "AI returned an empty reply.", code)
                } else {
                    AiResult.Success(resultText, System.currentTimeMillis() - startMs)
                }
            }
        } catch (ce: CancellationException) {
            call.cancel()
            throw ce
        } catch (ste: SocketTimeoutException) {
            errorLog.record("AI", "Request timed out", ste)
            AiResult.Failure(AiErrorKind.TIMEOUT, "AI request timed out. Please try again.")
        } catch (uhe: UnknownHostException) {
            errorLog.record("AI", "Host unreachable", uhe)
            AiResult.Failure(AiErrorKind.NETWORK_OFFLINE, "No internet connection or endpoint host unreachable.")
        } catch (ioe: IOException) {
            if (call.isCanceled()) {
                AiResult.Failure(AiErrorKind.CANCELLED, "Generation stopped.")
            } else {
                errorLog.record("AI", "Network error", ioe)
                AiResult.Failure(AiErrorKind.NETWORK_OFFLINE, "Network error while contacting AI provider.")
            }
        } catch (t: Throwable) {
            errorLog.record("AI", "Unexpected AI error", t)
            AiResult.Failure(AiErrorKind.UNKNOWN, "Unexpected error while contacting AI provider.")
        }
    }

    override suspend fun diagnose(
        baseUrl: String,
        apiKey: String,
        model: String
    ): AiDiagnostics = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        if (apiKey.isBlank()) {
            return@withContext AiDiagnostics(false, null, 0L, "No API key saved.")
        }
        val validUrl = UrlValidator.validate(baseUrl, allowCleartext).getOrElse {
            return@withContext AiDiagnostics(false, null, 0L, it.message ?: "Invalid URL.")
        }
        val req = Request.Builder()
            .url("${validUrl}models")
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()
        return@withContext try {
            client.newCall(req).execute().use { res ->
                val elapsed = System.currentTimeMillis() - startMs
                when {
                    res.isSuccessful -> AiDiagnostics(true, res.code, elapsed, "Endpoint reachable ($model).")
                    res.code == 401 || res.code == 403 -> AiDiagnostics(false, res.code, elapsed, "API key rejected (HTTP ${res.code}).")
                    res.code == 404 -> AiDiagnostics(true, res.code, elapsed, "Host reachable; /models not exposed (HTTP 404).")
                    else -> AiDiagnostics(false, res.code, elapsed, "Provider returned HTTP ${res.code}.")
                }
            }
        } catch (e: Throwable) {
            val elapsed = System.currentTimeMillis() - startMs
            AiDiagnostics(false, null, elapsed, Redactor.redact(e.message ?: e.javaClass.simpleName))
        }
    }

    private fun mapHttpFailure(code: Int): AiResult.Failure = when (code) {
        401, 403 -> AiResult.Failure(AiErrorKind.UNAUTHORIZED, "API key was rejected (HTTP $code). Check your key in Settings.", code)
        404 -> AiResult.Failure(AiErrorKind.NOT_FOUND, "Model or endpoint not found (HTTP 404). Check Base URL and Model.", code)
        429 -> AiResult.Failure(AiErrorKind.RATE_LIMITED, "Rate limit reached (HTTP 429). Wait a moment and retry.", code)
        in 500..599 -> AiResult.Failure(AiErrorKind.SERVER_ERROR, "AI provider server error (HTTP $code). Try again shortly.", code)
        else -> AiResult.Failure(AiErrorKind.UNKNOWN, "AI provider returned HTTP $code.", code)
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
