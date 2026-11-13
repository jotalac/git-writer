#!/usr/bin/env bash
# Build native Arch Linux .pkg.tar.zst and a portable .tar.zst.
# Run from the repository root, inside archlinux:latest mounted as /src.
set -euo pipefail

cd /src
version="$(sed -n 's/^app\.version\.number=//p' gradle.properties | head -n 1 | tr -d '\r[:space:]')"
test -n "$version" || { echo "Missing app.version.number" >&2; exit 1; }
pkgver="${version//-/.}"
[[ "$pkgver" =~ ^[0-9][a-zA-Z0-9._+]*$ ]] || { echo "Invalid pkgver: $pkgver" >&2; exit 1; }

pacman -Syu --noconfirm --needed \
  base-devel git jdk21-openjdk zip unzip zstd \
  gtk3 alsa-lib libx11 libxext libxrender libxtst \
  libxi libxrandr fontconfig freetype2

uid="$(stat -c %u /src)"
useradd --create-home --uid "$uid" builder
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
runuser -u builder -- env JAVA_HOME="$JAVA_HOME" \
  ./gradlew :desktopApp:createReleaseDistributable --no-daemon "-Papp.version.number=$version"

launcher="$(find desktopApp/build/compose/binaries -type f -path '*/app/*/bin/GitWriter' -print -quit)"
test -n "$launcher" || { echo "Could not find Compose GitWriter app-image launcher" >&2; exit 1; }
image_dir="$(dirname "$(dirname "$(realpath "$launcher")")")"
test -d "$image_dir/lib" || { echo "Invalid application image at $image_dir" >&2; exit 1; }

mkdir -p _archpkg dist
cat > _archpkg/PKGBUILD <<'PKGBUILD'
pkgname=git-writer
pkgver=0.0.0
pkgrel=1
pkgdesc='Minimalistic Git-backed Markdown note-taking application'
arch=('x86_64')
url='https://github.com/jotalac/git-writer'
license=('GPL-3.0-only')
depends=('glibc' 'libx11' 'libxext' 'libxrender' 'libxtst' 'libxi' 'libxrandr' 'fontconfig' 'freetype2' 'alsa-lib')
options=('!debug' '!strip')

package() {
  install -d "$pkgdir/opt/git-writer"
  cp -a "$APP_IMAGE/." "$pkgdir/opt/git-writer/"

  install -d "$pkgdir/usr/bin"
  ln -s /opt/git-writer/bin/GitWriter "$pkgdir/usr/bin/git-writer"

  install -Dm644 /src/desktopApp/launcher_icons/icon.png \
    "$pkgdir/usr/share/icons/hicolor/256x256/apps/git-writer.png"
  install -Dm644 /src/LICENSE "$pkgdir/usr/share/licenses/git-writer/LICENSE"
  install -d "$pkgdir/usr/share/applications"
  cat > "$pkgdir/usr/share/applications/git-writer.desktop" <<'DESKTOP'
[Desktop Entry]
Type=Application
Name=Git Writer
Comment=Git-backed Markdown note-taking app
Exec=/opt/git-writer/bin/GitWriter
Icon=git-writer
Terminal=false
Categories=Office;TextEditor;
StartupWMClass=GitWriter
DESKTOP
}
PKGBUILD

sed -i "s/^pkgver=.*/pkgver=$pkgver/" _archpkg/PKGBUILD
chown -R builder:builder _archpkg
runuser -u builder -- env APP_IMAGE="$image_dir" \
  bash -c 'cd /src/_archpkg && makepkg --force --noconfirm --nodeps'

cp _archpkg/*.pkg.tar.zst dist/

portable="GitWriter-${version}-linux-x86_64"
mkdir -p "dist/$portable"
cp -a "$image_dir/." "dist/$portable/"
tar -I 'zstd -T0 -10' -cf "dist/$portable.tar.zst" -C dist "$portable"
rm -rf "dist/$portable"

ls -lh dist/*.zst
