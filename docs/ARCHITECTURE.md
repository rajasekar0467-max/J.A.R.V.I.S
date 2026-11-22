# JARVIS Architecture

- **Manual Dependency Injection:** `AppContainer` (owned by `JarvisApplication`) + `JarvisViewModelFactory`. No Hilt, Dagger, or Koin.
- **Data Layer:** Room (`JarvisDatabase` with `ConversationDao`, `MessageDao`, `MemoryFactDao`, `ReminderDao` via KSP) + Preferences DataStore (`SettingsRepository`) + `InMemory*Repository` fallback if SQLite fails to open.
- **Security:** `SecretStore` using `AndroidKeyStore` AES-256-GCM + `ApiKeyValidator` + `Redactor`.
- **AI Engine:** `OpenAiCompatibleProvider` (OkHttp SSE) wrapped by `AiEngine` (single retry before first delta), `ContextWindow`, `PromptBuilder`, `UrlValidator`, `SseParser` (manual `Json.parseToJsonElement`, no serialization compiler plugin).
- **Voice Engine:** `SpeechRecognizerController` + `TtsController` + `VoiceStateMachine` + `FridayVoiceProfile` (single profile) + `SpeechTextSanitizer` + `RecognitionErrorMapper`.
- **Device Assistant:** `CommandRouter` -> `AndroidDeviceActions` + `ReminderAlarms` (inexact `setAndAllowWhileIdle`) + `ReminderReceiver`/`BootReceiver` (`goAsync()`).
- **UI Layer:** Single-activity Jetpack Compose (`MainActivity` with `enableEdgeToEdge()` and native `TextView` fallback), 5-tab `NavigationBar` (`nav_home`, `nav_chat`, `nav_voice`, `nav_history`, `nav_settings`) + `diagnostics`.
