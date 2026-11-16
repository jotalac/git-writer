#!/usr/bin/env bash
# Builds an unsigned .ipa of the iOS app, for sideloading - AltStore and SideStore sign it on the device.
# Needs Xcode and the libgit2 slices (tools/build-libgit2.sh). No certificate or Apple account is involved.
#
#   tools/build-ios-app.sh [output]     default output: build/git-writer.ipa
#
# The version comes from app.version.number in gradle.properties, like for every other app - it overrides the
# one in iosApp/Configuration/Config.xcconfig, so the two cannot disagree.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

die() { echo "error: $*" >&2; exit 1; }

OUTPUT="${1:-build/git-writer.ipa}"
DERIVED_DATA="build/ios_release"

VERSION="$(grep -m1 '^app.version.number=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"
[[ -n "$VERSION" ]] || die "app.version.number is missing from gradle.properties"
[[ -d third_party/libgit2/slices/iosArm64 ]] || die "libgit2 is not built - run tools/build-libgit2.sh first"

echo "Building GitWriter $VERSION for iOS"

# 1. the .app for devices, without signing
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -derivedDataPath "$DERIVED_DATA" \
  MARKETING_VERSION="$VERSION" \
  CURRENT_PROJECT_VERSION="$VERSION" \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGNING_REQUIRED=NO

APP="$(find "$DERIVED_DATA/Build/Products/Release-iphoneos" -maxdepth 1 -name '*.app' -print -quit)"
[[ -n "$APP" ]] || die "xcodebuild produced no .app"

# 2. an .ipa is a zip with the .app in a Payload folder
STAGING="$(mktemp -d)"
trap 'rm -rf "$STAGING"' EXIT
mkdir "$STAGING/Payload"
cp -R "$APP" "$STAGING/Payload/"

mkdir -p "$(dirname "$OUTPUT")"
OUTPUT="$(cd "$(dirname "$OUTPUT")" && pwd)/$(basename "$OUTPUT")"
rm -f "$OUTPUT"
(cd "$STAGING" && zip -qry "$OUTPUT" Payload)

echo "Built $OUTPUT"
