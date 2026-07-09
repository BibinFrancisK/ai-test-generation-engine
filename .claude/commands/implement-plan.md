# implement-plan

## Objective
Walk through `plan/day_N.md` task-by-task, implementing 2–3 tasks per round with user confirmation at each step, until all tasks are complete.

---

## Input

The user provides the day number as `$ARGUMENTS` (e.g. `/implement-plan 8`).

- If `$ARGUMENTS` is empty or not a positive integer, stop and ask:
  > "Which day would you like to implement? Please provide a day number (e.g. `/implement-plan 8`)."
- Do not proceed until a valid day number is supplied.

---

## Steps

### 1. Load the Plan
- Check whether `plan/day_$ARGUMENTS.md` exists.
- If the file does not exist, stop and tell the user:
  > "`plan/day_$ARGUMENTS.md` not found. Run `/plan $ARGUMENTS` first to generate the task list."
- If the file exists, read it and list all tasks with their titles and goals so the user can see the full scope.

### 2. Confirm the Starting Batch
- Identify tasks 1 and 2 from the plan.
- Ask the user for confirmation before touching any code:
  > "Can I start implementing tasks 1 and 2?
  > - Task 1: `<title>` — `<goal>`
  > - Task 2: `<title>` — `<goal>`"
- Wait for explicit approval. If the user says no or asks a question, answer it and re-ask.

### 3. Implement the Approved Batch
For each approved task:
- Follow the task's **key deliverables** and **acceptance criteria** exactly.
- Apply all project conventions from `.claude/rules/` and `CLAUDE.md`:
  - Domain objects are immutable Java `record`s in `model/`.
  - No business logic in `config/` — wiring only.
  - `model/` must not import from any other package.
  - `NoopProvider` must be used in all test classes — never real LLM calls.
  - Use SLF4J `Logger` — never `System.out.println`.
  - No hardcoded secrets, API keys, or endpoint URLs anywhere.
- Write tests alongside production code — not as a separate step unless the task explicitly says otherwise.
- Do not implement anything beyond the approved tasks.

### 4. Report Files Changed
After completing the batch, list every file that was **created** or **modified**:
```
Created:
  src/main/java/com/testgen/.../<ClassName>.java
  src/test/java/com/testgen/.../<ClassNameTest>.java

Modified:
  src/main/java/com/testgen/config/AppConfig.java
```

### 5. Handle Feedback
- If the user provides feedback, corrections, or asks questions about the work just done:
  - Address the feedback or answer the question.
  - Apply any requested changes.
  - Re-report the updated file list.
- Continue this loop until the user signals they are satisfied with the current batch.

### 6. Proceed to the Next Batch
- Once the user is satisfied, identify the next 2–3 unimplemented tasks.
- Ask for confirmation in the same format as Step 2:
  > "Ready for the next batch. Can I implement tasks 3 and 4?
  > - Task 3: `<title>` — `<goal>`
  > - Task 4: `<title>` — `<goal>`"
- Repeat Steps 3–5 for each confirmed batch.
- Adjust batch size (2 or 3 tasks) based on task complexity — prefer smaller batches when tasks are large or involve unfamiliar APIs.

### 7. Final Verification
- After the last task in the plan is implemented and accepted, run the verification commands listed in `plan/day_$ARGUMENTS.md`'s `## Verification` section (typically `./mvnw clean verify`).
- If verification passes, tell the user:
  > "All tasks in `plan/day_$ARGUMENTS.md` are complete and tests pass. Run `/commit-and-open-pr` to review, commit, and open a pull request."
- If verification fails, show the failure output and ask the user how to proceed. Do **not** proceed to the PR step with a broken build.

---

## General Rules
- NEVER implement more tasks than the user has approved in the current batch.
- NEVER skip the confirmation step — even for "simple" tasks.
- NEVER commit or push — that is handled by `/commit-and-open-pr`.
- NEVER read or modify files inside `.env`, `.env.local`, `docker/.env`, or any `.env.*`.
- ALWAYS end every response with the list of files created or modified in that batch.
- If a task's acceptance criteria cannot be met (e.g. a dependency is missing), stop and explain — do not silently skip it.