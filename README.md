# JARVIS — Native Android AI Assistant

**Package:** `com.jarvis.assistant`  
**Target Device:** Realme 9i 5G (`RMX3612`, Android 14 / API 34, Realme UI 5.0)  
**Architecture:** 100% Native Kotlin + Jetpack Compose + Material 3 + Room + DataStore + OkHttp SSE + Android KeyStore  
**Build & Verification Status in Sandbox:** `NOT VERIFIED` / `BLOCKED` (sandbox lacks `javac`, Android SDK, Gradle, and network).

## Key Capabilities
- **Premium Emerald-Green Interface:** Deep black & dark emerald glassmorphism, animated Canvas `JarvisOrb` (reduced-motion aware), and 5 bottom tabs (`Home`, `Chat`, `Voice`, `History`, `Settings`) plus `Diagnostics`.
- **Real OpenAI-Compatible AI Streaming:** OkHttp SSE streaming with cancellation, safe retry before first delta, provider presets (Gemini OpenAI-compatible, OpenAI, Groq, OpenRouter, Custom HTTPS), and endpoint diagnostics.
- **Single FRIDAY-Inspired Voice Profile:** Real Android `SpeechRecognizer` + `TextToSpeech` with generation counter, markdown sanitization, sentence chunking, and locale-aware voice scoring (no hardcoded gender or vendor voice names).
- **Multilingual Support:** English (India), Tamil (தமிழ்), and Tanglish detection and reply routing.
- **Local Device Assistant:** Offline time/date, inexact AlarmManager reminders (with boot rescheduling), Clock timers, app launcher, Android settings shortcuts, and local memory facts.
- **Privacy & Crash Protection:** `CrashLogger` installed first in `JarvisApplication`, `StartupTracker`, `InMemory` fallback if Room fails, `Redactor` on all logs/exports, Android KeyStore AES-GCM key storage, and strictly separate **Clear Stored Secrets** vs **Clear All App Data**.

See `docs/BUILD.md` for step-by-step instructions to build the signed debug APK with Android Studio, command-line Gradle, or GitHub Actions CI.
