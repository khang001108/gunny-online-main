#!/usr/bin/env bash
set -euo pipefail
task_deploy_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
if [ "$#" -eq 0 ]; then
  exec python3 "$task_deploy_dir/update-gunny.py" menu
fi
exec python3 "$task_deploy_dir/update-gunny.py" "$@"
