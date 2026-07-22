# Domain and API Design Rules

- Source of truth: `plan/EXECUTION_PLAN.md` and `docs/adr/`.

---

## Package Structure Rules

| Package | Responsibility |
|---------|---------------|
| `analysis/` | Diff parsing and AST analysis — `DiffParser`, `DiffAnalyzer`, `SourceAnalyzer`, `ProjectConventionsAnalyzer` |
| `api/` | Spring `@RestController` classes only — `TestGenerationController`, `WebhookController`, `DashboardController` |
| `config/` | Spring `@Configuration` and `@Bean` definitions — no business logic |
| `context/` | Two-tier context assembly — `ContextAssembler` |
| `dashboard/` | Dashboard aggregation — `CoverageAggregator` reads `TestRun` data via `persistence/` and computes `CoverageStats` |
| `generation/` | LLM abstraction and prompt building — `LlmProvider` (sealed), `AnthropicLlmProvider`, `OpenAiLlmProvider`, `TestGenerationService`, `TestGenerationPromptBuilder` |
| `github/` | All GitHub API calls — `WebhookSignatureValidator`, `GitHubWebhookHandler`, `GitHubContentsFetcher`, `GitHubAppAuthenticator`, `GitHubPrCreator` |
| `healing/` | Self-healing pipeline — `JUnitXmlReportParser`, `ChangeCorrelator`, `HealingTrigger`, `TestHealer`, `HealingOrchestrator` |
| `model/` | Immutable records and sealed interfaces — no business logic |
| `orchestration/` | Pipeline wiring — `TestGenerationOrchestrator` |
| `persistence/` | AWS SDK calls — `DynamoDbTestRepository`, `ProjectConventionsRepository`, `S3TestArtifactStore` |
| `validation/` | In-process compilation and execution — `TestCompiler`, `TestExecutor` |

- No circular dependencies. `model/` must not import from any other package. `analysis/`, `generation/`, `validation/`, and `github/` must not import from each other.
- `config/` holds only wiring — no `if` statements, no business logic.

---

## Domain Record Rules

- All domain objects are Java `record`s — immutable, no setters.
- Records live in `model/` unless they are request/response DTOs (those live alongside their controller in `api/`).
- No business logic inside records — records are pure data carriers.
- Records stored in DynamoDB must include a `String schemaVersion` field (e.g. `"v1"`).

Key records: `ChangedMethod`, `DiffHunk`, `FileDiff`, `GeneratedTest`, `GenerationContext`, `ProjectConventions`, `TestRun`.

---

## Sealed Interface Rules

- `LlmProvider` — permits `AnthropicLlmProvider`, `OpenAiLlmProvider`, `NoopProvider`.
- `ValidationResult` — permits `ValidationSuccess`, `CompilationFailure`, `ExecutionFailure`.
- `HealingResult` — permits `HealingSuccess`, `HealingFailure`.
- Every `switch` on a sealed type must be exhaustive — no `default` that silently ignores cases.
- Active `LlmProvider` implementation is selected at startup via `testgen.llm.provider` in `application.yml` — never hardcoded in business logic.
- `NoopProvider` must always be a permitted implementation — used in all tests to prevent real LLM calls.

---

## REST API Conventions

| Endpoint | Method | Success | Error codes |
|----------|--------|---------|-------------|
| `/api/v1/generate-tests` | POST | 201 `TestGenerationResponse` | 422 (compile failure), 500 (LLM/infra) |
| `/webhook/github` | POST | 200 | 401 (bad HMAC), 400 (unrecognised event) |
| `/api/v1/dashboard` | GET | 200 `DashboardResponse` | 500 |
| `/actuator/health` | GET | 200 | — |

- Never expose internal stack traces or exception messages in API response bodies.
- `TestGenerationResponse` must always include `testRunId` — even when generation fails (status = `FAILED`).
- Webhook handler: validate HMAC-SHA256 signature **before** deserializing the payload — reject with 401 on mismatch.

---

## GitHub API Conventions

- **Contents API** (read): `GET /repos/{owner}/{repo}/contents/{path}?ref={ref}` — response `content` field is Base64-encoded; always decode before use.
- **Git Refs API** (create branch): `POST /repos/{owner}/{repo}/git/refs`
- **Pulls API** (open PR): `POST /repos/{owner}/{repo}/pulls`
- **Issues Comments API** (notify): `POST /repos/{owner}/{repo}/issues/{number}/comments`
- **App auth**: RS256 JWT (10-min TTL) → `POST /app/installations/{id}/access_tokens` → installation token (1-hour TTL).
- `GitHubAppAuthenticator` caches the installation token and refreshes it before expiry — `GitHubContentsFetcher` and `GitHubPrCreator` both call `getInstallationToken()` at request time (cheap due to cache).
- Use `RestClient` (Spring 6) for all GitHub API calls — never `HttpURLConnection` or `OkHttp` directly.
- Set `Accept: application/vnd.github+json` and `X-GitHub-Api-Version: 2022-11-28` on every request.
- On 404 from Contents API: return `Optional.empty()` — do not throw.

---

## Two-Tier Context Assembly (ADR-007)

- `ContextAssembler.assemble(...)` always executes Tier 1 first, then Tier 2.
- Tier 1 (`GitHubContentsFetcher`): fetch full source, existing test file, up to 3 dependency sources — always fresh from GitHub per PR.
- Tier 2 (`ProjectConventionsRepository`): check DynamoDB; if absent or `analyzedAt` >7 days old, run `ProjectConventionsAnalyzer`, save result, use it.
- Result: `GenerationContext` record passed to `TestGenerationService`.
- Token budget: `TestGenerationPromptBuilder` must log estimated token count and keep total under 2000 tokens. `max_tokens=1500` on every LLM call.

---

## Error Handling Rules

- Webhook HMAC mismatch → return 401 immediately; log at WARN with `deliveryId` in MDC.
- LLM failure → circuit breaker (Resilience4j); persist `TestRun` with `status=FAILED`, `generatedTestCode=null`; return 500.
- Compile failure → return 422 with `ValidationResult` details; still persist `TestRun`.
- GitHub API 5xx → retry once with exponential backoff; on second failure, throw and return 500 to caller.
- Never swallow exceptions silently — always log with `traceId` and context before rethrowing or returning an error response.
- Never expose `ANTHROPIC_API_KEY`, private key material, or AWS credentials in any log line or API response.
