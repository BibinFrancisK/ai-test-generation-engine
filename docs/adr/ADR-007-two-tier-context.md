# ADR-007 — LLM Context Assembly: Two-Tier Over Diff-Only or Full-Snapshot

## Context

The quality of a generated test depends entirely on what the LLM is shown. Three approaches were evaluated:

1. **Diff-lines-only** — just the changed lines from the PR diff
2. **Full-codebase-snapshot** — the entire repository, or a large slice of it, on every request
3. **Two-tier context** — a per-PR fetch of exactly what's needed, plus a cached, per-repository convention profile

Diff-lines-only produces syntactically plausible but stylistically generic tests — it has no visibility into the project's existing test patterns, mocking style, or assertion conventions. Full-snapshot solves that but is expensive (large prompts, higher LLM cost, slower requests) and goes stale the moment a file changes elsewhere in the repo.

## Decision

**Two-tier context assembly**, combined by `ContextAssembler` into a single `GenerationContext` record:

- **Tier 1 — always fresh, fetched per PR via the GitHub Contents API (`GitHubContentsFetcher`):** full source of the changed class, the existing test file for that class (if any), and up to 3 dependency sources resolved from the changed file's imports.
- **Tier 2 — per-repository conventions, cached in DynamoDB (`project-conventions` table):** testing framework, mock library, base test class, detected by `ProjectConventionsAnalyzer` from a sample of existing test files. Reused if `analyzedAt` is ≤7 days old; re-analyzed and re-saved otherwise.

`TestGenerationPromptBuilder` assembles the prompt from `GenerationContext` and logs the estimated token count, kept under a 2000-token budget (`max_tokens=1500` per LLM call).

## Consequences

- The existing test file is the single highest-value input — it shows the LLM the project's actual testing style directly, which no amount of prompt engineering replicates as well
- Tier 1 is always fresh (fetched per PR), so it never risks acting on stale source — a real correctness requirement, since the test must compile against the current code
- Tier 2's 7-day cache avoids re-scanning the whole test suite on every single PR, keeping latency and LLM/API cost down, while the staleness window is short enough that convention drift is caught quickly
- Total context stays small and bounded (full source + 1 test file + ≤3 deps + a conventions record) rather than growing with repository size — cost and latency are predictable regardless of how large the target repo is
- Trade-off: dependency resolution is import-based and capped at 3 files, so context can miss relevant code reachable only transitively — acceptable given the token budget, and mitigated by the existing test file usually being the more valuable signal anyway
