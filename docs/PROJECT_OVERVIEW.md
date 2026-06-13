# Sword Forge Backend Overview

## Goal

Build a server for the sword-forging game with persistent saves, enhancement logic, evolution, item handling, logs, and balancing data.

## Stack

- Java 17
- Spring Boot 3.4
- Spring Web, Validation, Security
- Spring Data JPA
- Springdoc OpenAPI / Swagger UI
- Flyway
- PostgreSQL in production
- H2 for tests
- Docker Compose for local infrastructure

## Current Backend Scope

- Health check endpoint
- Weapon catalog endpoints
- Stored weapon equip endpoint
- Stored weapon lock and unlock endpoints
- Save load and save upsert endpoints
- Enhancement preview and attempt endpoints
- Shop preview and purchase endpoints
- Weapon sale preview and sale endpoints
- Evolution preview and attempt endpoints
- Special item catalog, grant, consume, and purchase endpoints
- Enhancement logs and reward logs endpoints with pagination and filters
- Economy ledger log endpoint with signed source/sink entries
- Enhancement probability disclosure endpoint
- Grade-bound enhancement, evolution-only grade upgrades, grade-floor protection, and pity stacks
- Gold economy for enhancement fees, weapon sale, and special-item purchases
- Optimistic save locking for concurrent mutation defense
- Audit details on enhancement attempt logs
- Basic Auth runtime guard, enabled outside tests
- CSRF token endpoint for runtime mutation requests
- Swagger UI at `/swagger-ui.html`

## Game Data Files

- `src/main/resources/data/weapons.json`
- `src/main/resources/data/enhance_table.json`
- `src/main/resources/data/failure_rewards.json`
- `src/main/resources/data/weapon_purchase_costs.json`
- `src/main/resources/data/weapon_sale_prices.json`
- `src/main/resources/data/enhance_costs.json`
- `src/main/resources/data/item_purchase_prices.json`
- `src/main/resources/data/evolution_requirements.json`
- `src/main/resources/data/special_items.json`
- `src/main/resources/data/image_prompts.json`

## Validation and Balance Tools

- `scripts/validate_data_schemas.py`
- `scripts/simulate_balance.py`
- JSON schema drafts under `schemas/`
- Simulation output under `outputs/` is ignored by git except `.gitkeep`

## Branch Policy

- `dev` is the integration branch.
- Feature work should merge into `dev` first.
- `main` should only receive changes that have already passed through `dev`.
- Direct commits to `main` are avoided.

## Current Artifacts

- API documentation under `docs/API_SPEC.md`
- Rules under `docs/SYSTEM_RULES.md`
- QA checklist under `docs/QA_CHECKLIST.md`
- Economy notes under `docs/ECONOMY_DESIGN.md`

## GitHub Automation

- `.github/workflows/ci.yml`: test and schema validation on pushes and PRs.
- `.github/workflows/balance.yml`: balance simulator on PRs and manual runs.
- `.github/workflows/codeql.yml`: CodeQL analysis for Java/Kotlin.
- `.github/workflows/pr-review.yml`: dependency review and automated PR checklist comment.
- `.github/workflows/auto-pr.yml`: creates or reuses a PR from `codex/**` branches into `dev`.
- `.github/pull_request_template.md`: PR checklist for gameplay, economy, migration, and API review.
- Auto PR requires either the repository workflow permission to create pull requests or a
  `PR_AUTOMATION_TOKEN` repository secret with pull-request write permission.
- Dependency review is advisory until GitHub Dependency graph is enabled for the repository.
