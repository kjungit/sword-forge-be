#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

mkdir -p .local

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

SERVER_PORT="${SERVER_PORT:-8080}"
SWORD_FORGE_DB="${SWORD_FORGE_DB:-h2}"
APP_SECURITY_ENABLED="${APP_SECURITY_ENABLED:-true}"
SPRING_SECURITY_USER_NAME="${SPRING_SECURITY_USER_NAME:-local_user}"
SPRING_SECURITY_USER_PASSWORD="${SPRING_SECURITY_USER_PASSWORD:-local_password}"

PID_FILE=".local/sword-forge-server.pid"
LOG_FILE=".local/sword-forge-server.log"

if [[ -f "$PID_FILE" ]]; then
  existing_pid="$(cat "$PID_FILE")"
  if [[ -n "$existing_pid" ]] && kill -0 "$existing_pid" 2>/dev/null; then
    echo "Sword Forge server is already running on pid $existing_pid."
    echo "Health: http://127.0.0.1:${SERVER_PORT}/api/v1/health"
    exit 0
  fi
  rm -f "$PID_FILE"
fi

if [[ "$SWORD_FORGE_DB" == "postgres" ]]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo "SWORD_FORGE_DB=postgres requires Docker, but docker was not found." >&2
    echo "Use SWORD_FORGE_DB=h2 for the no-Docker local profile." >&2
    exit 1
  fi

  docker compose up -d postgres redis

  POSTGRES_DB="${POSTGRES_DB:-sword_forge}"
  POSTGRES_USER="${POSTGRES_USER:-sword_forge}"
  POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-sword_forge}"
  POSTGRES_PORT="${POSTGRES_PORT:-5432}"
  SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-default}"
  SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:${POSTGRES_PORT}/${POSTGRES_DB}}"
  SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-$POSTGRES_USER}"
  SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$POSTGRES_PASSWORD}"
else
  SPRING_PROFILES_ACTIVE="local"
  SPRING_DATASOURCE_URL="jdbc:h2:file:./.local/sword_forge_local;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
  SPRING_DATASOURCE_USERNAME="sa"
  SPRING_DATASOURCE_PASSWORD=""
fi

export SERVER_PORT
export APP_SECURITY_ENABLED
export SPRING_SECURITY_USER_NAME
export SPRING_SECURITY_USER_PASSWORD
export SPRING_PROFILES_ACTIVE
export SPRING_DATASOURCE_URL
export SPRING_DATASOURCE_USERNAME
export SPRING_DATASOURCE_PASSWORD
export SPRING_DOCKER_COMPOSE_ENABLED=false

echo "Starting Sword Forge server..."
echo "  profile: ${SPRING_PROFILES_ACTIVE}"
echo "  db mode: ${SWORD_FORGE_DB}"
echo "  port: ${SERVER_PORT}"
echo "  log: ${ROOT_DIR}/${LOG_FILE}"

./gradlew bootJar

jar_file="$(find build/libs -maxdepth 1 -type f -name "*.jar" ! -name "*-plain.jar" | head -n 1)"
if [[ -z "$jar_file" ]]; then
  echo "Could not find a boot jar under build/libs." >&2
  exit 1
fi

nohup java -jar "$jar_file" >"$LOG_FILE" 2>&1 &
server_pid="$!"
echo "$server_pid" >"$PID_FILE"

health_url="http://127.0.0.1:${SERVER_PORT}/api/v1/health"

for _ in $(seq 1 90); do
  if curl -fsS "$health_url" >/dev/null 2>&1; then
    echo "Server is ready: $health_url"
    exit 0
  fi

  if ! kill -0 "$server_pid" 2>/dev/null; then
    echo "Server process exited before becoming healthy. Recent log:" >&2
    tail -n 120 "$LOG_FILE" >&2 || true
    rm -f "$PID_FILE"
    exit 1
  fi

  sleep 1
done

echo "Timed out waiting for server health. Recent log:" >&2
tail -n 120 "$LOG_FILE" >&2 || true
exit 1
