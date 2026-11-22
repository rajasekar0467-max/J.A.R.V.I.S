package com.jarvis.assistant.ai

data class ProviderPreset(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val defaultModel: String,
    val description: String
)

object ProviderPresets {
    val GEMINI = ProviderPreset(
        id = "gemini",
        displayName = "Google Gemini (OpenAI-Compatible)",
        baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/",
        defaultModel = "gemini-2.0-flash",
        description = "Fast multimodal and multilingual replies via Gemini OpenAI compatibility endpoint."
    )

    val OPENAI = ProviderPreset(
        id = "openai",
        displayName = "OpenAI",
        baseUrl = "https://api.openai.com/v1/",
        defaultModel = "gpt-4o-mini",
        description = "OpenAI Chat Completions API."
    )

    val GROQ = ProviderPreset(
        id = "groq",
        displayName = "Groq",
        baseUrl = "https://api.groq.com/openai/v1/",
        defaultModel = "llama-3.3-70b-versatile",
        description = "Low-latency OpenAI-compatible inference."
    )

    val OPENROUTER = ProviderPreset(
        id = "openrouter",
        displayName = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1/",
        defaultModel = "google/gemini-2.0-flash-001",
        description = "Unified gateway across multiple model providers."
    )

    val CUSTOM = ProviderPreset(
        id = "custom",
        displayName = "Custom OpenAI-Compatible",
        baseUrl = "https://api.openai.com/v1/",
        defaultModel = "gpt-4o-mini",
        description = "Enter any HTTPS OpenAI-compatible /chat/completions base URL."
    )

    val ALL: List<ProviderPreset> = listOf(GEMINI, OPENAI, GROQ, OPENROUTER, CUSTOM)

    fun byId(id: String): ProviderPreset = ALL.firstOrNull { it.id == id } ?: CUSTOM
}
