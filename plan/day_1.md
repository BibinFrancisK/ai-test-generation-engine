# Day 1 — System Design, ADRs, and Repo Scaffold

**Branch:** `feat/scaffold-and-adrs`  
**Time box:** ~90 min  
**Goal:** Establish the architecture in writing, record the five key decisions as ADRs, and create the directory skeleton the rest of the project will grow into.

---

## Definition of Done

- [ ] `docs/adr/` contains ADR-001 through ADR-005, each 10–20 lines, three sections each
- [ ] `docs/architecture.md` contains a valid Mermaid sequence diagram of the happy path
- [ ] Full directory tree exists under `src/`, `infra/terraform/`, `.github/workflows/`, with `.gitkeep` files in every empty directory
- [ ] `README.md` is meaningful to a first-time reader: pitch, diagram, prerequisites, quickstart stub
- [ ] `.gitignore` covers all generated/sensitive/tool artifacts
- [ ] All changes committed: `feat: project scaffold, architecture diagram, and ADRs 001-005`

---

## Task Breakdown

### Task 1 — Write ADRs (docs/adr/)

Create `docs/adr/` and write five ADR files using this lightweight template:

```
# ADR-NNN — <Title>

## Context
<What problem are we solving and what constraints apply?>

## Decision
<What we decided and the key reason.>

## Consequences
<Trade-offs, follow-up work, what this rules out.>
```

Keep each ADR to 10–20 lines. These are decisions, not essays.

#### ADR-001 — Compute: ECS Fargate + ALB
- **Context:** Need a serverless container platform; criteria are: IaC-friendly full teardown, no idle cost, real-company technology for portfolio signal, and least-privilege IAM separation.
- **Decision:** ECS Fargate + ALB. One cluster, one task definition, one service. Two IAM roles: task execution role (ECR pull + CloudWatch logs) and task role (S3 + DynamoDB + SSM paths only). ALB handles HTTPS termination.
- **Consequences:** Requires ECR for image storage; ALB adds ~$16/month — run `terraform destroy` after every demo session to zero cost. App Runner was rejected because it hides the IAM/networking layer that makes this a portfolio piece.

#### ADR-002 — LLM Provider: Anthropic Claude first, OpenAI stub, sealed interface
- **Context:** Need to call an LLM for test generation. Provider quality, cost predictability, and the ability to swap providers without code changes are all requirements.
- **Decision:** `LlmProvider` is a Java sealed interface permitting `AnthropicLlmProvider`, `OpenAiLlmProvider`, and `NoopProvider`. Active provider is selected by `testgen.llm.provider` in `application.yml`. `max_tokens=1500` on every call; $10/month hard cap set in the Anthropic console.
- **Consequences:** Claude first because it leads on Java code generation quality and LangChain4j has first-class Anthropic support. OpenAI is a one-line config swap. `NoopProvider` ensures zero LLM cost during tests.

#### ADR-003 — Test Storage: DynamoDB (metadata) + S3 (artifacts)
- **Context:** Need to store test-run metadata (status, PR link, errors) and generated test source files. Must stay within AWS free tier for portfolio usage.
- **Decision:** DynamoDB Enhanced Client for two tables: `test-runs` (keyed by `testRunId`) and `project-conventions` (keyed by `repositoryId`). S3 for generated `.java` artifacts (private bucket, presigned URL access). Both provisioned via Terraform; emulated locally by LocalStack.
- **Consequences:** DynamoDB free tier (25 GB) covers all portfolio traffic. S3 keeps raw files out of DynamoDB, which has a 400 KB item limit. Schema evolution is handled by `schemaVersion` fields on all records.

#### ADR-004 — Local AWS: LocalStack
- **Context:** Need to develop and test AWS SDK calls (DynamoDB, S3, SSM) without incurring AWS cost or requiring a live AWS account on every `mvn verify`.
- **Decision:** LocalStack (latest stable Docker image) via Docker Compose. All integration tests target LocalStack; Terraform can also run against LocalStack for infra validation. Ports wired via `@DynamicPropertySource` in Testcontainers.
- **Consequences:** Zero local AWS cost. Full Terraform parity on CI. Testcontainers starts LocalStack per-test-class with `static` container reuse. Do not use `latest` image tag — pin to an exact version.

#### ADR-005 — Source Analysis: JavaParser
- **Context:** Need to parse Java source files to extract method signatures (`ChangedMethod`) and detect project test conventions (`ProjectConventions`) without requiring compiled bytecode.
- **Decision:** `com.github.javaparser:javaparser-core`. Use `StaticJavaParser.parse(String)` to get a `CompilationUnit`; walk `MethodDeclaration` nodes via `VoidVisitorAdapter`. Works on raw `.java` source strings fetched from the GitHub Contents API.
- **Consequences:** Simpler than Spoon (which needs a full classpath) and byte-buddy (which needs bytecode). No classpath resolution needed — we only need method names, parameter types, annotations, and line numbers, all available in the raw AST.

---

### Task 2 — Architecture Documentation (docs/architecture.md)

Create `docs/architecture.md` with a Mermaid sequence diagram showing the happy path end-to-end.

The diagram must include these participants and steps:

