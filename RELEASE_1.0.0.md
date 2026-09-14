# SystemDeck Android 1.0.0 — Release Freeze

Status: stable release candidate promotion target.

## Frozen product scope

- Local, no-root Android telemetry dashboard.
- CPU frequency/topology/governor telemetry.
- Memory and swap totals.
- Battery public-API telemetry.
- Thermal state and readable thermal sensors.
- Network, storage, GPU experimental telemetry, hardware sensors.
- Rolling in-memory history and diagnostics/export.
- Local threshold alerts.
- Kvaesitso-compatible responsive AppWidget.
- No account, cloud sync, analytics, INTERNET permission, or broad storage permission.

## Version

- applicationId: `dev.rajmyr.systemdeck`
- versionCode: `13`
- versionName: `1.0.0`

The debug build remains `dev.rajmyr.systemdeck.debug` and may coexist with the release build.

## Release signing

The Gradle build reads signing credentials only from process environment variables:

- `SYSTEMDECK_STORE_FILE`
- `SYSTEMDECK_STORE_PASSWORD`
- `SYSTEMDECK_KEY_ALIAS`
- `SYSTEMDECK_KEY_PASSWORD`

Passwords are not committed to the repository.

Create the release identity exactly once:

```bash
./scripts/create-release-keystore.sh
```

Build and verify signed artifacts:

```bash
./scripts/build-signed-release.sh
```

Expected outputs:

- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

## Final device validation

The signed release package is separate from the debug package. Install it alongside debug for final smoke testing:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell monkey -p dev.rajmyr.systemdeck -c android.intent.category.LAUNCHER 1
```

Verify the release package:

```bash
adb shell dumpsys package dev.rajmyr.systemdeck | grep -E 'versionName=|versionCode='
```

Expected: `versionName=1.0.0`, versionCode 13.

Because release and debug have different application IDs, release preferences and widget instances begin separately. Do not treat this as lost debug data.

## Source archive

```bash
./scripts/export-source-release.sh
```

The exporter excludes local SDK paths, build outputs, backups, and signing material.

## Signing-key rule

The release keystore is the long-term application identity. Back it up offline. Do not regenerate it after publishing a build that users may update in place.
