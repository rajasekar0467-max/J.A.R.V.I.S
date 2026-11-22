# Building & Installing JARVIS

## 1. Prerequisites (Local Machine)
- **JDK 17** (Temurin / Corretto / Android Studio embedded JBR)
- **Android SDK** with Platform `android-35`, Build-Tools `35.0.0`, and Platform-Tools (`adb`)
- **Gradle 8.9** (or Android Studio Ladybug / Meerkat)

## 2. Build Debug APK Locally
```bash
# Option A: Using Android Studio
# Open the JARVIS folder -> Sync Gradle -> Build -> Build APK(s)

# Option B: Command line (with Gradle 8.9 installed)
gradle testDebugUnitTest assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`

## 3. Build via GitHub Actions CI (Zero Local SDK Needed)
1. Push this repository to GitHub.
2. Open the **Actions** tab and run **JARVIS Android CI**.
3. Download the `jarvis-debug-apk` artifact after the workflow passes `testDebugUnitTest`, `assembleDebug`, `aapt2 dump badging`, and `apksigner verify`.

## 4. Install on Realme 9i 5G (RMX3612, Android 14)
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.jarvis.assistant/.MainActivity
adb logcat -s AndroidRuntime:E
```
