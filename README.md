# SystemDeck Android

SystemDeck is a native Android system telemetry dashboard designed for tablet-first use. It presents device, CPU, memory, battery, thermal, network, storage, GPU, and sensor telemetry in a compact local dashboard, with diagnostics, alerts, export tools, and a responsive home-screen widget.

The application is designed around three constraints:

1. **Local-first** — telemetry is collected and processed on the device.
2. **No root requirement** — the baseline product uses public Android APIs and readable system interfaces only.
3. **No fabricated telemetry** — unsupported or restricted metrics are hidden, marked unavailable, or explicitly labeled experimental.

---

## Project Status

| Item | Status |
| --- | --- |
| Stable version | `1.0.0` |
| Release channel | Stable |
| Application ID | `dev.rajmyr.systemdeck` |
| Debug application ID | `dev.rajmyr.systemdeck.debug` |
| Minimum Android | Android 10 / API 29 |
| Target SDK | API 34 |
| Compile SDK | API 37 |
| Architecture | Single-app, feature-oriented Android architecture |
| UI | Jetpack Compose |
| Root required | No |
| Cloud account | No |
| Analytics | No |
| Internet permission | No |

SystemDeck `1.0.0` is the first signed stable release.

---

## Capabilities

SystemDeck currently provides:

- device and Android build information;
- CPU topology, online state, cluster structure, current frequency, minimum/maximum frequency, and governor information;
- memory, available memory, cache, buffers, slab, and swap totals;
- battery level, charging state, health, voltage, temperature, and additional electrical metrics when exposed by the device;
- Android thermal status and readable hardware thermal zones;
- active network state, transport information, receive/transmit rates, and traffic totals;
- internal storage usage and capacity;
- Android hardware sensor telemetry;
- GPU metadata and supported GPU telemetry, with experimental metrics clearly identified;
- rolling in-memory telemetry history;
- local threshold alerts and notifications;
- diagnostics and collector health states;
- text and CSV telemetry export;
- responsive home-screen widgets for compact, normal, and expanded sizes;
- configurable interface density and telemetry presentation.

### Main Screens

`Overview` · `Performance` · `CPU` · `Memory` · `Battery` · `Thermal` · `Network` · `Storage` · `GPU` · `Sensors` · `Device` · `Alerts` · `Diagnostics` · `Settings`

---

## Telemetry Model

SystemDeck does not assume that every Android device exposes the same low-level information.

| Domain | Primary source | Notes |
| --- | --- | --- |
| CPU | `/sys/devices/system/cpu/...` | Frequency/topology/governor where readable |
| Memory | `/proc/meminfo` | Memory and swap totals |
| Battery | Android battery APIs | Device-dependent electrical fields |
| Thermal | `PowerManager` + readable thermal sysfs | OEM/kernel dependent |
| Network | Android connectivity and traffic APIs | No Internet permission required |
| Storage | `StatFs` | App-visible internal storage |
| Sensors | `SensorManager` | Hardware dependent |
| GPU | readable platform interfaces | Some metrics are experimental |
| Device | Android `Build` / platform APIs | Local device metadata |

### Important CPU Limitation

SystemDeck does **not** present fabricated CPU utilization percentages when the device prevents reliable access to the required counters.

On the reference Xiaomi Pad 6 environment, `/proc/stat` is not available to the normal application context. SystemDeck therefore presents validated frequency/topology information instead of inventing a utilization value.

---

## Widget Update Policy

The SystemDeck widget is a snapshot surface, not a permanent one-second telemetry process.

| State | Update behavior |
| --- | --- |
| Manual refresh | Immediate |
| SystemDeck in foreground | RemoteViews publication throttled to approximately 10 seconds |
| SystemDeck in background | WorkManager periodic snapshot, minimum interval 15 minutes |
| Last widget removed | Periodic widget work is cancelled |

Android and OEM power management may defer background work. The widget therefore shows snapshot/update state rather than claiming continuous live telemetry.

Supported widget layouts:

- compact;
- normal;
- expanded.

The expanded layout uses the additional space for real telemetry rather than decorative filler.

---

## Architecture

SystemDeck follows a feature-oriented architecture with shared telemetry infrastructure.

```text
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                            │
│  Overview / CPU / Memory / Battery / Thermal / Network ... │
└──────────────────────────────┬──────────────────────────────┘
                               │
                     SystemDeckViewModel
                               │
          ┌────────────────────┴────────────────────┐
          │                                         │
   Telemetry collectors                    Core services
          │                              ┌───────────────┐
          │                              │ History       │
          │                              │ Statistics    │
          │                              │ Alerts        │
          │                              │ Diagnostics   │
          │                              │ Export        │
          │                              └───────────────┘
          │
┌─────────┴───────────────────────────────────────────────────┐
│ CPU · Memory · Battery · Thermal · Network · Storage       │
│ GPU · Sensors · Device                                     │
└─────────┬───────────────────────────────────────────────────┘
          │
┌─────────┴───────────────────────────────────────────────────┐
│ Android APIs · procfs/sysfs interfaces readable by app     │
└─────────────────────────────────────────────────────────────┘
```

