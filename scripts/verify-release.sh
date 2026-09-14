#!/usr/bin/env bash
# SystemDeck Android release verification
# Runs source invariants, unit tests, Android lint, and debug/release assembly.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

fail() {
  printf '\n[FAIL] %s\n' "$1" >&2
  exit 1
}

printf 'SystemDeck release verification\n'
printf 'Project: %s\n\n' "$ROOT"

printf '[1/8] Source hygiene\n'
if find app/src/main -type f \( -name '*.bak' -o -name '*.bak-*' -o -name '*~' \) -print | grep -q .; then
  find app/src/main -type f \( -name '*.bak' -o -name '*.bak-*' -o -name '*~' \) -print
  fail 'Backup/editor files remain under app/src/main.'
fi
printf '  PASS: no backup/editor files under app/src/main\n'

printf '[2/8] RemoteViews allowlist guard\n'
if grep -RniE '<[[:space:]]*View([[:space:]/>])' app/src/main/res/layout --include='*.xml'; then
  fail 'Generic <View> found in widget/layout resources. RemoteViews may reject it.'
fi
printf '  PASS: no generic <View> tags in layout XML\n'

printf '[3/8] No unfinished markers\n'
if grep -RniE '\b(TODO|FIXME|XXX)\b' app/src/main app/src/test --exclude='*.bak*'; then
  fail 'TODO/FIXME/XXX markers remain in shipping source.'
fi
printf '  PASS: no unfinished markers\n'

printf '[4/8] Manifest/privacy invariants\n'
grep -q 'android:allowBackup="false"' app/src/main/AndroidManifest.xml || fail 'allowBackup must remain false.'
grep -q 'android.permission.ACCESS_NETWORK_STATE' app/src/main/AndroidManifest.xml || fail 'ACCESS_NETWORK_STATE missing.'
grep -q 'android.permission.POST_NOTIFICATIONS' app/src/main/AndroidManifest.xml || fail 'POST_NOTIFICATIONS missing.'
if grep -qE 'INTERNET|MANAGE_EXTERNAL_STORAGE|READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE|QUERY_ALL_PACKAGES' app/src/main/AndroidManifest.xml; then
  fail 'Unexpected broad/network permission found in manifest.'
fi
grep -q 'androidx.core.content.FileProvider' app/src/main/AndroidManifest.xml || fail 'FileProvider missing.'
printf '  PASS: local-only permission surface preserved\n'

printf '[5/8] Widget scheduling invariants\n'
grep -q 'android:updatePeriodMillis="0"' app/src/main/res/xml/systemdeck_widget_info.xml || fail 'Widget provider periodic timer should remain disabled.'
grep -q 'PERIOD_MINUTES = 15L' app/src/main/java/dev/rajmyr/systemdeck/feature/widget/WidgetRefreshScheduler.kt || fail 'Widget WorkManager cadence changed unexpectedly.'
grep -q 'FOREGROUND_PUBLISH_INTERVAL_MS = 10_000L' app/src/main/java/dev/rajmyr/systemdeck/feature/widget/SystemDeckWidgetProvider.kt || fail 'Foreground widget throttle changed unexpectedly.'
printf '  PASS: widget cadence policy verified\n'

[[ -x ./gradlew ]] || fail './gradlew is missing or not executable.'

printf '[6/8] Unit tests\n'
./gradlew :app:testDebugUnitTest

printf '[7/8] Android lint\n'
./gradlew :app:lintDebug

printf '[8/8] APK assembly\n'
./gradlew :app:assembleDebug :app:assembleRelease

DEBUG_APK='app/build/outputs/apk/debug/app-debug.apk'
RELEASE_APK='app/build/outputs/apk/release/app-release-unsigned.apk'

printf '\nVerification complete.\n'
if [[ -f "$DEBUG_APK" ]]; then
  printf 'Debug APK:   %s\n' "$DEBUG_APK"
  shasum -a 256 "$DEBUG_APK" 2>/dev/null || sha256sum "$DEBUG_APK"
fi
if [[ -f "$RELEASE_APK" ]]; then
  printf 'Release APK: %s (unsigned)\n' "$RELEASE_APK"
  shasum -a 256 "$RELEASE_APK" 2>/dev/null || sha256sum "$RELEASE_APK"
fi
