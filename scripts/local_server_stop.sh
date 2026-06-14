#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PID_FILE=".local/sword-forge-server.pid"

if [[ -f "$PID_FILE" ]]; then
  server_pid="$(cat "$PID_FILE")"
  if [[ -n "$server_pid" ]] && kill -0 "$server_pid" 2>/dev/null; then
    echo "Stopping Sword Forge server pid $server_pid..."
    kill "$server_pid"
    for _ in $(seq 1 20); do
      if ! kill -0 "$server_pid" 2>/dev/null; then
        break
      fi
      sleep 0.5
    done
    if kill -0 "$server_pid" 2>/dev/null; then
      echo "Server did not stop gracefully; forcing pid $server_pid."
      kill -9 "$server_pid" 2>/dev/null || true
    fi
  else
    echo "No running server process found for pid file."
  fi
  rm -f "$PID_FILE"
else
  echo "No local server pid file found."
fi

if [[ "${1:-}" == "--with-infra" ]]; then
  if command -v docker >/dev/null 2>&1; then
    docker compose down
  else
    echo "Docker is not installed; no compose infrastructure to stop."
  fi
fi