```
GitHub → WebhookController : PR opened (push event)
WebhookController → WebhookController : validate HMAC-SHA256
WebhookController → GitHubWebhookHandler : route event
GitHubWebhookHandler → DiffAnalyzer : parse + analyze diff
DiffAnalyzer → ContextAssembler : List<ChangedMethod>
ContextAssembler → GitHubContentsFetcher : fetch source, test file, deps (Tier 1)
ContextAssembler → ProjectConventionsRepository : load conventions (Tier 2)
ContextAssembler → TestGenerationService : GenerationContext
TestGenerationService → LlmProvider : prompt + GenerationContext
LlmProvider → TestGenerationService : generated test source
TestGenerationService → TestValidator : compile + execute
TestValidator → TestGenerationService : ValidationResult
TestGenerationService → S3TestArtifactStore : upload .java artifact
TestGenerationService → DynamoDbTestRepository : save TestRun
TestGenerationService → GitHubPrCreator : GeneratedTest
GitHubPrCreator → GitHub : POST /git/refs (create testgen/ branch)
GitHubPrCreator → GitHub : PUT /contents (commit test file)
GitHubPrCreator → GitHub : POST /pulls (open test PR)
GitHubPrCreator → GitHub : POST /issues/{n}/comments (link comment on source PR)
GitHub → Engineer : test PR visible, source PR has notification comment
```

Also add a short paragraph describing the two-tier context assembly (ADR-007) and a component table matching the package structure from `.claude/rules/api-design.md`.

---

### Task 3 — Directory Tree Skeleton

Create all directories with `.gitkeep` files. Exact list:

**Source packages** (`src/main/java/com/testgen/`):
- `analysis/`
- `api/`
- `config/`
- `context/`
- `generation/`
- `github/`
- `healing/`
- `model/`
- `orchestration/`
- `persistence/`
- `validation/`

**Test packages** (`src/test/java/com/testgen/`):
- `analysis/`
- `generation/`
- `orchestration/`
- `validation/`
- `e2e/`

**Test resources:**
- `src/test/resources/fixtures/`

**Infra:**
- `infra/terraform/` — create stub `.tf` files (empty but named) for: `main.tf`, `variables.tf`, `outputs.tf`, `s3.tf`, `dynamodb.tf`, `iam.tf`, `ecr.tf`, `ecs.tf`, `alb.tf`

**CI:**
- `.github/workflows/` — `.gitkeep` (CI workflow is added on Day 2)

**Docs:**
- `docs/adr/` — already created in Task 1

---

### Task 4 — Update README.md

Rewrite `README.md` to include:

1. **Title + one-line pitch:** "AI-Powered Test Generation Engine — analyzes PR diffs, generates JUnit 5 tests via Claude, and opens a PR with the results."
2. **Status badge:** `Status: In Progress — Week 1`
3. **Architecture diagram:** embed the Mermaid sequence diagram (copy from `docs/architecture.md`)
4. **What it does:** 4-bullet summary of the pipeline (webhook → diff → generate → PR)
5. **Tech stack table:** Java 21, Spring Boot 3, LangChain4j, JavaParser, DynamoDB, S3, ECS Fargate, Terraform, LocalStack
6. **Prerequisites:** Java 21, Maven 3.9+, Docker Desktop, AWS CLI v2, Terraform, IntelliJ (Community), smee.io channel URL, Anthropic API key with $10 cap
7. **Local quickstart (stub):**
   ```
   # Coming Day 2 — Spring Boot scaffold
   # ./mvnw verify
   # docker compose up
   ```
8. **ADR links:** table linking to each ADR file in `docs/adr/`

---

### Task 5 — Update .gitignore

Add the following entries (keep existing content, append only what's missing):

```
# Build output
target/
*.class

# Terraform
*.tfstate
*.tfstate.backup
.terraform/
.terraform.lock.hcl

# Local config and secrets
.env
.env.*
*.pem

# Runtime artifacts
tmp/
generated-tests/
smee-url.txt
```

---

### Task 6 — Commit

Stage and commit all new files. Do **not** use `git add .` — stage by directory to avoid accidentally including IDE or OS files.

```
git add docs/ src/ infra/ .github/ README.md .gitignore
git commit -m "feat: project scaffold, architecture diagram, and ADRs 001-005"
```

Verify with `git status` that the working tree is clean after the commit.

---

## Notes and Risks

- **ADR length discipline:** If any ADR starts running past 20 lines, cut to bullet-point stubs. The purpose of Day 1 ADRs is to record the decision, not to argue it. Elaboration goes in `docs/architecture.md`.
- **ADR-006 and ADR-007:** These two decisions (PR-based delivery, two-tier context) were made during planning. Add them on Day 7 buffer or inline on Day 1 if time permits — they are not blocking.
- **Mermaid rendering:** GitHub natively renders Mermaid in Markdown. Test by pushing and viewing the file in the GitHub UI. If the diagram doesn't render, check for unclosed `end` blocks or participant name collisions.
- **Stub `.tf` files vs `.gitkeep`:** Prefer creating named stub `.tf` files (empty `# placeholder`) over `.gitkeep` in `infra/terraform/` — this signals intent and allows Terraform to parse the directory on Day 18 without surprises.
- **No code on Day 1:** All tasks are documentation and directory structure. The first Java file is written on Day 2.
