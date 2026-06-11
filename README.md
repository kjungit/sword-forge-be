# Sword Growth Server

`Sword Growth` is a backend scaffold for the sword-enhancement game.

## Stack

- Java 21
- Spring Boot 3
- PostgreSQL
- Flyway
- Docker

## Current scope

- REST API foundation
- health check endpoint
- shared API response and error handling
- database migration scaffold
- local database containers
- weapon, save, enhance, shop, evolution, item, and log APIs
- JSON schema validation script for game data
- balance simulator and cross-file data checks

## Project structure

- `src/main/java`: application source
- `src/main/resources`: config and migrations
- `docs`: design and API notes
- `scripts`: utility scripts
- `data`: game balancing data drafts
- `schemas`: JSON schema drafts
- `docs/PROJECT_OVERVIEW.md`: project and branch policy summary

## Next steps

1. Add authentication and role-based access for admin endpoints.
2. Expand balance simulations with scenario presets.
3. Add pagination and filtering for log queries.
