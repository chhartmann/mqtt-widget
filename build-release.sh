#!/usr/bin/env bash
# Build the signed release APK for mqtt-widget on this machine.
#
# This host is ARM64 (aarch64), but the Android build tools (aapt2, zipalign)
# are x86-64 binaries. They run via qemu-user binfmt emulation, which needs two
# things:
#   1. a registered x86-64 binfmt handler (kernel state -> lost on every reboot)
#   2. QEMU_LD_PREFIX pointing at an x86-64 glibc rootfs, because the host has
#      no /lib64/ld-linux-x86-64.so.2 for qemu to hand the binary to
#
# See AGENTS.md ("Build environment") for the full background.
set -euo pipefail

X86_ROOTFS=/home/chn/Android/x86-rootfs
JDK=/home/chn/Android/jdk-21.0.12.1+1
SDK=/home/chn/Android/Sdk
BINFMT_MARKER=/proc/sys/fs/binfmt_misc/qemu-x86_64

cd "$(dirname "$0")"

# 1. Restore the x86-64 binfmt handler if it is gone (it disappears on reboot).
if [ ! -e "$BINFMT_MARKER" ]; then
    echo "Registering x86-64 binfmt handler (tonistiigi/binfmt) ..."
    docker run --privileged --rm tonistiigi/binfmt --install amd64
fi

# 2. Make qemu find the x86-64 dynamic loader inside the rootfs.
export QEMU_LD_PREFIX="$X86_ROOTFS"
export JAVA_HOME="$JDK"
export ANDROID_HOME="$SDK"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

# 3. The Gradle daemon keeps its startup environment. If a daemon is already
#    running without QEMU_LD_PREFIX, its AAPT2 daemons cannot start, so stop it.
#    (A few seconds per build; guarantees the emulation fix reaches AAPT2.)
./gradlew --stop >/dev/null 2>&1 || true

# 4. Build.
./gradlew assembleRelease "$@"

APK=app/build/outputs/apk/release/app-release.apk
echo
echo "Release APK:"
ls -l "$APK"
