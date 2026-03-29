#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# Kill previous WorkSuite/Main java processes that can keep stale runs alive.
pkill -f 'apps.worksuite.WorkSuiteApp| Main ' 2>/dev/null || true

rm -rf output
mkdir -p output

javac -d output $(find src -name '*.java')

java -Dui.scroll.xFactor="${UI_SCROLL_X_FACTOR:-1.0}" \
	-Dui.scroll.yFactor="${UI_SCROLL_Y_FACTOR:-1.0}" \
	-Dui.scroll.latchMs="${UI_SCROLL_LATCH_MS:-220}" \
	-cp output apps.worksuite.WorkSuiteApp "${1:-0}"
