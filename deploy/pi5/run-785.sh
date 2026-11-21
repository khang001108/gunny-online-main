#!/usr/bin/env bash
set -euo pipefail
task_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
export GUNNY_ASSETS="${GUNNY_ASSETS:-$task_root/backend/assets}"
export GUNNY_PUBLIC="${GUNNY_PUBLIC:-$task_root/public}"
export GUNNY_TRIBAL_CONFIG="${GUNNY_TRIBAL_CONFIG:-$task_root/backend/tribal.properties}"
exec java -Xms32m -Xmx256m -Djava.awt.headless=true -jar "$task_root/gunny-server-7.8.5.jar"
