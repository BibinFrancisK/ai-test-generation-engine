# Skill: Security Review

## Purpose

Perform a targeted security review of pending changes on the current branch against the threat model for this AI test generation service. Scope covers the application layer — webhook validation, GitHub App authentication, LLM integration, in-process test compilation, DynamoDB/S3 persistence, and secrets handling.

---

## When to Invoke

- Before opening a PR that touches `github/`, `generation/`, `validation/`, or `persistence/`
- When adding or changing webhook signature validation, GitHub App JWT logic, or installation token caching
- When modifying `TestGenerationPromptBuilder`, LLM provider wiring, or `ContextAssembler`
- When changing how secrets (`ANTHROPIC_API_KEY`, `GITHUB_APP_PRIVATE_KEY`) are read or used
- When modifying DynamoDB or S3 write paths
- When touching in-process Java compilation (`TestCompiler`) or test execution (`TestExecutor`)

---

## What This Skill Checks

See `checklist.md` in this directory for the full itemized list. At a high level:

1. **Webhook signature validation** — `WebhookSignatureValidator` verifies HMAC-SHA256 before payload deserialization; missing or invalid signature returns 401, never 500
2. **GitHub App JWT security** — private key is read from SSM Parameter Store only; the JWT is short-lived (10-min TTL); installation tokens are cached and refreshed before expiry
3. **LLM prompt injection** — source code and diff content passed to the LLM is bounded in size; no raw webhook JSON is interpolated into the system prompt
4. **Secrets handling** — `ANTHROPIC_API_KEY` and `GITHUB_APP_PRIVATE_KEY` are read from environment variables or SSM only; never logged, hardcoded, or returned in responses
5. **In-process compilation safety** — `TestCompiler` uses an in-memory file manager; compiled classes are not written to a path accessible to the production JVM's classpath
6. **S3 artifact safety** — generated test files are stored in S3 with private ACL; no public-read bucket policy; access via presigned URLs only
7. **DynamoDB write idempotency** — test runs are keyed by `testRunId`; duplicate webhook deliveries do not create duplicate test runs
8. **Dependency hygiene** — no new dependencies with known CVEs; `./mvnw dependency:check` is clean

---

## What This Skill Does NOT Cover

- Authentication / authorization of API callers — webhook auth is HMAC-only by design; `/api/v1/generate-tests` is internal
- HTTPS / TLS — terminated at the ALB in AWS; HTTP is acceptable locally
- AWS IAM policies — covered by `infra/terraform/iam.tf`; task role has least-privilege S3/DynamoDB/SSM paths only
- GitHub App installation permissions — scoped to repository contents (read) and pull requests (write) only; reviewed at App registration time

---

## How to Run

1. Check out the branch under review
2. Run `git diff main...HEAD --name-only` to identify changed files
3. Work through `checklist.md` item by item against the diff
4. Report each finding with: file, line, risk level (LOW / MEDIUM / HIGH), and recommended fix
5. If all items pass, confirm clean and summarize

---

## Output Format

```
## Security Review — <branch-name>

### Findings
| # | File | Line | Risk | Issue | Recommendation |
|---|------|------|------|-------|----------------|
| 1 | ... | ... | HIGH | ... | ... |

### Passed Checks
- [x] Secrets not hardcoded
- [x] HMAC validated before deserialization
- ...

### Verdict
PASS / FAIL — <one sentence summary>
```
