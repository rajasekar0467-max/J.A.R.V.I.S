# Architectural & Engineering Decisions (`DECISIONS.md`)

1. **Honest Separation of Artifact A (Argon Native Android API APK) and Artifact B (Kotlin + Compose + Hilt Gradle Repo)**:
   - Because the Gemini Argon container is air-gapped from Google Maven (`dl.google.com`), Artifact A is built as a zero-external-dependency Native Android API APK (`classes.dex` + binary `AndroidManifest.xml` + `resources.arsc`) and explicitly labelled as such. Artifact B contains the full Kotlin + Jetpack Compose + Material 3 + Hilt Gradle project for source control and optional GitHub Actions cloud builds.
2. **Supported App-Data Reset Instead of `ActivityManager.clearApplicationUserData()`**:
   - `AppDataResetManager` explicitly clears secret preferences, deletes the `AndroidKeyStore` master key alias, resets user settings preferences, deletes local databases via `context.deleteDatabase()`, and recursively deletes internal `filesDir`/`cacheDir` contents, keeping **Clear Stored Secrets Only** strictly separate from **Reset All Application Data**.
3. **Minimal Progressive Manifest Permissions**:
   - Phase 0 declares **0 permissions** in `AndroidManifest.xml`. Permissions (`RECORD_AUDIO`, `INTERNET`, `FOREGROUND_SERVICE_MICROPHONE`, `READ_CALENDAR`, `READ_CONTACTS`) will be added strictly in Phases 1–7 as their features are implemented.
4. **Special Access Settings Flows**:
   - Battery Optimization exemption uses `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` + `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`, and Notification Listener access uses `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.
