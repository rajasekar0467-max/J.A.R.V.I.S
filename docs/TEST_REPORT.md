# JARVIS Test Report

| Test Layer | Files Included | Sandbox Status | Notes |
|---|---|---|---|
| Static Integration Audit | All `.kt`, `.kts`, `.xml` files | **PASSED (Static)** | Verified package headers, balanced delimiters, zero TODOs, zero forbidden imports, exact 5 permissions, and cross-file symbol resolution. |
| JVM Unit Tests (`app/src/test`) | 11 test classes (`LanguageDetectorTest`, `CommandRouterTest`, `SseParserTest`, `OpenAiCompatibleProviderTest`, `AiEngineTest`, `ValidatorsAndCoreTest`, `VoiceStateMachineTest`, `VoiceHelpersTest`, `StartupTrackerTest`, `AssistantViewModelTest`, `DiagnosticsReportTest`) | **BLOCKED** (`NOT VERIFIED`) | Requires JDK 17 (`javac`) + Gradle + Maven Central to run `gradle testDebugUnitTest`. |
| Android Instrumentation Tests (`app/src/androidTest`) | 5 test classes (`JarvisDatabaseTest`, `SecretStoreInstrumentedTest`, `MainActivityStartupTest`, `NavigationTest`, `PermissionDenialTest`) | **BLOCKED** (`NOT VERIFIED`) | Requires Android 14 device (Realme 9i 5G RMX3612) or emulator (`gradle connectedDebugAndroidTest`). |
| APK Compilation & Signature Verification | `assembleDebug` + `aapt2` + `apksigner` | **BLOCKED** (`NOT VERIFIED`) | Configured in `.github/workflows/android-ci.yml` and `docs/BUILD.md`. |
