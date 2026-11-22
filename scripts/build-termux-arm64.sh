#!/usr/bin/env bash
# Reproducible local ARM64 Termux build & verification script for JARVIS (Realme 9i 5G, Android 14).
# Uses native Termux ARM64 aapt2 via -Pandroid.aapt2FromMavenOverride so AGP 8.7.2 does not run x86-64 aapt2.
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

echo "=== 1. Environment Check (Termux ARM64) ==="
uname -m
command -v java >/dev/null || { echo "ERROR: java not found. Install with: pkg install openjdk-21"; exit 1; }
command -v gradle >/dev/null || { echo "ERROR: gradle not found. Install Gradle 8.9."; exit 1; }
AAPT2_BIN="$(command -v aapt2 || true)"
if [ -z "$AAPT2_BIN" ]; then
    echo "ERROR: Native Termux aapt2 not found. Install with: pkg install aapt2"
    exit 1
fi

java -version
gradle -version | head -n 5
echo "Native aapt2: $AAPT2_BIN"

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
echo "ANDROID_HOME: $SDK_DIR"

if [ ! -f "$SDK_DIR/platforms/android-35/android.jar" ]; then
    echo "ERROR: $SDK_DIR/platforms/android-35/android.jar not found."
    echo "Install official Android SDK platform 35 via sdkmanager:"
    echo "  sdkmanager --sdk_root=\"$SDK_DIR\" \"platforms;android-35\" \"platform-tools\" \"build-tools;35.0.0\""
    exit 1
fi
if [ ! -f "$SDK_DIR/licenses/android-sdk-license" ]; then
    echo "ERROR: Android SDK licenses not accepted in $SDK_DIR/licenses/."
    echo "Accept official licenses via: yes | sdkmanager --sdk_root=\"$SDK_DIR\" --licenses"
    exit 1
fi

printf "sdk.dir=%s\n" "$SDK_DIR" > local.properties

echo "=== 2. Running JVM Unit Tests & Debug APK Build ==="
gradle testDebugUnitTest assembleDebug \
    --no-daemon \
    --max-workers=2 \
    --stacktrace \
    -Dorg.gradle.jvmargs="-Xmx1536m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8" \
    -Pandroid.aapt2FromMavenOverride="$AAPT2_BIN" \
    2>&1 | tee build-termux.log

APK="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
    echo "ERROR: Build completed without producing $APK"
    exit 1
fi

cp "$APK" "$PROJECT_DIR/JARVIS-debug.apk"

echo "=== 3. APK Verification ==="
ls -lh "$PROJECT_DIR/JARVIS-debug.apk"
sha256sum "$PROJECT_DIR/JARVIS-debug.apk"
"$AAPT2_BIN" dump badging "$PROJECT_DIR/JARVIS-debug.apk" | grep -E "package:|sdkVersion:|targetSdkVersion:|application-label:" || true
unzip -l "$PROJECT_DIR/JARVIS-debug.apk" | grep -E "classes.*\.dex|AndroidManifest\.xml" || true
if command -v apksigner >/dev/null; then
    apksigner verify --verbose "$PROJECT_DIR/JARVIS-debug.apk"
else
    echo "Note: apksigner not installed in PATH (install with: pkg install apksigner)"
fi

echo "=== BUILD & VERIFICATION COMPLETE: $PROJECT_DIR/JARVIS-debug.apk ==="
