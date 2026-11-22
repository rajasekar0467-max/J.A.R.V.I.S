# JARVIS Permissions Manifest Policy

JARVIS declares **exactly 5 permissions** in `AndroidManifest.xml`:

| Permission | Purpose | Runtime Prompt? |
|---|---|---|
| `android.permission.RECORD_AUDIO` | Voice input via Android `SpeechRecognizer` on the Voice screen. | Yes (requested only when user taps Mic) |
| `android.permission.INTERNET` | Streaming chat completions from the user-configured HTTPS AI endpoint. | Normal |
| `android.permission.ACCESS_NETWORK_STATE` | Live online/offline indicator in the header and Diagnostics screen. | Normal |
| `android.permission.POST_NOTIFICATIONS` | Displaying local reminder alerts on Android 13/14+. | Yes (Android 13+) |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Rescheduling pending local reminders after device reboot. | Normal |

No exact-alarm, overlay, accessibility, contacts, SMS, location, storage, foreground-service, or background-microphone permissions are declared.
