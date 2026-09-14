#!/usr/bin/env bash
# SystemDeck Xiaomi Pad smoke test helper.
# Requires adb and an attached device. Does not clear app or launcher data.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
PACKAGE='dev.rajmyr.systemdeck.debug'
APK='app/build/outputs/apk/debug/app-debug.apk'

command -v adb >/dev/null || { echo 'adb not found in PATH'; exit 1; }
[[ -f "$APK" ]] || { echo "Missing $APK; run scripts/verify-release.sh or ./gradlew :app:assembleDebug first."; exit 1; }

printf 'Connected devices:\n'
adb devices

printf '\nInstalling debug APK (preserving app data)...\n'
adb install -r -t "$APK"

printf '\nLaunching SystemDeck...\n'
adb shell am force-stop "$PACKAGE"
adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
sleep 2

printf '\nPackage/version:\n'
adb shell dumpsys package "$PACKAGE" | grep -E 'versionName=|versionCode=' | head -4 || true

printf '\nWidget state:\n'
adb shell dumpsys appwidget | grep -A12 -B4 'dev.rajmyr.systemdeck' || true

printf '\nWorkManager / JobScheduler entries:\n'
adb shell dumpsys jobscheduler | grep -i -A12 -B5 'systemdeck' || true

printf '\nBackground restriction snapshot:\n'
adb shell am get-standby-bucket "$PACKAGE" || true
adb shell cmd appops get "$PACKAGE" RUN_ANY_IN_BACKGROUND 2>/dev/null || true

printf '\nSmoke helper complete. Inspect the app manually using PHASE7_11_RELEASE_CHECKLIST.md.\n'
