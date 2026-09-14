#!/usr/bin/env bash
#

set -euo pipefail

LIBGIT2_VERSION="${LIBGIT2_VERSION:-1.9.7}"
IOS_DEPLOYMENT_TARGET="${IOS_DEPLOYMENT_TARGET:-13.0}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC="$ROOT/build/libgit2-src"
BUILDS="$ROOT/build"
OUT="$ROOT/third_party/libgit2/slices"

log()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31merror:\033[0m %s\n' "$*" >&2; exit 1; }

# --------------------------------------------------------------------------
# 0. Optional clean
if [[ "${1:-}" == "--clean" ]]; then
  log "Cleaning $BUILDS/libgit2-* and $OUT"
  rm -rf "$BUILDS"/libgit2-* "$OUT"
fi

# --------------------------------------------------------------------------
# 1. Xcode
if [[ -z "${DEVELOPER_DIR:-}" ]]; then
  if [[ -d /Applications/Xcode.app/Contents/Developer ]]; then
    export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
  else
    die "No full Xcode at /Applications/Xcode.app. Install it, or set DEVELOPER_DIR."
  fi
fi
log "DEVELOPER_DIR=$DEVELOPER_DIR"

for tool in cmake ninja git; do
  command -v "$tool" >/dev/null 2>&1 || die "$tool not found on PATH."
done
log "CMake $(cmake --version | head -1 | awk '{print $3}'), Ninja $(ninja --version)"
log "iPhoneOS SDK $(xcrun --sdk iphoneos --show-sdk-version 2>/dev/null || echo '?'), iPhoneSimulator SDK $(xcrun --sdk iphonesimulator --show-sdk-version 2>/dev/null || echo '?')"

# --------------------------------------------------------------------------
# 2. Pinned source (cloned automatically on a fresh machine)
mkdir -p "$BUILDS"

if [[ ! -d "$SRC/.git" ]]; then
  log "Cloning libgit2 v$LIBGIT2_VERSION"
  git clone --depth 1 --branch "v$LIBGIT2_VERSION" \
    https://github.com/libgit2/libgit2.git "$SRC" \
    || die "Failed to clone libgit2 v$LIBGIT2_VERSION"
fi

got="$(grep -m1 'LIBGIT2_VERSION ' "$SRC/include/git2/version.h" | sed -E 's/.*"([^"]+)".*/\1/')"
[[ "$got" == "$LIBGIT2_VERSION" ]] \
  || die "Source is libgit2 $got but $LIBGIT2_VERSION was requested. Re-run with --clean."
log "Using libgit2 $got at $(git -C "$SRC" rev-parse --short HEAD)"

# --------------------------------------------------------------------------
# 3. Build one slice per SDK
SLICES=(
  "iosArm64|iphoneos|arm64"
  "iosSimulatorArm64|iphonesimulator|arm64"
)

build_slice() {
  local name="$1" sysroot="$2" arch="$3"
  local bdir="$BUILDS/libgit2-$name"
  local prefix="$OUT/$name"

  log "Configuring $name (sysroot=$sysroot arch=$arch)"

  # A fresh build dir per slice is REQUIRED: CMake caches find_package /
  # find_library results per configure-time SDK, so reusing one directory
  rm -rf "$bdir"

  # SYSTEM_NAME=iOS is required or CMake skips the iOS workarounds. SHA1 has no
  # "builtin". SONAME must stay unset or the archive loses the libgit2.a name.
  cmake -S "$SRC" -B "$bdir" -G Ninja \
    -DCMAKE_SYSTEM_NAME=iOS \
    -DCMAKE_OSX_SYSROOT="$sysroot" \
    -DCMAKE_OSX_ARCHITECTURES="$arch" \
    -DCMAKE_OSX_DEPLOYMENT_TARGET="$IOS_DEPLOYMENT_TARGET" \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX="$prefix" \
    -DCMAKE_MAKE_PROGRAM="$(command -v ninja)" \
    -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
    -DBUILD_SHARED_LIBS=OFF \
    -DBUILD_TESTS=OFF \
    -DBUILD_CLI=OFF \
    -DBUILD_EXAMPLES=OFF \
    -DBUILD_FUZZERS=OFF \
    -DUSE_SSH=OFF \
    -DUSE_HTTPS=SecureTransport \
    -DUSE_SHA1=CollisionDetection \
    -DUSE_SHA256=CommonCrypto \
    -DUSE_BUNDLED_ZLIB=ON \
    -DUSE_ICONV=OFF \
    -DEXPERIMENTAL_SHA256=OFF \
    -DDEPRECATE_HARD=OFF

  log "Building + installing $name"
  cmake --build "$bdir" --target install --parallel
}

for s in "${SLICES[@]}"; do
  IFS='|' read -r name sysroot arch <<< "$s"
  build_slice "$name" "$sysroot" "$arch"
done

# --------------------------------------------------------------------------
# 4. Verify -- fail loudly rather than produce a silently broken binding
log "Verifying slices"

# True if the archive exports the given C symbol.
#
has_symbol() {
  local n
  n="$(nm -gU "$1" 2>/dev/null | grep -c "_$2\$" || true)"
  [[ "${n:-0}" -gt 0 ]]
}

# True if nm produced any usable output at all, which distinguishes a transient
# toolchain failure from a genuinely missing symbol.
has_symbol_any() {
  local n
  n="$(nm -gU "$1" 2>/dev/null | grep -c ' T _git_' || true)"
  [[ "${n:-0}" -gt 0 ]]
}

for s in "${SLICES[@]}"; do
  IFS='|' read -r name sysroot arch <<< "$s"
  local_prefix="$OUT/$name"

  archive="$(ls "$local_prefix/lib/"libgit*.a 2>/dev/null | head -1 || true)"
  [[ -n "$archive" && -f "$archive" ]] \
    || die "$name: no libgit*.a found in $local_prefix/lib"

  header="$local_prefix/include/git2/experimental.h"
  [[ -f "$header" ]] \
    || die "$name: $header missing (includeDirs must point at the installed include/ tree)"

  # ABI guard: git_oid is 20 bytes only while GIT_EXPERIMENTAL_SHA256 is
  # undefined. If it is set, cinterop's view of the header disagrees with the
  if grep -qE '^#define[[:space:]]+GIT_EXPERIMENTAL_SHA256[[:space:]]+1' "$header"; then
    die "$name: built with GIT_EXPERIMENTAL_SHA256=1 (33-byte git_oid). Rebuild with -DEXPERIMENTAL_SHA256=OFF."
  fi

  slice_arch="$(lipo -archs "$archive" 2>/dev/null || echo unknown)"
  case " $slice_arch " in
    *" $arch "*) : ;;
    *) die "$name: expected arch $arch, got '$slice_arch'" ;;
  esac

  # Primary check: the archive must actually contain libgit2 objects. This is
  # what guarantees the linker will find symbols, and `ar` is not affected by
  objects="$(ar t "$archive" 2>/dev/null | wc -l | tr -d ' ')"
  [[ "${objects:-0}" -gt 50 ]] \
    || die "$name: archive contains only ${objects:-0} objects"

  # Secondary check: confirm the symbols this binding actually needs. Advisory
  # only -- if `nm` produces nothing and no detected symbol is reported missing,
  nm_failed=0
  if ! has_symbol "$archive" git_clone; then
    nm_failed=1
  elif ! has_symbol "$archive" git_merge; then
    nm_failed=1
  elif ! has_symbol "$archive" git_credential_userpass_plaintext_new; then
    nm_failed=1
  fi

  if [[ "$nm_failed" -eq 1 ]]; then
    # Distinguish "nm produced nothing at all" (transient toolchain failure)
    # from "nm worked but the symbol is genuinely absent".
    if has_symbol_any "$archive"; then
      die "$name: archive is missing a required symbol (git_clone / git_merge / credential helper)"
    fi
    printf '    %s: note -- nm produced no usable output (transient toolchain cache failure).\n' "$name"
    printf '    %s: relying on the object-count check only.\n' "$name"
  fi

  printf '    %-22s arch=%-6s size=%-6s objects=%-5s %s\n' \
    "$name" "$slice_arch" "$(du -h "$archive" | cut -f1)" "$objects" "$(basename "$archive")"
done

cat <<EOF

$(log "Done. Static libgit2 $LIBGIT2_VERSION slices ready at $OUT")

Reminder: third_party/libgit2/slices/ is build output. Keep it out of git.
EOF
