package com.jarvis.assistant.security

object ApiKeyValidator {
    fun validate(raw: String): Result<String> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("API key cannot be blank."))
        if (trimmed.length > 512) return Result.failure(IllegalArgumentException("API key exceeds 512 characters."))
        if (trimmed.any { it.isWhitespace() || it.isISOControl() }) {
            return Result.failure(IllegalArgumentException("API key must not contain whitespace or control characters."))
        }
        return Result.success(trimmed)
    }
}
