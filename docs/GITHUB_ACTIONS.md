# GitHub Actions Setup

This document explains the automation currently configured for the Sword Forge backend repository.

Korean version: `docs/GITHUB_ACTIONS_KO.md`

## Overview

| Workflow | File | Main purpose |
| --- | --- | --- |
| CI | `.github/workflows/ci.yml` | Run Java tests and validate game-data JSON. |
| Balance Simulation | `.github/workflows/balance.yml` | Run economy/progression simulation and upload balance artifacts. |
| CodeQL | `.github/workflows/codeql.yml` | Run static security analysis for Java/Kotlin code. |
| PR Review Checks | `.github/workflows/pr-review.yml` | Run dependency review and post/update an automated PR checklist comment. |
| Auto PR | `.github/workflows/auto-pr.yml` | Create or reuse a PR from `codex/**` branches into `dev`. |

Additional GitHub files:

- `.github/pull_request_template.md`: PR checklist for tests, data validation, balance review, migrations, and API docs.
- `.github/CODEOWNERS`: marks `@kjungit` as the code owner for all files.

## CI

File:

```text
.github/workflows/ci.yml
```

Runs on:

- Pushes to `main`
- Pushes to `dev`
- Pushes to `codex/**`
- Pushes to `feature/**`
- Pull requests targeting `main`
- Pull requests targeting `dev`

What it does:

1. Checks out the repository.
2. Sets up Java 17 using Temurin.
3. Sets up Python 3.12.
4. Makes `gradlew` executable.
5. Runs:

```bash
./gradlew test
```

6. Runs:

```bash
python3 scripts/validate_data_schemas.py
```

Why it exists:

- Catches Java/Spring regressions.
- Verifies controllers, services, concurrency behavior, security, and game flow tests.
- Ensures JSON data files still match schemas and cross-file references.

## Balance Simulation

File:

```text
.github/workflows/balance.yml
```

Runs on:

- Pull requests targeting `main`
- Pull requests targeting `dev`
- Manual `workflow_dispatch`

What it does:

1. Checks out the repository.
2. Sets up Python 3.12.
3. Runs:

```bash
python3 scripts/simulate_balance.py --runs 300 --max-attempts 1000
```

4. Appends `outputs/balance_summary.md` to the GitHub Actions step summary.
5. Uploads these artifacts:

```text
outputs/balance_result.csv
outputs/balance_summary.md
```

Why it exists:

- Checks whether the enhancement economy still lets players keep attempting.
- Gives reviewers actual source/sink and progression numbers.
- Makes economy tuning visible before merge.

## CodeQL

File:

```text
.github/workflows/codeql.yml
```

Runs on:

- Pushes to `main`
- Pushes to `dev`
- Pull requests targeting `main`
- Pull requests targeting `dev`
- Manual `workflow_dispatch`

What it does:

1. Checks out the repository.
2. Initializes CodeQL for `java-kotlin`.
3. Uses CodeQL autobuild.
4. Runs CodeQL analysis.

Why it exists:

- Finds security issues and dangerous code patterns.
- Publishes findings to GitHub code scanning.
- Helps catch security regressions around authentication, CSRF, data access, and request handling.

Required permission:

```yaml
security-events: write
```

## PR Review Checks

File:

```text
.github/workflows/pr-review.yml
```

Runs on:

- Pull requests targeting `main`
- Pull requests targeting `dev`

Jobs:

### Dependency review

Uses:

```text
actions/dependency-review-action@v5
```

Behavior:

- Reviews dependency changes in PRs.
- Runs with `continue-on-error: true`, so it is advisory until the repository dependency graph is fully enabled.

Recommended repository setting:

```text
Settings > Security > Code security and analysis > Dependency graph
```

### Post review checklist

Uses:

```text
actions/github-script@v9
```

Behavior:

- Posts an automated PR checklist comment.
- Updates the same comment on later runs instead of spamming duplicates.

The comment reminds reviewers to check:

- Java tests
- game-data schema validation
- balance simulation output
- dependency changes
- CodeQL security analysis
- gameplay intent
- economy tuning
- API compatibility
- migration safety

## Auto PR

File:

```text
.github/workflows/auto-pr.yml
```

Runs on:

- Pushes to `codex/**`

What it does:

1. Checks whether an open PR already exists from the pushed branch to `dev`.
2. If one exists, it prints the PR URL and exits successfully.
3. If no PR exists, it tries to create one with:

```text
base: dev
head: codex/<branch>
title: Auto PR: <branch>
```

4. If GitHub token permissions do not allow PR creation, it leaves a warning and exits successfully.

Why it exists:

- Keeps Codex work branches flowing into `dev`.
- Avoids duplicate PRs for the same branch.
- Avoids failing the whole PR when GitHub repository settings block Actions from creating pull requests.

Recommended repository setting:

```text
Settings > Actions > General > Workflow permissions
Allow GitHub Actions to create and approve pull requests
```

Optional secret:

```text
PR_AUTOMATION_TOKEN
```

Use that secret when you want a dedicated automation identity with pull-request write permission.

Manual fallback:

```bash
gh pr create --base dev --head codex/<branch>
```

## PR Template

File:

```text
.github/pull_request_template.md
```

It asks each PR to confirm:

- Tests pass locally or in CI.
- Game data schema validation passes.
- Balance simulation was reviewed when economy/probability data changed.
- Migrations are safe for existing saves.
- API changes are reflected in docs.

Review focus:

- Gameplay rule changes
- Economy source/sink changes
- Security or user access changes
- Save-data compatibility

## CODEOWNERS

File:

```text
.github/CODEOWNERS
```

Current owner:

```text
* @kjungit
```

Meaning:

- All files are owned by `@kjungit`.
- GitHub can request or require review from that owner if branch protection is configured.

## Current Merge Flow

Recommended flow:

1. Work on a branch named `codex/<topic>` or `feature/<topic>`.
2. Push the branch.
3. For `codex/**`, Auto PR tries to open a PR into `dev`.
4. If Auto PR cannot create the PR because of repository permissions, create the PR manually.
5. PR checks run:
   - CI
   - Balance Simulation
   - CodeQL
   - PR Review Checks
6. Review the automated checklist comment.
7. Merge into `dev`.
8. Promote `dev` to `main` only after the integration branch is stable.

## What Is Not Configured Yet

These are intentionally not done yet:

- Production deployment workflow.
- Release tagging workflow.
- Database backup or migration approval workflow.
- Frontend Godot CI.
- Environment-specific secret rollout.
- Runtime monitoring or alerting.

Those should be added after the hosting target and deployment strategy are decided.
