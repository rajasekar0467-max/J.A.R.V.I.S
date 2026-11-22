package com.jarvis.assistant.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface SseEvent {
    data class Delta(val text: String) : SseEvent
    data object Done : SseEvent
    data class Error(val message: String) : SseEvent
}

object SseParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parseLine(rawLine: String): SseEvent? {
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith(":")) return null
        val payload = when {
            line.startsWith("data:") -> line.removePrefix("data:").trim()
            else -> return null
        }
        if (payload.isEmpty()) return null
        if (payload == "[DONE]") return SseEvent.Done

        return runCatching {
            val root = json.parseToJsonElement(payload).jsonObject
            val errObj = root["error"]
            if (errObj != null) {
                val msg = when (errObj) {
                    is JsonObject -> errObj["message"]?.jsonPrimitive?.contentOrNull ?: "Provider error"
                    is JsonPrimitive -> errObj.contentOrNull ?: "Provider error"
                    else -> "Provider error"
                }
                return@runCatching SseEvent.Error(msg)
            }
            val choices = root["choices"] as? JsonArray ?: return@runCatching null
            if (choices.isEmpty()) return@runCatching null
            val first = choices[0].jsonObject
            val delta = first["delta"] as? JsonObject
            val content = delta?.get("content")?.jsonPrimitive?.contentOrNull
                ?: (first["message"] as? JsonObject)?.get("content")?.jsonPrimitive?.contentOrNull
            if (!content.isNullOrEmpty()) {
                SseEvent.Delta(content)
            } else {
                null
            }
        }.getOrElse {
            SseEvent.Error("Malformed stream chunk.")
        }
    }

    fun parseNonStreamingBody(body: String): String? {
        return runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            val choices = root["choices"]?.jsonArray ?: return@runCatching null
            if (choices.isEmpty()) return@runCatching null
            val message = choices[0].jsonObject["message"]?.jsonObject
            message?.get("content")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }
}
