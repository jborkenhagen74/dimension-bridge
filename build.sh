#!/usr/bin/env bash
set -euo pipefail
GRADLE_BIN="${GRADLE_BIN:-gradle}"
"$GRADLE_BIN" clean collectReleaseArtifacts "$@"