### Source Layout

```text
app/src/main/java/dev/rajmyr/systemdeck/
├── core/
│   ├── alerts/
│   ├── format/
│   ├── model/
│   ├── preferences/
│   ├── report/
│   └── telemetry/
├── data/
│   ├── battery/
│   ├── cpu/
│   ├── device/
│   ├── gpu/
│   ├── memory/
│   ├── network/
│   ├── sensors/
│   ├── storage/
│   └── thermal/
├── feature/
│   ├── alerts/
│   ├── battery/
│   ├── cpu/
│   ├── diagnostics/
│   ├── gpu/
│   ├── memory/
│   ├── network/
│   ├── overview/
│   ├── performance/
│   ├── sensors/
│   ├── settings/
│   ├── shell/
│   ├── storage/
│   ├── thermal/
│   └── widget/
└── ui/
    ├── components/
    └── theme/
```

---

## Technology Stack

- **Kotlin**
- **Jetpack Compose**
- **AndroidX Lifecycle / ViewModel**
- **Kotlin Coroutines / Flow**
- **DataStore Preferences**
- **WorkManager**
- **Android RemoteViews / AppWidget**
- **Gradle Kotlin DSL**

Current build toolchain:

| Tool | Version |
| --- | --- |
| Android Gradle Plugin | `9.3.0` |
| Gradle | `9.5.0` |
| Kotlin Compose plugin | `2.3.21` |
| Java compatibility | `17` |
| Compose BOM | `2026.08.00` |
| WorkManager | `2.11.2` |
| DataStore | `1.2.1` |

---

## Requirements

For development:

- macOS, Linux, or Windows;
- JDK 17 or newer compatible with the configured Android toolchain;
- Android SDK with API 37 installed;
- Android SDK Build Tools;
- ADB for physical-device testing;
- Android Studio recommended.

Verify the local toolchain:

```bash
./gradlew --version
adb version
```

---

## Getting Started

Clone the repository and enter the project directory:

```bash
git clone <repository-url>
cd SystemDeck-Android
```

Build the debug application:

```bash
./gradlew :app:assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```bash
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
```

Launch:

```bash
adb shell monkey \
  -p dev.rajmyr.systemdeck.debug \
  -c android.intent.category.LAUNCHER 1
