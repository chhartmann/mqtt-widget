# AGENTS.md

Android home-screen widget app (Kotlin, Gradle) that publishes one MQTT message
per widget tap. App code lives in `app/src/main/java/com/chn/mqttwidget/`.

## Build the release app

```bash
./build-release.sh
```

Output: `app/build/outputs/apk/release/app-release.apk` (signed, see
`keystore.properties`). The script is idempotent and safe to run after a reboot.

Unit tests: `./build-release.sh testDebugUnitTest` (the script forwards extra
Gradle tasks/flags).

## Build environment (ARM64 host, x86-64 Android tools)

The host is **aarch64 (Debian 13)**, but the Android build tools shipped in the
SDK/AGP Maven artifacts are **x86-64** binaries. They run through qemu-user
binfmt emulation. Two independent pieces make that work; both were set up on
2026-09-27 and both are handled automatically by `build-release.sh`:

1. **x86-64 binfmt handler** – registered with Docker:
   `docker run --privileged --rm tonistiigi/binfmt --install amd64`
   This is kernel state under `/proc/sys/fs/binfmt_misc/` and is **lost on every
   reboot** (and when that pseudo-fs is remounted). Check with
   `ls /proc/sys/fs/binfmt_misc/qemu-x86_64`. Without it, AAPT2 fails with
   `Syntax error: "(" unexpected` or `Exec format error`.

2. **x86-64 glibc rootfs** – `/home/chn/Android/x86-rootfs` (contains
   `lib64/ld-linux-x86-64.so.2`, libc, libgcc, zlib; extracted from Debian amd64
   `libc6`, `libgcc-s1`, `zlib1g` packages). The host has no
   `/lib64/ld-linux-x86-64.so.2`, so qemu cannot load the tool binaries without
   pointing at this rootfs:
   `export QEMU_LD_PREFIX=/home/chn/Android/x86-rootfs`
   Missing this produces `qemu-x86_64: Could not open '/lib64/ld-linux-x86-64.so.2'`.

Tool locations on this machine:

| Tool   | Path                            |
|--------|---------------------------------|
| JDK 21 | `/home/chn/Android/jdk-21.0.12.1+1` |
| Android SDK | `/home/chn/Android/Sdk`     |
| x86-64 rootfs | `/home/chn/Android/x86-rootfs` |

`local.properties` in the project root points Gradle at the SDK (`sdk.dir=...`).

### Manual build (equivalent to the script)

```bash
export QEMU_LD_PREFIX=/home/chn/Android/x86-rootfs
export JAVA_HOME=/home/chn/Android/jdk-21.0.12.1+1
export ANDROID_HOME=/home/chn/Android/Sdk
./gradlew --stop        # see gotcha below
./gradlew assembleRelease
```

### Gotchas

- **Never remove the Paho keep rules** in `app/proguard-rules.pro`. The release
  build is minified with R8, and R8's class merging breaks Paho's
  `LoggerFactory`, which loads `JSR47Logger` via `Class.forName(...)` and casts
  it to `Logger`. With the merged class the cast can never succeed, so R8 then
  deleted the entire connect/publish path of `MqttPublisher` as dead code.
  Symptom: tapping the widget silently does nothing and no MQTT message ever
  leaves the device (2026-09-27). After AGP/R8 upgrades, sanity-check the
  release dex still contains call sites into
  `Lorg/eclipse/paho/client/mqttv3/MqttAsyncClient;`.

- **Stale Gradle daemon**: the daemon caches its startup environment and spawns
  the AAPT2 daemons. If it was started without `QEMU_LD_PREFIX`, every build
  fails at `:app:processReleaseResources` with `AAPT2 ... Daemon startup failed`
  even after the variable is exported. Always `./gradlew --stop` first (the
  build script does this).
- **Reboot breaks builds** until the binfmt handler is re-registered (step 1).
  `build-release.sh` does this automatically via the already-present
  `tonistiigi/binfmt` image; it needs no sudo, only Docker access.
- `sudo` on this machine prompts for a password; keep the workflow rootless.
- Do not use `localhost`/`127.0.0.1` as the MQTT broker when testing on an
  Android emulator; use `10.0.2.2` for a broker on the dev machine.

## Project notes

- The app **publishes** MQTT messages (Eclipse Paho `MqttClient`) from a
  WorkManager job when a widget is tapped; it does not subscribe.
- Widget config UI: `WidgetConfigActivity`; storage: `WidgetConfigStore`
  (per-appWidgetId JSON in SharedPreferences).
- Known-scope troubleshooting aid: publish failures surface as a Toast with the
  Paho reason code (`PublishErrors.describe`).
- **Never rely on a Toast alone for publish results.** A toast posted from the
  background is dropped on Android 13+ unless `POST_NOTIFICATIONS` is granted
  (it also gates background toasts) and on several OEM skins whenever the app's
  notifications are blocked. This app reports failures three ways
  (`NotificationHelper`, `MqttWidgetProvider.showResult`): notification, toast,
  and a persistent red error icon on the widget itself (cleared on the next tap
  or on reconfigure). The error text is deliberately **not** rendered inside the
  widget - it looked like a mini-toast there and was removed (2026-10-03).
  `POST_NOTIFICATIONS` must stay in
  the manifest and is requested at runtime from `WidgetConfigActivity` and
  `MainActivity`.
- Paho reports failures as bare reason codes; `PublishErrors` maps them to
  readable text (e.g. reason 5 -> "not authorized — check username and
  password").
- A broker-side drop can be invisible to the client: a publish rejected by an
  ACL returns normally from Paho `publish()` even at QoS 1, so the app cannot
  report that as an error (verified against mosquitto 2.1.2).
