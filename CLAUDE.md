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

# Start LocalStack (DynamoDB + S3 emulation)
docker compose up -d

# Start the app locally (requires LocalStack running)
./mvnw spring-boot:run

# Full local environment including the app
docker compose --profile full up -d
```

> **`infra/terraform/` targets real AWS, not LocalStack.** It was repointed during the ECS Fargate deploy work to provision the actual production infrastructure (ECS/ALB/ECR/DynamoDB/S3/IAM) and now has a real S3 remote-state backend — running `terraform apply` from a dev machine provisions real, billable AWS resources, not LocalStack ones. For local dev, LocalStack's DynamoDB tables and S3 bucket are created directly via the AWS CLI against `http://localhost:4566` (dummy credentials — LocalStack ignores them), matching the schemas in `infra/terraform/dynamodb.tf` and `s3.tf`. For an actual AWS deploy: `cd infra/terraform && terraform apply`, then `terraform destroy -auto-approve` immediately after — see `docs/cost-estimate.md`.

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
- **IaC:** `infra/terraform/` — 9 `.tf` files provisioning the full production stack (ECS, ALB, ECR, DynamoDB, S3, IAM); real AWS S3 backend for Terraform state.
- **CI/CD:** `.github/workflows/ci.yml` runs build + tests on every push/PR; `.github/workflows/deploy.yml` builds the image, pushes to ECR, and forces a new ECS deployment on merge to `main`.
- **Cost guard:** ALB costs ~$16/month, the one fixed cost that doesn't scale to zero. Run `terraform destroy -auto-approve` immediately after every demo session — see `docs/cost-estimate.md` for the full breakdown.

## GitHub App Integration

Authentication flow: RS256 JWT (10-min TTL) → `POST /app/installations/{id}/access_tokens` → installation token (cached, 1-hour TTL). `GitHubAppAuthenticator` owns the cache; all other GitHub classes call `getInstallationToken()`.

All GitHub API calls use Spring 6 `RestClient` with headers `Accept: application/vnd.github+json` and `X-GitHub-Api-Version: 2022-11-28`. On 404 from Contents API: return `Optional.empty()` — do not throw.

## Security Constraints

- Validate HMAC-SHA256 webhook signature **before** deserializing the payload; reject with 401 on mismatch.
- Never expose `ANTHROPIC_API_KEY`, private key material, or AWS credentials in any log line or API response.
- `max_tokens=1500` on every LLM call, enforced twice — `application.yml`'s `testgen.llm.max-tokens` and a hard ceiling (`Constants.HARD_MAX_TOKENS`) that `AnthropicLlmProvider` clamps to regardless of config, so a config-only mistake can't silently exceed it. $10/month hard spend cap set in the Anthropic console.
- All secrets live in SSM Parameter Store as `SecureString`s under `/testgen/`: `/testgen/llm-api-key`, `/testgen/github-app-private-key`, `/testgen/github-webhook-secret`, `/testgen/github-app-id`. Injected into the ECS task via the native `secrets` block (execution role resolves them at launch) and read at runtime by the app itself via Spring Cloud AWS (task role) — never as plaintext environment variables, never in source.
- Full security checklist: `.claude/skills/security-review/checklist.md`.

## Key Rules Reference

| Topic | File |
|-------|------|
| Package structure, records, sealed interfaces, REST/GitHub API conventions, error handling | `.claude/rules/api-design.md` |
| Test commands, pyramid, coverage targets, Testcontainers, fixture files | `.claude/rules/testing.md` |
| Security review checklist | `.claude/skills/security-review/checklist.md` |
| Architecture decisions (ADR-001 through ADR-007) | `docs/adr/` |
| Execution plan | `plan/EXECUTION_PLAN.md` (git-ignored; local only) |
| What I learned, cost breakdown | `docs/what-i-learned.md`, `docs/cost-estimate.md` |
