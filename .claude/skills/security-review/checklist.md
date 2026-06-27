# Security Review Checklist

Use this checklist when reviewing any PR. Mark each item PASS / FAIL / N/A with a note if failed.

---

## 1. Webhook Signature Validation

- [ ] `WebhookSignatureValidator` computes HMAC-SHA256 over the raw request body using the webhook secret — not over a parsed/re-serialized form
- [ ] Signature check happens **before** JSON deserialization — a missing or invalid `X-Hub-Signature-256` header returns 401 immediately
- [ ] The comparison uses a constant-time equality check (`MessageDigest.isEqual`) — not `String.equals()`, which is vulnerable to timing attacks
- [ ] The webhook secret is read from environment variable or SSM — never hardcoded
- [ ] Unrecognised event types (`X-GitHub-Event` not in the handled set) return 400, not 500

---

## 2. GitHub App Authentication

- [ ] `GITHUB_APP_PRIVATE_KEY` is read from SSM Parameter Store at startup — not present in any source file, property file, or log output
- [ ] `*.pem` files are listed in `.gitignore` — no private key material is committed
- [ ] The JWT is issued with a 10-minute expiry — the `exp` claim is set correctly
- [ ] Installation access tokens are cached and refreshed before their 1-hour expiry — no unnecessary token requests
- [ ] The installation token is not logged at any level (DEBUG, TRACE, or otherwise)

---

## 3. LLM Prompt Injection

- [ ] Source code content passed to the LLM is bounded in length (total prompt ≤2000 tokens) before `TestGenerationService` is called
- [ ] No raw webhook JSON payload string is interpolated directly into the system prompt
- [ ] `TestGenerationPromptBuilder` keeps the system instruction and user-supplied code context in separate prompt sections — never concatenated into a single string
- [ ] LLM response is treated as untrusted text and written as a `.java` source file — it is never `eval`'d or executed directly

---

## 4. In-Process Compilation Safety

- [ ] `TestCompiler` uses `javax.tools.JavaCompiler` with an in-memory `JavaFileManager` — compiled `.class` files are not written to a path on the production JVM's classpath
- [ ] The compiled class is isolated to a `URLClassLoader` with the system classloader as parent — it cannot access `sun.*` or internal JVM APIs beyond what the security manager allows
- [ ] `TestExecutor` runs the compiled test via the JUnit Platform Launcher — it does not use `Runtime.exec()` or `ProcessBuilder`
- [ ] Compilation errors are captured and returned as `CompilationFailure` — they are never logged with the full source content at ERROR level (only at DEBUG)

---

## 5. Secrets Handling

- [ ] `ANTHROPIC_API_KEY` is read from environment variable or SSM only — not present in any source file, log output, or API response body
- [ ] `GITHUB_APP_PRIVATE_KEY` is read from SSM only — not in `application.yml`, `application-*.yml`, or any committed property file
- [ ] `.env` and `.env.*` files are listed in `.gitignore` — only `.env.example` (with placeholder values) is committed
- [ ] No secret values appear in Micrometer metric tags, MDC fields, or structured log output
- [ ] `git grep -r -i "api.key\|secret\|password\|token\|private.key" -- src/` returns zero results

---

## 6. S3 Artifact Safety

- [ ] The S3 bucket for test artifacts is created with `block_public_access = true` in Terraform — no public-read ACL or bucket policy
- [ ] Generated test files are accessed via presigned URLs (TTL ≤1 hour) — not via public object URLs
- [ ] The S3 bucket name does not appear hardcoded in source — it is read from `aws.s3.bucket` config
- [ ] IAM task role has `s3:PutObject` and `s3:GetObject` on the specific artifact bucket only — not `s3:*` or wildcard resource

---

## 7. DynamoDB Write Safety

- [ ] `DynamoDbTestRepository` saves test runs keyed by `testRunId` — a duplicate webhook delivery for the same PR does not create a duplicate record
- [ ] DynamoDB endpoint URL is read from `aws.dynamodb.endpoint` config — never hardcoded (`localhost:8000` for local, blank for production uses default AWS endpoint)
- [ ] IAM task role has `dynamodb:PutItem`, `dynamodb:GetItem`, `dynamodb:UpdateItem` on specific table ARNs only — not `dynamodb:*`
- [ ] `ProjectConventionsRepository` updates `analyzedAt` on every refresh — stale conventions are always replaced, never silently retained

---

## 8. Dependency Hygiene

- [ ] No new Maven dependencies with known CVEs — check with `./mvnw dependency-check:check` or review manually
- [ ] Testcontainer images are pinned to exact versions — no `latest` tags in test infrastructure
- [ ] No use of `@SuppressWarnings("unchecked")` on security-relevant deserialization paths
- [ ] `FAIL_ON_UNKNOWN_PROPERTIES` is `false` on Jackson ObjectMapper — unknown fields in GitHub webhook payloads do not crash the service

---

## Risk Level Definitions

| Level | Description |
|-------|-------------|
| HIGH | Could lead to secret leakage, arbitrary code execution, prompt injection, or uncontrolled LLM spend |
| MEDIUM | Could lead to duplicate test runs, incorrect generation context, or unexpected API behavior |
| LOW | Minor hygiene issue; no immediate exploitability |
