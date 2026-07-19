# Changelog

All notable changes to this project are documented here. Format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [v0.2] — 2026-07-19

Core generation pipeline complete — diff analysis → LLM generation → validation → creates `testgen/` branch and opens PR against source branch.

### Added
- In-process test validation (`TestValidator`): compiles the generated test alongside the class-under-test's source via the Java Compiler API, then executes it with the JUnit Platform Launcher
- LocalStack-backed persistence: `DynamoDbTestRepository` (test-run metadata), `ProjectConventionsRepository` (per-repo conventions cache), `S3TestArtifactStore` (generated test artifacts)
- Two-tier context assembly (`ContextAssembler`, ADR-007): always-fresh GitHub source/tests/dependencies (Tier 1) combined with cached, repo-level testing conventions (Tier 2)
- Full GitHub App integration: RS256 JWT → installation token auth (`GitHubAppAuthenticator`), HMAC-SHA256 webhook signature validation, and `GitHubPrCreator`'s four-step delivery (create branch → commit test file → open PR → post notification comment)
- GitHub Actions CI now runs the LocalStack-backed integration test suite on every push/PR, not just unit tests
- `docs/demo-e2e-run.md` — evidence from a real end-to-end run against a live GitHub repository

### Fixed
- `GitHubWebhookHandler` now ignores `pull_request` events on its own `testgen/` branches — without this guard, opening a generated test PR fired a webhook back at the engine, which recursively (and unsuccessfully) tried to generate a test for the generated test file

## [v0.1] — 2026-07-09

Week 1: source analysis + LLM test generation (no validation or GitHub integration yet).

### Added
- `DiffParser` / `DiffAnalyzer` — parses GitHub unified diffs and correlates hunks to changed method signatures via JavaParser AST (`SourceAnalyzer`)
- `ProjectConventionsAnalyzer` — detects testing framework, mock library, and base test class from existing test sources
- `TestGenerationService` — calls the active `LlmProvider` (sealed interface: `AnthropicLlmProvider` / `OpenAiLlmProvider` / `NoopProvider`) and writes the generated JUnit 5 test to disk
- Spring Boot 3 scaffold with virtual threads enabled and a green CI pipeline
