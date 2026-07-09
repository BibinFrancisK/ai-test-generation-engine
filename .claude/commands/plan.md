# plan

## Objective
Read the execution plan for a specific day, check out a correctly named feature branch, and write a structured task list to `plan/day_N.md`. Do **not** begin implementation.

---

## Input

The user provides the day number as `$ARGUMENTS` (e.g. `/plan 8`).

- If `$ARGUMENTS` is empty or not a positive integer, stop and ask:
  > "Which day would you like to plan? Please provide a day number (e.g. `/plan 8`)."
- Do not proceed until a valid day number is supplied.

---

## Steps

### 1. Read the Execution Plan
- Open `plan/EXECUTION_PLAN.md`.
- Locate the section for Day `$ARGUMENTS`.
- Identify:
  - The day's **theme / title** (e.g. "Context Assembler", "GitHub PR Creator")
  - The key **deliverables** listed for that day
  - Any explicit tasks, sub-tasks, or acceptance criteria

If the day number is not found in `EXECUTION_PLAN.md`, stop and tell the user which days are available.

### 2. Determine the Branch Name
- Derive a short, meaningful slug from the day's theme — **never use the day or week number** in the branch name.
- Format: `feat/<topic-slug>` (or `fix/`, `chore/`, etc. if more appropriate).
  - Good examples: `feat/context-assembler`, `feat/github-pr-creator`, `feat/webhook-handler`
  - Bad examples: `feat/day-8`, `week2/day8`, `day8-context`
- If the current branch already matches the correct slug, stay on it. Otherwise:
  - Check whether the branch already exists locally or on the remote.
  - If it exists, check it out. If not, create it from `main`.

### 3. Check Out the Branch
- Run `git checkout <branch>` (or `git checkout -b <branch>` for a new branch).
- Confirm the active branch to the user.

### 4. Write the Day Plan
- Create or overwrite `plan/day_N.md` (where `N` is the provided day number).
- The file must be structured as a numbered task list. Each task entry must include:
  - **Task number and title** — concise, action-oriented (e.g. "Task 3: Implement `ContextAssembler`")
  - **Goal** — one sentence stating what must be true when this task is done
  - **Key deliverables** — bullet list of files to create or modify, methods to implement, tests to write
  - **Acceptance criteria** — specific, verifiable conditions (e.g. "`./mvnw test` passes", "returns `Optional.empty()` on 404")
- Aim for 6–10 tasks per day, matching the granularity in `EXECUTION_PLAN.md`.
- End the file with a `## Verification` section listing the commands to run once all tasks are complete (typically `./mvnw clean verify`).

### 5. Confirm to the User
- Print a summary:
  - Branch checked out
  - Path of the plan file written
  - Number of tasks created
  - First and last task titles (so the user can sanity-check scope)
- Tell the user: "Run `/implement-plan $ARGUMENTS` to start implementation."

---

## General Rules
- NEVER start implementing any task — this command is planning only.
- NEVER commit or push anything.
- The `plan/` directory is git-ignored; no `.gitignore` changes are needed.
- Branch naming must follow the project convention: `feat/<slug>` — never include day or week numbers.
- If `plan/day_N.md` already exists, warn the user and ask whether to overwrite it before proceeding.