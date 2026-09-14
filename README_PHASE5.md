# SystemDeck Android — Phase 5 Foundation

This starter establishes the Android application shell only. It deliberately does **not** fabricate telemetry.

## Toolchain

- Android Gradle Plugin: 9.3.0
- Gradle: 9.5.0
- compileSdk: 37
- targetSdk: 34
- minSdk: 29
- Jetpack Compose BOM: 2026.08.00
- Compose compiler plugin: Kotlin 2.3.21
- DataStore: 1.2.1
- Java target: 17

## Architecture in Phase 5

Single `app` module with package-level separation. This is intentional: do not multi-module prematurely.

- `core/model`
- `core/preferences`
- `data/device`
- `feature/shell`
- `feature/overview`
- `feature/diagnostics`
- `feature/settings`
- `ui/theme`

Phase 6 adds collectors/repositories. Later phases can split Gradle modules if the project size justifies it.

## First open on macOS

1. Extract to `~/Developer/SystemDeck-Android-Phase5`.
2. Open Android Studio.
3. `File > Open` and select the extracted directory.
4. Accept Gradle sync/downloads.
5. Ensure Android SDK Platform 37 and Build Tools 36.0.0+ are installed.
6. Use Android Studio's bundled JDK or any JDK >= 17 supported by Gradle 9.5.

The source package is `dev.rajmyr.systemdeck`.

## Device

Connect Xiaomi Pad 6 with USB debugging enabled, then verify:

```bash
adb devices -l
```

The device should appear as `device`, not `unauthorized`.

## Run

Select Xiaomi Pad 6 from Android Studio's device selector and press Run.

Debug application ID:

```text
dev.rajmyr.systemdeck.debug
```

The debug suffix intentionally keeps development builds separate from future release builds.

## Expected Phase 5 behavior

- Landscape-first SystemDeck shell
- Sidebar navigation on wide tablet layouts
- Compact horizontal navigation on narrower layouts
- Real device identity from Android `Build`
- StateFlow-backed navigation state
- DataStore-backed `Compact density` preference
- Diagnostics foundation page
- No root
- No network permission
- No fake CPU/RAM/battery/thermal values

## Phase 5 acceptance test

1. App launches on Xiaomi Pad 6.
2. `Overview`, `Diagnostics`, and `Settings` open.
3. Other sections explicitly say `PENDING / PHASE 6`.
4. `Settings > Compact density` survives app restart.
5. Rotate landscape/portrait and confirm no crash.
6. Return to landscape; sidebar should reappear.
7. Force-stop and reopen; app still works.
8. No runtime permissions should be requested in Phase 5.

## Optional: generate Gradle wrapper CLI files

The project includes `gradle/wrapper/gradle-wrapper.properties` but not a wrapper JAR.
After Android Studio has completed Gradle sync, run the `wrapper` Gradle task from the Gradle tool window once. Commit the generated:

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`

After that CLI builds work normally:

```bash
./gradlew assembleDebug
```

Install manually if desired:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Phase 6 rule

Never show a telemetry value unless the collector can prove it is valid. Unsupported metrics must become capability/diagnostic states, not `0`, guessed data, or decorative placeholders.
