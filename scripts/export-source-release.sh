#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

OUT="${1:-$HOME/Desktop/SystemDeck-1.0.0-source.zip}"
rm -f "$OUT"

zip -r "$OUT" . \
  -x '.git/*' \
     '.gradle/*' \
     '.idea/*' \
     '.phase*-backups/*' \
     '.phase11-backups/*' \
     '.legacy-source-backups/*' \
     'app/build/*' \
     'build/*' \
     'local.properties' \
     '*.iml' \
     '*.jks' \
     '*.keystore' \
     '*.p12' \
     'keystore.properties' \
     'release-secrets.env' \
     '**/.DS_Store' >/dev/null

echo "Source archive: $OUT"
(shasum -a 256 "$OUT" 2>/dev/null || sha256sum "$OUT")
