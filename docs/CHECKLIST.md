# Realme 9i 5G (RMX3612, Android 14) Verification Checklist

1. Build `app-debug.apk` via GitHub Actions CI or local Gradle (`gradle testDebugUnitTest assembleDebug`).
2. Confirm all 11 JVM unit test classes pass.
3. Install APK via `adb install -r app-debug.apk` on Realme 9i 5G (Android 14 / Realme UI 5.0).
4. Launch JARVIS and verify StartupScreen transitions smoothly into the emerald Home screen without crashing.
5. Verify all 5 bottom tabs (`Home`, `Chat`, `Voice`, `History`, `Settings`) and `Diagnostics` open cleanly.
6. Test offline local command in Chat: `"What time is it?"` and `"Remind me in 5 minutes to drink water"`.
7. Add a Gemini/OpenAI/Groq/OpenRouter API key in Settings -> tap **Test AI Connection** in Diagnostics.
8. Test streaming chat reply + Stop button + English/Tamil/Tanglish switching.
9. Test Voice screen mic permission flow (allow and deny), live transcript, and FRIDAY-inspired TTS playback.
10. Verify **Clear Stored Secrets** does not erase chats, and **Clear All App Data** preserves stored secrets.
