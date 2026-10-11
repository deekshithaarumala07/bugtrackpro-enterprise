#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # .env is a shell-compatible KEY=value file; keep it local and untracked.
  source "$ROOT_DIR/.env"
  set +a
fi
cd "$ROOT_DIR/backend"
exec mvn spring-boot:run
