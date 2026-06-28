# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

```bash
# Unit tests only (fast feedback)
./mvnw test

# Unit + integration tests via Testcontainers (run before every PR)
./mvnw verify

# Single test class
./mvnw -Dtest=DiffParserTest test

# Start the app locally (requires LocalStack running)
./mvnw spring-boot:run

# Start LocalStack (DynamoDB + S3 emulation)
docker compose up -d

# Full local environment including the app
docker compose --profile full up -d

# Provision local infra via Terraform against LocalStack
cd infra/terraform && terraform apply
```

> `pom.xml`, `docker-compose.yml`, and CI workflow are scaffolded on Day 2. Until then these commands will not work.

## Architecture

Webhook-driven pipeline: GitHub sends a `pull_request` event → the service analyzes the diff, assembles context, calls an LLM, validates the generated test in-process, and opens a pull request with the result.

**Full sequence:** `docs/architecture.md` — read this first when debugging the pipeline end-to-end.

**Two-tier context assembly (ADR-007)** is the most important design detail:
- Tier 1 — always-fresh from GitHub Contents API: full source of changed class + existing test file + up to 3 dependency sources
- Tier 2 — per-repo conventions from DynamoDB (`project-conventions` table), refreshed if >7 days old
- `ContextAssembler` combines both tiers into a single `GenerationContext` record passed to `TestGenerationService`

## Package Layout

All production code lives under `src/main/java/com/testgen/`. Each package has a single responsibility; see `.claude/rules/api-design.md` for the full table and cross-package dependency rules.

Key constraint: **`model/` imports nothing else** — it is pure data. `analysis/`, `generation/`, `validation/`, and `github/` must not import from each other.

## Domain Model

All domain objects are Java `record`s (immutable). Key records: `ChangedMethod`, `DiffHunk`, `FileDiff`, `GeneratedTest`, `GenerationContext`, `ProjectConventions`, `TestRun`. Records persisted to DynamoDB carry a `String schemaVersion` field.

Three sealed interfaces drive exhaustive `switch` expressions:
- `LlmProvider` — `AnthropicLlmProvider | OpenAiLlmProvider | NoopProvider`
- `ValidationResult` — `ValidationSuccess | CompilationFailure | ExecutionFailure`
- `HealingResult` — `HealingSuccess | HealingFailure`

Active LLM provider is selected by `testgen.llm.provider` in `application.yml` — never hardcoded.

## Testing

**`NoopProvider` is mandatory in all tests** — enforced by `src/test/resources/application-test.yml`. Zero real LLM calls during `./mvnw verify`.

Integration tests use Testcontainers:
- DynamoDB: `GenericContainer("amazon/dynamodb-local:2.5.2")` with `-inMemory -sharedDb`
- S3: `LocalStackContainer` (pinned version)
- Containers declared `static`; ports wired via `@DynamicPropertySource` — never hardcoded

Test naming: `<Subject>Test.java` (unit), `<Subject>IT.java` (integration/E2E).

Full coverage targets, fixture file list, and what NOT to test: `.claude/rules/testing.md`.

## Infrastructure

- **Compute:** ECS Fargate + ALB (ADR-001). Two IAM roles: task execution role (ECR + CloudWatch only) and task role (scoped S3/DynamoDB/SSM paths only).
- **Storage:** DynamoDB Enhanced Client for metadata; S3 for `.java` artifacts (private bucket, presigned URLs ≤1 h TTL). Object key pattern: `test-artifacts/{repositoryId}/{testRunId}/{ClassName}Test.java`.
- **Local emulation:** LocalStack via Docker Compose and Testcontainers (ADR-004).
- **IaC stubs:** `infra/terraform/` — 9 `.tf` files, fully implemented on Day 18.
- **Cost guard:** ALB costs ~$16/month. Run `terraform destroy -auto-approve` immediately after every demo session.

## GitHub App Integration

Authentication flow: RS256 JWT (10-min TTL) → `POST /app/installations/{id}/access_tokens` → installation token (cached, 1-hour TTL). `GitHubAppAuthenticator` owns the cache; all other GitHub classes call `getInstallationToken()`.

All GitHub API calls use Spring 6 `RestClient` with headers `Accept: application/vnd.github+json` and `X-GitHub-Api-Version: 2022-11-28`. On 404 from Contents API: return `Optional.empty()` — do not throw.

## Security Constraints

- Validate HMAC-SHA256 webhook signature **before** deserializing the payload; reject with 401 on mismatch.
- Never expose `ANTHROPIC_API_KEY`, private key material, or AWS credentials in any log line or API response.
- `max_tokens=1500` on every LLM call; $10/month hard spend cap set in the Anthropic console.
- Full security checklist: `.claude/skills/security-review/checklist.md`.

## Key Rules Reference

| Topic | File |
|-------|------|
| Package structure, records, sealed interfaces, REST/GitHub API conventions, error handling | `.claude/rules/api-design.md` |
| Test commands, pyramid, coverage targets, Testcontainers, fixture files | `.claude/rules/testing.md` |
| Security review checklist | `.claude/skills/security-review/checklist.md` |
| Architecture decisions (ADR-001 through ADR-005) | `docs/adr/` |
| Execution plan | `plan/EXECUTION_PLAN.md` (git-ignored; local only) |
