#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

DEFAULT_KEYSTORE="$HOME/.config/systemdeck/systemdeck-release.jks"
KEYSTORE="${SYSTEMDECK_STORE_FILE:-$DEFAULT_KEYSTORE}"
ALIAS="${SYSTEMDECK_KEY_ALIAS:-systemdeck}"

[[ -f "$KEYSTORE" ]] || {
  echo "ERROR: release keystore not found: $KEYSTORE" >&2
  echo 'Run ./scripts/create-release-keystore.sh first.' >&2
  exit 1
}

read -rsp 'Keystore password: ' STORE_PASSWORD
echo
read -rsp 'Key password (Enter = same as keystore): ' KEY_PASSWORD
echo
if [[ -z "$KEY_PASSWORD" ]]; then
  KEY_PASSWORD="$STORE_PASSWORD"
fi

export SYSTEMDECK_STORE_FILE="$KEYSTORE"
export SYSTEMDECK_STORE_PASSWORD="$STORE_PASSWORD"
export SYSTEMDECK_KEY_ALIAS="$ALIAS"
export SYSTEMDECK_KEY_PASSWORD="$KEY_PASSWORD"

cleanup() {
  unset SYSTEMDECK_STORE_PASSWORD SYSTEMDECK_KEY_PASSWORD STORE_PASSWORD KEY_PASSWORD
}
trap cleanup EXIT

VERSION_LINE="$(grep -m1 'versionName = ' app/build.gradle.kts || true)"
[[ "$VERSION_LINE" == *'"1.0.0"'* ]] || {
  echo "ERROR: expected versionName 1.0.0, got: $VERSION_LINE" >&2
  exit 2
}

echo '== SystemDeck 1.0.0 signed release =='
echo "Keystore: $KEYSTORE"
echo "Alias:    $ALIAS"
echo

echo '[1/5] Unit tests'
./gradlew :app:testDebugUnitTest

echo '[2/5] Release lint'
./gradlew :app:lintRelease

echo '[3/5] Signed APK + AAB'
./gradlew :app:assembleRelease :app:bundleRelease

APK='app/build/outputs/apk/release/app-release.apk'
AAB='app/build/outputs/bundle/release/app-release.aab'
[[ -f "$APK" ]] || { echo "ERROR: signed APK missing: $APK" >&2; exit 3; }
[[ -f "$AAB" ]] || { echo "ERROR: release AAB missing: $AAB" >&2; exit 4; }

echo '[4/5] Signature verification'
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
APKSIGNER="$(find "$SDK_ROOT/build-tools" -type f -name apksigner 2>/dev/null | sort | tail -1 || true)"
if [[ -z "$APKSIGNER" || ! -x "$APKSIGNER" ]]; then
  echo 'WARNING: apksigner not found; APK signature could not be independently verified.' >&2
else
  "$APKSIGNER" verify --verbose --print-certs "$APK"
fi

if command -v jarsigner >/dev/null; then
  jarsigner -verify "$AAB" >/dev/null
  echo 'AAB JAR signature: verified'
else
  echo 'WARNING: jarsigner not found; AAB signature verification skipped.' >&2
fi

echo '[5/5] SHA-256'
(shasum -a 256 "$APK" "$AAB" 2>/dev/null || sha256sum "$APK" "$AAB")

echo
echo 'SIGNED RELEASE SUCCESS'
echo "APK: $APK"
echo "AAB: $AAB"
echo
echo 'Do not delete or regenerate the release keystore after publishing.'
