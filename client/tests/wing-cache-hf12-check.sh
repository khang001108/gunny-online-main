#!/usr/bin/env bash
# Cache checks are included in the attachment integration test.
set -euo pipefail
exec bash "$(dirname -- "$0")/wing-pose-check.sh"
