# Sword Growth Backend Overview

## Goal

Build a server for the sword-enhancement game with persistent saves, enhancement logic, evolution, item handling, logs, and balancing data.

## Stack

- Java 21
- Spring Boot 3.4
- Spring Web, Validation, Security
- Spring Data JPA
- Flyway
- PostgreSQL in production
- H2 for tests
- Docker Compose for local infrastructure

## Current Backend Scope

- Health check endpoint
- Weapon catalog endpoints
- Save load and save upsert endpoints
- Enhancement preview and attempt endpoints
- Shop preview and purchase endpoints
- Evolution preview and attempt endpoints
- Special item catalog, grant, and consume endpoints
- Enhancement logs and reward logs endpoints

## Game Data Files

- `src/main/resources/data/weapons.json`
- `src/main/resources/data/enhance_table.json`
- `src/main/resources/data/failure_rewards.json`
- `src/main/resources/data/weapon_purchase_costs.json`
- `src/main/resources/data/evolution_requirements.json`
- `src/main/resources/data/special_items.json`
- `src/main/resources/data/image_prompts.json`

## Validation and Balance Tools

- `scripts/validate_data_schemas.py`
- `scripts/simulate_balance.py`
- JSON schema drafts under `schemas/`

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