```

---

## Build Variants

### Debug

```text
Application ID: dev.rajmyr.systemdeck.debug
Version suffix: -debug
```

Used for development, ADB testing, telemetry validation, and UI iteration.

Build:

```bash
./gradlew :app:assembleDebug
```

### Release

```text
Application ID: dev.rajmyr.systemdeck
Current version: 1.0.0
Version code: 13
```

Release builds are non-debuggable. Minification/resource shrinking are intentionally disabled in `1.0.0` to preserve behavior validated during the release-candidate cycle.

---

## Quality Gates

The project treats lint and release verification as build gates.

Run the standard verification pipeline:

```bash
./scripts/verify-release.sh
```

The verification script checks:

1. source hygiene;
2. RemoteViews-compatible widget layouts;
3. unfinished source markers;
4. manifest/privacy invariants;
5. widget scheduling invariants;
6. unit tests;
7. Android lint;
8. debug and release assembly.

Individual checks can also be run manually:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:lintRelease
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

### Physical Device Smoke Test

With a device connected through ADB:

```bash
./scripts/device-smoke.sh
```

This installs the debug APK while preserving application data and inspects:

- package/version state;
- widget binding;
- JobScheduler/WorkManager state;
- background restriction state.

It does **not** clear SystemDeck data or launcher data.

---

## Release Signing

Release signing credentials are never stored directly in source control.

The Gradle release configuration reads:

```text
SYSTEMDECK_STORE_FILE
SYSTEMDECK_STORE_PASSWORD
SYSTEMDECK_KEY_ALIAS
SYSTEMDECK_KEY_PASSWORD
```

### Create the Permanent Release Key

Run once:

```bash
./scripts/create-release-keystore.sh
```

Default location:

```text
~/.config/systemdeck/systemdeck-release.jks
```

Default alias:

```text
systemdeck
```

> The release keystore is part of the application's long-term identity. Do not commit it, regenerate it after publishing, or lose the offline backup. Future application updates must be signed with the same key.

### Build Signed Release Artifacts

```bash
./scripts/build-signed-release.sh
```

The script performs:

- unit tests;
- release lint;
- signed APK build;
- signed AAB build;
- APK signature verification;
- AAB signature verification;
- SHA-256 calculation.

Outputs:

```text
app/build/outputs/apk/release/app-release.apk
app/build/outputs/bundle/release/app-release.aab
```

---

## Release Artifacts

The stable release archive for `v1.0.0` contains:

```text
SystemDeck-1.0.0.apk
SystemDeck-1.0.0.aab
SystemDeck-1.0.0-source.zip
SHA256SUMS.txt
```

The APK is the primary direct-install artifact. The AAB is intended for Android distribution systems such as Google Play.

Verify downloaded artifacts using:

```bash
shasum -a 256 -c SHA256SUMS.txt
```

---

## Source Release Export

Create a clean source archive:

```bash
./scripts/export-source-release.sh
```

The exporter excludes local IDE state, build outputs, backup directories, signing material, local SDK configuration, and other machine-specific files.

---

## Permissions and Privacy

SystemDeck `1.0.0` requests only:

```text
android.permission.ACCESS_NETWORK_STATE
android.permission.POST_NOTIFICATIONS
```

The application intentionally does **not** request:

- `INTERNET`;
- broad external-storage access;
- package enumeration;
- root access.

Additional privacy properties:

- telemetry remains on-device;
- no user account is required;
- no cloud backend is used;
- no analytics SDK is included;
- Android backup is disabled;
- exported telemetry uses an application-controlled `FileProvider`.

---

## Alerts

SystemDeck can evaluate local telemetry thresholds and publish Android notifications when enabled.

Notification publication is treated as best-effort:

- runtime notification permission is checked;
- disabled notifications are respected;
- permission revocation is handled defensively;
- alert cooldown prevents repeated notification spam;
- background checks use WorkManager instead of a permanent foreground service.

---

## Telemetry History and Export

SystemDeck keeps a bounded rolling telemetry buffer in memory.

Current session history:

```text
Retention: 15 minutes
Maximum:   900 samples per series
Storage:   memory only
```

Tracked series include CPU frequency, memory usage, network receive/transmit rate, battery, thermal maximum, experimental GPU busy telemetry, and storage usage.

Exports are available as human-readable text and CSV where applicable.

---

## Design Principles

SystemDeck follows a data-first interface model:

- telemetry values take visual priority;
- supporting prose is minimized;
- unsupported metrics are not fabricated;
- experimental metrics are labeled explicitly;
- stale/unavailable/error states remain distinguishable;
- tablet landscape usage is treated as a primary layout;
- decorative UI is kept subordinate to information density.

The visual language is intentionally dark, restrained, and inspired by desktop/Linux telemetry tooling while preserving Android-native interaction.

---

## Known Platform Constraints

Android and OEM security policies can restrict low-level telemetry.

Expected limitations include:

- some `/proc` and `/sys` files may be inaccessible to applications;
- thermal-zone availability varies by kernel and OEM;
- battery current/charge counters vary by hardware implementation;
- GPU telemetry is highly device-specific;
- background WorkManager execution is not an exact timer;
- third-party launchers may implement AppWidget sizing differently.

SystemDeck treats these conditions as capability differences rather than application failures.

---

## Development Policy

Changes should preserve the following invariants:

1. no fabricated telemetry;
2. no root dependency for baseline functionality;
3. no unnecessary permissions;
4. no hidden network/cloud dependency;
5. no destructive launcher/application reset as part of normal diagnostics;
6. widget layouts must remain RemoteViews-compatible;
7. release builds must pass tests and Android lint;
8. signing secrets must remain outside the repository.

For architecture-impacting changes, update or add tests before release promotion.

---

## Versioning

SystemDeck follows semantic versioning:

```text
MAJOR.MINOR.PATCH
```

Examples:

```text
1.0.0  first stable release
1.0.1  backward-compatible bug fix
1.1.0  backward-compatible feature release
2.0.0  incompatible architecture/product change
```

Git release tags use:

```text
v1.0.0
```

---

## Contributing

For changes to the project:

1. create a focused branch;
2. keep changes scoped to one concern;
3. preserve telemetry capability semantics;
4. add or update tests where applicable;
5. run the release verification script;
6. submit the change for review before merging into `main`.

Recommended local validation:

```bash
./scripts/verify-release.sh
```

Do not include signing keys, passwords, generated release binaries, build directories, local SDK configuration, or development backup directories in commits.

---

## Security

Do not report or commit:

- release keystores;
- signing passwords;
- local environment secrets;
- device-specific private information.

Security-sensitive defects should be reported privately to the project maintainer rather than disclosed with working exploitation details in a public issue.

---

## License

No public open-source license is declared in this repository at this time.

Unless a `LICENSE` file is added, the source code should be treated as **all rights reserved** and not assumed to permit redistribution, modification, or commercial reuse.

---

## Maintainer

**Raj**  
SystemDeck Android

---

## Release

Current stable release:

```text
SystemDeck Android v1.0.0
STABLE · SIGNED · RELEASE READY
```
