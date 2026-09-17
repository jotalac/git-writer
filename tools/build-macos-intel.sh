#!/usr/bin/env bash
#
# tools/build-macos-intel.sh
#
# Builds the macOS Intel (x86_64) desktop release from an Apple Silicon Mac.
#
# Why this is needed: Compose Desktop packages with the jpackage that ships in the
# JVM running Gradle -- the *daemon*. On Apple Silicon that daemon is arm64, so a
# plain `./gradlew packageReleaseDistributionForCurrentOS` always emits an arm64
# app no matter what shell variables are set. Forcing JAVA_HOME to an x86_64 JDK
# before Gradle starts makes the daemon x86_64, and jpackage then produces an
# Intel bundle (built under Rosetta, which works fine and is the only way to
# cross-build this).
#
# Requires an x86_64 JDK 21. Install one with:
#   brew install --cask temurin@21        # or download Corretto 21 x64
#
# Usage: tools/build-macos-intel.sh [gradle task ...]
#   Default task: packageReleaseDistributionForCurrentOS

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TASK="${*:-packageReleaseDistributionForCurrentOS}"

log() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
die() { printf '\033[1;31merror:\033[0m %s\n' "$*" >&2; exit 1; }

[[ "$(uname -m)" == "arm64" ]] || log "note: host is not arm64; building Intel anyway"

# Locate an x86_64 JDK 21. java_home -a reports the architecture of the JDK itself.
X64_JDK="$(/usr/libexec/java_home -v 21 -a x86_64 2>/dev/null || true)"
[[ -n "$X64_JDK" ]] || die "No x86_64 JDK 21 found. Install one (e.g. brew install --cask temurin@21)."

[[ "$(file -b "$X64_JDK/bin/jpackage" | grep -c x86_64)" == "1" ]] \
    || die "$X64_JDK does not provide an x86_64 jpackage."

log "Using x86_64 JDK: $X64_JDK"

# --stop first: a running arm64 daemon would be reused and would package arm64.
log "Stopping any running Gradle daemon"
(cd "$ROOT" && JAVA_HOME="$X64_JDK" ./gradlew --stop >/dev/null 2>&1) || true

log "Building: $TASK"
(cd "$ROOT" && JAVA_HOME="$X64_JDK" ./gradlew $TASK)

# Verify the produced bundle really is Intel.
APP="$(find "$ROOT/desktopApp/build/compose/binaries" -name "*.app" -type d 2>/dev/null | head -1)"
if [[ -n "$APP" ]]; then
    BIN="$APP/Contents/MacOS/$(basename "$APP" .app)"
    log "Result: $(file -b "$BIN")"
    [[ "$(file -b "$BIN")" == *x86_64* ]] || die "Bundle is not x86_64: $APP"
    log "Intel bundle OK: $APP"
else
    log "No .app found under desktopApp/build/compose/binaries (checked task output)"
fi
