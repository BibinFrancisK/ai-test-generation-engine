# ADR-006 — Delivery Mechanism: PR-Based Over Review Comments

## Context

Once a test is generated and validated, the service needs to hand it back to the developer in a form they can actually use. Two options were evaluated:

1. Post the generated test code as a comment on the source PR (read-only, copy-paste required)
2. Create a dedicated branch, commit the test file, and open a real pull request against the source branch

The generated test must be runnable in CI, reviewable via a normal diff, and mergeable or editable without manual copy-paste.

## Decision

**PR-based delivery.** `GitHubPrCreator` creates a `testgen/{source-branch}-{id}` branch from the source branch, commits the generated `.java` test file via the Contents API, opens a PR from that branch back against the source branch, and posts a short notification comment with a link on the original source PR.

## Consequences

- The generated test lives in git history from the moment it's created — it's diffable, revertable, and blamable like any other commit
- CI runs against the `testgen/` branch automatically, so the test's validity is checked a second time in a real environment, not just the in-process `TestValidator`
- The developer can review, edit, or merge the test through the normal PR workflow — no copy-paste step
- Two PRs exist per source PR (source + testgen) — slightly more GitHub noise than a single comment, mitigated by the notification comment linking them together
- Mirrors how production AI coding tools (Devin, Copilot Workspace) deliver suggestions — a comment-only approach is closer to a linter than a collaborator
- Self-healing (`HealingOrchestrator`) reuses the same `GitHubPrCreator` path for `testgen/heal-...` PRs — one delivery mechanism serves both generation and healing
