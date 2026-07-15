# AI-Powered Test Generation Engine

> Analyzes GitHub PR diffs, generates JUnit 5 tests via Anthropic Claude, validates them in-process, and opens a pull request with the results — automatically.

**Status:** Week 1 complete (v0.1) — diff analysis → LLM test generation; validation + GitHub integration coming in Week 2

---

## What It Does

1. **Receives** a GitHub `pull_request` webhook and validates the HMAC-SHA256 signature
2. **Analyzes** the diff with JavaParser to extract changed methods and assemble a rich generation context (full source, existing test file, project conventions)
3. **Generates** a JUnit 5 test class via LangChain4j + Anthropic Claude, compiles and executes it in-process to validate it
4. **Delivers** the tests by creating a `testgen/{source-branch}-{id}` branch, committing the test file, opening a PR against the source branch, and posting a link comment on the original PR

---

## Architecture

```mermaid
flowchart TD
    GH["GitHub PR Event<br/>(open / synchronize)"]
    GH -->|"POST /webhook/github"| WH

    subgraph App["Spring Boot App"]
        direction TB
        WH["<b>GitHubWebhookHandler</b><br/><i>validates HMAC-SHA256, 401 if invalid</i>"]
        WH --> DA["<b>DiffAnalyzer</b><br/><i>extracts changed methods (JavaParser AST)</i>"]
        DA --> CA["<b>ContextAssembler</b>"]

        CA -->|"Tier 1"| GCF["<b>GitHubContentsFetcher</b><br/><i>full source, existing test, up to 3 deps</i>"]
        CA -->|"Tier 2"| PCR["<b>ProjectConventionsRepository</b><br/><i>cached; re-analyzed if absent or stale (7+ days)</i>"]
        CA --> TGS["<b>TestGenerationService</b>"]

        TGS -->|"generate"| LLM["<b>LlmProvider</b><br/><i>Anthropic Claude</i>"]
        TGS --> TV["<b>TestValidator</b><br/><i>compiles + executes in-process</i>"]
        TV --> S3S["<b>S3TestArtifactStore</b>"]
        S3S -->|"upload"| S3["S3"]
        S3S --> DDB["<b>DynamoDbTestRepository</b>"]
        DDB -->|"persist"| DYN["DynamoDB"]
        DDB --> GPC["<b>GitHubPrCreator</b><br/><i>branch → commit → PR → comment</i>"]
    end

    GPC -->|"test PR + notification comment"| DONE["Engineer sees<br/>generated tests"]

    classDef external fill:#eef2f7,stroke:#8899aa,color:#334;
    class GCF,PCR,LLM,S3,DYN,GH,DONE external;
```

See [`docs/architecture.md`](docs/architecture.md) for the full component breakdown.

---

## How It Works (Week 1 — Current State)

The following pipeline is fully implemented and tested. GitHub integration, validation, and persistence land in Week 2.

1. **Parse** — `DiffParser` turns a GitHub unified diff string into `FileDiff` records, extracting file paths and hunk line ranges for `.java` files only
2. **Analyze** — `DiffAnalyzer` uses `SourceAnalyzer` (JavaParser AST) to correlate changed hunk line ranges to method signatures, producing a `List<ChangedMethod>`
3. **Assemble prompt** — `TestGenerationPromptBuilder` builds a system prompt ("expert Java test engineer, return only valid Java") and a user prompt containing the class name, changed method signatures, return types, and annotations
4. **Generate** — `TestGenerationService` calls the active `LlmProvider` (Anthropic Claude in production; `NoopProvider` in all tests), strips markdown code fences from the response, and extracts the class name via a lookahead regex
5. **Write** — The generated JUnit 5 source is written to `tmp/generated-tests/{ClassName}.java` and returned as an immutable `GeneratedTest` record (`className`, `packageName`, `testCode`, `savedPath`, `generatedAt`)

The active LLM provider is selected at startup via `testgen.llm.provider` in `application.yml` — switching between Anthropic and OpenAI requires only a config change, not a code change (sealed `LlmProvider` interface, ADR-002).

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 21 (virtual threads via Project Loom) |
| Framework | Spring Boot 3.x (`RestClient`, `@ConfigurationProperties` records) |
| LLM | LangChain4j + Anthropic Claude (sealed `LlmProvider` interface) |
| Source analysis | JavaParser (`javaparser-core`) |
| Test generation targets | JUnit 5 + Mockito |
| Persistence | DynamoDB Enhanced Client (metadata) + S3 (test artifacts) |
| Compute | ECS Fargate + Application Load Balancer |
| IaC | Terraform (all AWS resources); LocalStack for local iteration |
| Local dev | Docker Compose v2 (`docker compose up` = LocalStack; `--profile full` adds the app) |
| CI/CD | GitHub Actions (build + test on PR; deploy to ECS on merge to `main`) |

---

## Prerequisites

- **Java 21 LTS** — verify with `java -version`
- **Maven 3.9+** — verify with `mvn -version`
- **Docker Desktop** — required for LocalStack and integration tests
- **AWS CLI v2** — for ECR login and ECS deploy commands
- **Terraform** (latest stable) — for infra provisioning
- **IntelliJ IDEA** (Community or Ultimate)
- **smee.io channel URL** — free webhook proxy for local development; create one at [smee.io](https://smee.io)
- **Anthropic API key** — with a **$10/month hard spend cap** set in the [Anthropic console](https://console.anthropic.com)
- **GitHub App** — installed on your test target repository (configured on Day 12)

---

## Quickstart

```bash
# 1. Copy and fill in your Anthropic API key
cp .env.example .env        # then set LLM_API_KEY=sk-ant-...

# 2. Run all tests (unit + integration; NoopProvider — no API key required)
./mvnw verify

# 3. Run a single test class
./mvnw -Dtest=DiffParserTest test

# 4. Start the app (Week 2: requires LocalStack for DynamoDB/S3)
./mvnw spring-boot:run
```

> **Note:** Docker Compose and LocalStack are added in Week 2 (Day 9). Until then, `./mvnw verify` is the primary dev loop.

---

## Architecture Decision Records

| ADR | Decision |
|-----|---------|
| [ADR-001](docs/adr/ADR-001-compute.md) | ECS Fargate + ALB for compute |
| [ADR-002](docs/adr/ADR-002-llm-provider.md) | Anthropic Claude first, sealed `LlmProvider` interface |
| [ADR-003](docs/adr/ADR-003-test-storage.md) | DynamoDB for metadata, S3 for test artifacts |
| [ADR-004](docs/adr/ADR-004-local-aws.md) | LocalStack for all local AWS iteration |
| [ADR-005](docs/adr/ADR-005-source-analysis.md) | JavaParser for source and convention analysis |
