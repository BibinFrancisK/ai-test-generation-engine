# Architecture — AI-Powered Test Generation Engine

## Happy Path: PR Opened → Test PR Created

The sequence below shows what happens from the moment a developer opens a pull request to the moment a test PR appears in the repository.

```mermaid
sequenceDiagram
    autonumber
    actor Engineer
    participant GH as GitHub
    participant WC as WebhookController
    participant WH as GitHubWebhookHandler
    participant DA as DiffAnalyzer
    participant CA as ContextAssembler
    participant GCF as GitHubContentsFetcher
    participant PCR as ProjectConventionsRepository
    participant TGS as TestGenerationService
    participant LLM as LlmProvider
    participant TV as TestValidator
    participant S3 as S3TestArtifactStore
    participant DDB as DynamoDbTestRepository
    participant GPC as GitHubPrCreator

    Engineer->>GH: opens pull request
    GH->>WC: POST /webhook/github (pull_request event)
    WC->>WC: verify HMAC-SHA256 signature<br/>(401 if invalid)
    WC->>WH: route event payload
    WH->>DA: parse unified diff + extract changed methods
    DA->>CA: changedMethods + filePath + ref
    note over CA: Two-tier context assembly (ADR-007)
    CA->>GCF: fetch full source of changed class (Tier 1)
    GCF-->>CA: sourceCode
    CA->>GCF: find existing test file (Tier 1)
    GCF-->>CA: existingTestSource (Optional)
    CA->>GCF: fetch up to 3 dependency sources (Tier 1)
    GCF-->>CA: dependencySources
    CA->>PCR: load ProjectConventions by repositoryId (Tier 2)
    alt conventions absent or stale (> 7 days)
        PCR-->>CA: empty
        CA->>GCF: list src/test/java/ — pick up to 5 .java files
        CA->>CA: ProjectConventionsAnalyzer.analyze(testSources)
        CA->>PCR: save refreshed ProjectConventions
    else conventions fresh
        PCR-->>CA: ProjectConventions
    end
    CA->>TGS: GenerationContext
    TGS->>LLM: prompt (system: conventions, user: source + existing test + deps + diff)
    LLM-->>TGS: generated test source (String)
    TGS->>TV: compile + execute generated test in-process
    TV-->>TGS: ValidationResult (success / compile failure / execution failure)
    TGS->>S3: upload GeneratedTest.java artifact
    S3-->>TGS: s3ArtifactUrl
    TGS->>DDB: save TestRun (status, s3ArtifactUrl, validationResult)
    TGS->>GPC: GeneratedTest + metadata
    GPC->>GH: POST /repos/{owner}/{repo}/git/refs<br/>(create testgen/{source-branch}-{id} branch)
    GPC->>GH: PUT /repos/{owner}/{repo}/contents/{path}<br/>(commit test file to testgen branch)
    GPC->>GH: POST /repos/{owner}/{repo}/pulls<br/>(open PR: testgen branch → source branch)
    GPC->>GH: POST /repos/{owner}/{repo}/issues/{n}/comments<br/>(post link comment on source PR)
    GH-->>Engineer: test PR visible + notification comment on source PR
```

---

## Two-Tier Context Assembly (ADR-007)

The quality of generated tests depends entirely on the context given to the LLM. Passing only diff lines produces shallow tests that don't match project conventions or understand class structure.

**Tier 1 — always-fresh code (per PR, via GitHub Contents API):**
- Full source of the changed class — gives the LLM class structure, field types, and method signatures beyond what's in the diff
- Existing test file for that class (if one exists) — the single most valuable input; shows the LLM the project's exact testing style rather than describing it
- Up to 3 dependency source files (parsed from imports, filtered to project-local classes) — gives the LLM context on collaborator types

**Tier 2 — per-repository conventions (DynamoDB, refreshed if > 7 days old):**
- Test framework (`junit5` / `junit4`), mock library (`mockito` / `easymock` / `none`), base test class — detected by `ProjectConventionsAnalyzer` using JavaParser
- Stored in the `project-conventions` DynamoDB table keyed by `repositoryId`
- Regenerated automatically on first use and whenever the cache is stale

`ContextAssembler` coordinates both tiers and returns a single `GenerationContext` record to `TestGenerationService`.

---

## Component Table

| Package | Key Classes | Responsibility |
|---------|------------|----------------|
| `analysis/` | `DiffParser`, `DiffAnalyzer`, `SourceAnalyzer`, `ProjectConventionsAnalyzer` | Parse unified diffs and Java source ASTs |
| `api/` | `TestGenerationController`, `WebhookController`, `DashboardController` | HTTP entry points |
| `config/` | `AppConfig` | Spring `@Bean` wiring only |
| `context/` | `ContextAssembler` | Coordinate Tier 1 + Tier 2 into `GenerationContext` |
| `generation/` | `LlmProvider` (sealed), `AnthropicLlmProvider`, `OpenAiLlmProvider`, `NoopProvider`, `TestGenerationService`, `TestGenerationPromptBuilder` | LLM abstraction and prompt construction |
| `github/` | `WebhookSignatureValidator`, `GitHubWebhookHandler`, `GitHubContentsFetcher`, `GitHubAppAuthenticator`, `GitHubPrCreator` | All GitHub API interactions |
| `healing/` | `JUnitXmlReportParser`, `ChangeCorrelator`, `HealingTrigger`, `TestHealer`, `HealingOrchestrator` | Self-healing broken tests (Days 15–16) |
| `model/` | `ChangedMethod`, `DiffHunk`, `FileDiff`, `GeneratedTest`, `GenerationContext`, `ProjectConventions`, `TestRun`, `ValidationResult`, `HealingResult` | Immutable records and sealed interfaces — no logic |
| `orchestration/` | `TestGenerationOrchestrator` | Pipeline wiring from diff to PR |
| `persistence/` | `DynamoDbTestRepository`, `ProjectConventionsRepository`, `S3TestArtifactStore` | AWS SDK calls |
| `validation/` | `TestCompiler`, `TestExecutor` | In-process compile and run generated tests |
