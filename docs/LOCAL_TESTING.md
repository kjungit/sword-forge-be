# Local Server Testing

Use this guide before deployment or before handing the backend to the frontend.

## Prerequisites

- Java 17
- Python 3
- `curl`
- Docker only if you want the Postgres-backed mode

## Quick Start Without Docker

This is the fastest local check. It uses the `local` Spring profile and a persistent H2 database under `.local/`.

```bash
cp .env.example .env
./scripts/local_server_start.sh
python3 scripts/smoke_api.py
```

Useful URLs:

```text
Health:   http://127.0.0.1:8080/api/v1/health
Swagger:  http://127.0.0.1:8080/swagger-ui.html
OpenAPI:  http://127.0.0.1:8080/v3/api-docs
```

Default local credentials:

```text
user:     local_user
password: local_password
```

The default player save id is also `local_user`. Override it with `SWORD_FORGE_USER_ID` or `python3 scripts/smoke_api.py --user-id <id>` only when the authenticated user is allowed to access that save. With the default security settings, the player user id must match the Basic Auth username.

Stop the server:

```bash
./scripts/local_server_stop.sh
```

Reset local H2 data:

```bash
./scripts/local_server_stop.sh
rm -rf .local
```

## Postgres Mode

Use this when Docker is available and you want a setup closer to deployment.

```bash
cp .env.example .env
SWORD_FORGE_DB=postgres ./scripts/local_server_start.sh
python3 scripts/smoke_api.py
```

Stop the server and Docker infrastructure:

```bash
./scripts/local_server_stop.sh --with-infra
```

The compose service uses:

```text
database: sword_forge
user:     sword_forge
password: sword_forge
port:     5432
```

## Smoke Test Coverage

`scripts/smoke_api.py` verifies:

- `GET /api/v1/health`
- public OpenAPI JSON
- `GET /api/v1/security/csrf`
- authenticated weapon catalog read
- save load/create for `local_user`
- normal-grade enhance preview with `requiredItems=[]`
- sale preview with `sellGold >= investedGold * 2`
- CSRF-protected idle claim mutation

Run one real enhancement attempt as well:

```bash
python3 scripts/smoke_api.py --with-enhance-attempt
```

That option mutates the local save and consumes gold, so use it when you are comfortable changing the local DB state.
The smoke test is safe to repeat; it accepts the current local save state after the first run.

## Manual CSRF Check

The smoke script handles CSRF automatically. For manual checks:

```bash
TOKEN=$(curl -s http://127.0.0.1:8080/api/v1/security/csrf \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["token"])')

curl -s -u local_user:local_password \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: ${TOKEN}" \
  -H "Cookie: XSRF-TOKEN=${TOKEN}" \
  -d '{"userId":"local_user"}' \
  http://127.0.0.1:8080/api/v1/idle/claim
```

## Frontend Environment

Use the same values in the Godot frontend:

```bash
export SWORD_FORGE_API_BASE_URL=http://127.0.0.1:8080/api/v1
export SWORD_FORGE_API_USER=local_user
export SWORD_FORGE_API_PASSWORD=local_password
export SWORD_FORGE_USER_ID=local_user
```

If you change `SERVER_PORT`, update `SWORD_FORGE_API_BASE_URL` as well.

After the backend smoke passes, run the Godot live smoke from the frontend repo:

```bash
cd /Users/jun/Documents/sword-forge-fe
/Applications/Godot.app/Contents/MacOS/Godot --headless --path . --script res://tests/live_backend_smoke.gd
```

## Pre-Deployment Checklist

Run these before a release candidate:

```bash
./gradlew test
python3 scripts/validate_data_schemas.py
python3 scripts/simulate_balance.py --runs 300 --max-attempts 1000
./scripts/local_server_start.sh
python3 scripts/smoke_api.py
```

Then confirm:

- Swagger loads.
- The frontend can fetch CSRF and make one mutation.
- `APP_CORS_ALLOWED_ORIGINS` is restricted to real frontend origins in deployment.
- `SPRING_SECURITY_USER_PASSWORD` is not a local default password in deployment.
- The deployment database uses Flyway migrations and is not using the H2 `local` profile.

## Troubleshooting

- Port conflict: set `SERVER_PORT=8081` and run the start script again.
- Server logs: check `.local/sword-forge-server.log`.
- Stale server process: run `./scripts/local_server_stop.sh`.
- Docker missing: use the default `SWORD_FORGE_DB=h2` mode.
- Bad CSRF: call `GET /api/v1/security/csrf` again and retry with both the returned header and `Cookie: XSRF-TOKEN=<token>`.
