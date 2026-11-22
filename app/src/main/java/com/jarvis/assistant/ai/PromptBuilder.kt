package com.jarvis.assistant.ai

import com.jarvis.assistant.conversation.MemoryFact
import com.jarvis.assistant.conversation.Message
import com.jarvis.assistant.language.LanguageMode

object PromptBuilder {

    fun buildMessages(
        history: List<Message>,
        memoryFacts: List<MemoryFact>,
        languageMode: LanguageMode,
        maxContextMessages: Int
    ): List<ChatMessage> {
        val systemPrompt = buildSystemPrompt(languageMode, memoryFacts)
        val recent = ContextWindow.selectRecent(history, maxContextMessages)
        return buildList(recent.size + 1) {
            add(ChatMessage("system", systemPrompt))
            addAll(recent)
        }
    }

    fun buildSystemPrompt(languageMode: LanguageMode, memoryFacts: List<MemoryFact>): String {
        val langInstruction = when (languageMode) {
            LanguageMode.EN_IN ->
                "Respond in clear, natural Indian English. Keep answers concise, warm, and ready to be spoken aloud."
            LanguageMode.TA_IN ->
                "Respond in natural, polite Tamil (தமிழ்) using Tamil script. Keep phrasing conversational and easy to listen to."
            LanguageMode.TANGLISH ->
                "Respond in natural Tanglish (conversational Tamil written in English letters mixed with everyday English). Keep it friendly, clear, and concise."
        }

        val memoryBlock = if (memoryFacts.isEmpty()) {
            ""
        } else {
            val lines = memoryFacts.take(15).joinToString("\n") { "- ${it.text}" }
            "\n\nUser-saved memory facts (use only when relevant):\n$lines"
        }

        return buildString {
            append("You are JARVIS, a calm, sharp, capable personal AI assistant on Android with a warm, composed FRIDAY-inspired conversational cadence. ")
            append("Be direct, accurate, and helpful without filler or theatrical roleplay. ")
            append("Never claim you executed a device action unless the user confirms it. ")
            append(langInstruction)
            append(memoryBlock)
        }
    }
}
