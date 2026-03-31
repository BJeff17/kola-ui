#!/usr/bin/env bash
echo "JAVAC=$(which javac)"
javac -version
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$ROOT_DIR/build-modules"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

javac -parameters --module-source-path "$ROOT_DIR/src" -d "$BUILD_DIR" $(find "$ROOT_DIR/src/kola.ui" -name '*.java')
jar --create --file "$ROOT_DIR/kola-ui.jar" -C "$BUILD_DIR/kola.ui" .

echo "kola-ui.jar rebuilt successfully"
