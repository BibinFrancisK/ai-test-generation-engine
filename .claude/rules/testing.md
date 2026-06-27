# Testing Rules

- Source of truth: `plan/EXECUTION_PLAN.md`.

---

## Test Commands

| Command | Scope | When to Run |
|---------|-------|-------------|
| `./mvnw test` | Unit tests only | Always; fast feedback |
| `./mvnw verify` | Unit + integration (Testcontainers) | Before every PR |
| `./mvnw -Dtest=<ClassName> test` | Single test class | Debugging a specific failure |

---

## Test Pyramid

```
         /\
        /e2e\          WebhookFlowIT — real webhook payload → full pipeline → PR in test repo
       /------\
      /   IT   \       TestGenerationOrchestratorIT, WebhookControllerIT (DynamoDB Local + LocalStack S3)
     /----------\
    /   Unit     \    DiffParserTest, DiffAnalyzerTest, ContextAssemblerTest,
   /--------------\   TestGenerationServiceTest, WebhookSignatureValidatorTest,
                       GitHubAppAuthenticatorTest, TestCompilerTest
```

---

## Coverage Targets by Layer

| Subject | Type | Target | Rationale |
|---------|------|--------|-----------|
| `DiffParser` / `DiffAnalyzer` | Unit | 90%+ | Pure parsing, fully deterministic |
| `WebhookSignatureValidator` | Unit | 95%+ | Security-critical; must not fail open |
| `ContextAssembler` | Unit | 80%+ | Two-tier coordination logic |
| `TestGenerationService` | Unit (Noop) | 70%+ | Mock LLM, test prompt building and record mapping |
| `TestCompiler` / `TestExecutor` | Unit | 80%+ | In-process compilation; deterministic on fixture inputs |
| `TestGenerationOrchestrator` | Unit | 80%+ | Pipeline order and collaborator calls |
| `GitHubAppAuthenticator` | Unit | 70%+ | JWT construction and token caching logic |
| `TestGenerationOrchestratorIT` | Integration | Happy path + compile failure → 422 |
| `WebhookFlowIT` | E2E | One full webhook → `testgen/` branch + PR created |

---

## Testcontainers Rules

- Use `GenericContainer("amazon/dynamodb-local:2.5.2")` with `-inMemory -sharedDb` for DynamoDB tests.
- Use `LocalStackContainer` (localstack/localstack, latest stable) for S3 tests.
- Always wire dynamic ports via `@DynamicPropertySource` — never hardcode `localhost:8000`.
- Containers declared `static` — reused across all tests in the class (startup cost paid once).

```java
@DynamicPropertySource
static void props(DynamicPropertyRegistry r) {
    r.add("aws.dynamodb.endpoint", () -> "http://localhost:" + ddb.getMappedPort(8000));
    r.add("aws.s3.endpoint", () -> localstack.getEndpointOverride(S3).toString());
}
```

---

## What NOT to Test

- Spring Boot auto-configuration wiring — it works by design.
- Anthropic / OpenAI API responses — not deterministic; always use `NoopProvider` in tests.
- GitHub API live responses — mock `GitHubContentsFetcher` and `GitHubPrCreator` in unit tests.
- DynamoDB Local internal behavior — test your code, not Amazon's library.
- Real file system paths from `JavaCompiler` — use in-memory file managers in `TestCompilerTest`.

---

## NoopProvider Rule

- All tests must run with `testgen.llm.provider=noop`.
- This is enforced in `src/test/resources/application-test.yml` — do not override in individual test classes.
- Zero real LLM API calls must be made during `./mvnw verify`.

---

## Test Naming Convention

- Test files: `<Subject>Test.java` (unit), `<Subject>IT.java` (integration/E2E).
- Method names: plain English describing behavior, not implementation.
- Use `@DisplayName` for non-obvious scenario descriptions.

```java
@Test
@DisplayName("returns 401 when HMAC-SHA256 signature is missing from request")
void rejectsWebhookWithoutSignature() { ... }
```

---

## Fixture Files

- `src/test/resources/fixtures/SampleService.java` — source file used by `DiffAnalyzer` and `ContextAssembler` tests.
- `src/test/resources/fixtures/SampleServiceTest.java` — existing test file for convention detection tests.
- `src/test/resources/fixtures/sample.diff` — unified diff fixture for `DiffParserTest`.
- `src/test/resources/fixtures/surefire-report.xml` — JUnit XML for `JUnitXmlReportParserTest`.

A parse failure on any fixture file is a breaking change — treat it as a test failure that must be fixed before merging.
