#!/usr/bin/env bash
set -euo pipefail

DEFAULT_DIR="$HOME/.config/systemdeck"
DEFAULT_KEYSTORE="$DEFAULT_DIR/systemdeck-release.jks"
KEYSTORE="${SYSTEMDECK_STORE_FILE:-$DEFAULT_KEYSTORE}"
ALIAS="${SYSTEMDECK_KEY_ALIAS:-systemdeck}"

command -v keytool >/dev/null || { echo 'ERROR: keytool not found. Install/use a JDK first.' >&2; exit 1; }

mkdir -p "$(dirname "$KEYSTORE")"
chmod 700 "$(dirname "$KEYSTORE")" 2>/dev/null || true

if [[ -e "$KEYSTORE" ]]; then
  echo "ERROR: keystore already exists: $KEYSTORE" >&2
  echo 'Refusing to overwrite the release identity.' >&2
  exit 2
fi

echo 'SystemDeck release keystore creation'
echo "Keystore: $KEYSTORE"
echo "Alias:    $ALIAS"
echo
echo 'You will be prompted by keytool for the keystore/key password and certificate identity.'
echo 'Keep this keystore and its password permanently. Future SystemDeck updates must use the same signing key.'
echo

keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storetype JKS

chmod 600 "$KEYSTORE" 2>/dev/null || true

echo
echo 'Keystore created successfully.'
echo "Path: $KEYSTORE"
echo
echo 'Back it up offline before publishing v1.0.0.'
