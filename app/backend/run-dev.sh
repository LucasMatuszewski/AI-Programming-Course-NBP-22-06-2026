#!/usr/bin/env bash
# run-dev.sh
# Reads .env from the repo root and starts the Spring Boot backend.
# Usage: cd app/backend && ./run-dev.sh
# Never hardcodes secrets — reads them from .env which is gitignored.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Find the main worktree root (works from both main repo and git worktrees)
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || true)"
# For git worktrees, the .env lives in the main repo root (one level above .git)
if [ -n "$REPO_ROOT" ]; then
  MAIN_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --git-common-dir 2>/dev/null | xargs dirname 2>/dev/null || echo "$REPO_ROOT")"
else
  MAIN_ROOT="$SCRIPT_DIR/../../.."
fi
ENV_FILE="$MAIN_ROOT/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: .env not found at $ENV_FILE. Copy .env.example and fill in your keys." >&2
  exit 1
fi

# Export KEY=VALUE pairs (skip comments and blank lines)
while IFS='=' read -r key value; do
  [[ "$key" =~ ^[[:space:]]*# ]] && continue
  [[ -z "$key" ]] && continue
  [[ "$key" =~ ^[[:space:]]*$ ]] && continue
  export "$key"="$value"
done < <(grep -E '^[A-Z_]+=' "$ENV_FILE")

echo "Starting Spring Boot backend with env from $ENV_FILE ..."
exec "$SCRIPT_DIR/mvnw" spring-boot:run
