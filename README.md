# Sword Forge Server

`Sword Forge` is a backend scaffold for the sword-forging game.

## Stack

- Java 17
- Spring Boot 3
- PostgreSQL
- Flyway
- Docker
- Swagger UI / OpenAPI

## Current scope

- REST API foundation
- health check endpoint
- shared API response and error handling
- database migration scaffold
- local database containers
- weapon, equip, save, enhance, shop, evolution, item, and log APIs
- Basic Auth guard for non-test runtime
- Swagger UI and OpenAPI JSON
- paginated log queries
- signed economy ledger queries
- player-facing enhancement probability disclosure
- GitHub Actions for CI, balance simulation, CodeQL, dependency review, and auto PR creation
- JSON schema validation script for game data
- balance simulator and cross-file data checks
- grade-floor protection, pity stacks, and evolution-only grade upgrades
- stored weapon equip and sale lock controls
- gold economy for enhancement fees, weapon sale, and item purchases
- optimistic locking and enhancement audit details
- authenticated player requests are bound to their own `userId`
- direct save upsert and item grant endpoints are admin-only at runtime

## Local API docs

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Runtime security

Runtime API security is enabled by default. Set credentials with:

- `SPRING_SECURITY_USER_NAME`
- `SPRING_SECURITY_USER_PASSWORD`

Set `APP_SECURITY_ENABLED=false` only for local throwaway runs or test profiles.

## GitHub Actions

- `CI`: runs Java tests and game-data schema validation.
- `Balance Simulation`: runs the balance simulator and uploads the summary artifact.
- `CodeQL`: runs Java security analysis.
- `PR Review Checks`: runs dependency review and posts a review checklist comment.
- `Auto PR`: opens or reuses a PR from `codex/**` branches into `dev`.

`Auto PR` uses `GITHUB_TOKEN` by default. For that token to create PRs, enable
`Settings > Actions > General > Workflow permissions > Allow GitHub Actions to create and approve pull requests`.
If that setting is unavailable or you want a dedicated automation identity, add a repository secret named
`PR_AUTOMATION_TOKEN` with pull-request write permission.

## Project structure

- `src/main/java`: application source
- `src/main/resources`: config and migrations
- `docs`: design and API notes
- `scripts`: utility scripts
- `data`: game balancing data drafts
- `schemas`: JSON schema drafts
- `docs/PROJECT_OVERVIEW.md`: project and branch policy summary
- `docs/PROBABILITY_RESEARCH.md`: probability/random reward research notes

## Next steps

1. Replace basic runtime authentication with player/admin identity flows.
2. Tune progression balance and material yields from simulation output.
3. Add deployment profiles and production secret management.
4. Add production monitoring and rate limiting before public launch.
