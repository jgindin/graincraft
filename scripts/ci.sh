#!/usr/bin/env bash
# Everything CI runs, in the same order.
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew ktfmtCheck detekt
./gradlew :core:allTests :app:desktopTest verifyRoborazziDesktop koverVerify :app:packageDeb
