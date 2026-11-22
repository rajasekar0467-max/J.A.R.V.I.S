package com.jarvis.assistant.ai

import java.net.URI

object UrlValidator {

    fun validate(raw: String, allowCleartext: Boolean = false): Result<String> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Base URL cannot be empty."))
        }
        val uri = runCatching { URI(trimmed) }.getOrElse {
            return Result.failure(IllegalArgumentException("Malformed URL."))
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "https" && !(allowCleartext && scheme == "http")) {
            return Result.failure(
                IllegalArgumentException(
                    if (scheme == "http") "HTTPS is required for release endpoints."
                    else "URL must start with https://"
                )
            )
        }
        val host = uri.host?.lowercase()?.trim()
        if (host.isNullOrEmpty()) {
            return Result.failure(IllegalArgumentException("URL must include a valid host."))
        }
        if (!allowCleartext && isLoopbackOrLinkLocal(host)) {
            return Result.failure(IllegalArgumentException("Localhost endpoints are only allowed in debug builds."))
        }
        val normalized = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        return Result.success(normalized)
    }

    private fun isLoopbackOrLinkLocal(host: String): Boolean {
        return host == "localhost" ||
            host == "127.0.0.1" ||
            host == "::1" ||
            host == "0.0.0.0" ||
            host.startsWith("169.254.")
    }
}
