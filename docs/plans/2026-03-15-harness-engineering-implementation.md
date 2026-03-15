# Harness Engineering Implementation Plan

> **For agentic workers:** REQUIRED: Use `subagent-driven-development` (if subagents available) or `executing-plans` to implement this plan. This is a living document. Update `Progress`, `Surprises & Discoveries`, `Decision Log`, `Validation Log`, and `Handoff / Resume Notes` whenever work advances or stalls.

**Goal:** Complete and harden the local-first harness architecture so contributors can use `AGENTS.md`, `justfile`, templates, and local `copilot` CLI delegation consistently.

**Architecture:** This is a repo-local process/tooling rollout, not an application feature. The implementation keeps the editor session responsible for direction and synthesis, while `justfile`, docs, and local `copilot` CLI patterns provide deterministic entry points for execution, review, and verification.

**Design spec:** `docs/specs/2026-03-15-harness-engineering-design.md`

**Tech stack:** Markdown, just, Git, GitHub Copilot CLI, Java 25/Spring Boot 3.5, React 19/Vite 7, Docker Compose

**Current status:** Implemented, externally revalidated, and locally re-verified

**Current focus:** Preserve the verified local-first harness and capture the remaining deferred follow-ups clearly.

---

## Progress

- [x] Created a local-first harness design spec.
- [x] Added `AGENTS.md` and `justfile` as the primary local harness entry points.
- [x] Removed out-of-scope GitHub-hosted CI and issue-template artifacts.
- [x] Added local `copilot` CLI delegation guidance and recipes.
- [x] Validated the harness against external literature from OpenAI, Anthropic, LangChain, Muraco, Martin Fowler, and HumanLayer.
- [x] Reconciled multi-agent review feedback into the harness docs and task runner.
- [x] Added a fail-fast readiness preflight so `just check` reports the `just up` prerequisite clearly before Playwright runs.
- [x] Stabilized an unrelated flaky backend JWT tampering test so full harness verification is trustworthy again.

## Surprises & Discoveries

- Observation: `copilot` CLI can consume prompt text from stdin even without `-p`.
  - Evidence: `printf 'Reply with only OK\n' | timeout 20s copilot` returned `OK`.
- Observation: installed CLI agent identifiers are namespaced IDs, not the VS Code display names used by editor subagents.
  - Evidence: `copilot --agent "Plan Mode - Strategic Planning & Architecture"` returned a list of available IDs such as `project-planning/plan` and `gem-team/gem-reviewer`.
- Observation: the previous harness overstated what `just check` proved.
  - Evidence: the reviewer passes consistently flagged that the docs claimed stronger guarantees than the actual local validation command provides.
- Observation: the remaining operational gap was not the E2E dependency itself, but the hidden prerequisite that the local stack already be serving `http://localhost:8000`.
  - Evidence: `rtk just check` failed with `ERR_CONNECTION_REFUSED` until the harness was updated to check readiness before Playwright ran.
- Observation: the backend test failure blocking final validation was a flaky assertion, not a JWT service defect.
  - Evidence: the full suite failed once, the focused tampering test passed 20 consecutive reruns, and mutating the signature segment deterministically restored consistent suite behavior.

## Decision Log

- Decision: keep the harness local-first and defer remote automation.
  - Rationale: this repository is a demo; local iteration and legibility are higher-value than hosted workflow complexity right now.
  - Date/Author: 2026-03-15 / GitHub Copilot

- Decision: shorten `AGENTS.md` and treat it as the onboarding map rather than the full handbook.
  - Rationale: external references consistently favor concise top-level agent files plus progressive disclosure.
  - Date/Author: 2026-03-15 / GitHub Copilot

- Decision: make plans the living source of execution state.
  - Rationale: long-running local agent work needs explicit progress, decision, validation, and handoff sections to survive context loss.
  - Date/Author: 2026-03-15 / GitHub Copilot

- Decision: keep safe/default and autonomous `copilot` CLI recipes separate.
  - Rationale: bounded local review work benefits from convenient CLI entry points, but autonomous execution should remain explicit opt-in.
  - Date/Author: 2026-03-15 / GitHub Copilot

- Decision: keep `just check` explicit about its runtime prerequisite by failing fast instead of auto-starting the local stack.
  - Rationale: the smallest honest fix is to expose the `just up` prerequisite at the point of failure while keeping validation deterministic and side-effect free.
  - Date/Author: 2026-03-15 / GitHub Copilot

- Decision: stabilize the flaky JWT tampering test uncovered during final validation.
  - Rationale: the harness should not be reported incomplete because of an unrelated nondeterministic assertion in the backend test suite.
  - Date/Author: 2026-03-15 / GitHub Copilot

## Validation Log

- Command: `just --list`
  - Result: passed
  - Notes: confirmed the task runner parses and exposes the expected recipes.

- Command: `fetch_webpage` against the six requested harness-engineering articles plus directly relevant linked references
  - Result: completed
  - Notes: used as the evidence base for the validation pass.

- Command: sequential reviewer passes using skeptic, constraint, user-advocate, and arbiter roles
  - Result: completed with final disposition `REVISE`
  - Notes: accepted objections were reconciled into the docs and task runner.

- Command: stdin-based local `copilot` CLI probe
  - Result: passed
  - Notes: enabled safer justfile recipes that no longer embed full prompt bodies on argv.

- Command: `timeout 30s just copilot-prompt .tmp/ok-prompt.txt` and `timeout 30s just copilot-agent project-planning/plan .tmp/ok-prompt.txt`
  - Result: passed
  - Notes: verified the safe/default stdin-based just recipes execute successfully for both the default agent and a named installed agent.

- Command: final arbiter pass after revisions
  - Result: `APPROVED`
  - Notes: the accepted objections were judged resolved within the repo’s local-first demo scope.

- Command: `rtk just check`
  - Result: failed before the readiness-preflight fix
  - Notes: backend tests passed, but Playwright E2E hit `ERR_CONNECTION_REFUSED` because no stack was serving `http://localhost:8000`.

- Command: `rtk just frontend-test`
  - Result: failed fast as designed
  - Notes: confirmed the new readiness preflight stops before Playwright launches and instructs contributors to run `just up`.

- Command: `cd backend && rtk mvn -q -Dtest=JwtTokenServiceTest#isTokenValid_returnsFalseForTamperedToken -Dmaven.repo.local=../.m2/repository test` (20 reruns)
  - Result: passed repeatedly
  - Notes: demonstrated that the original suite failure was a flaky tampering assertion rather than a stable JWT validation defect.

- Command: `cd backend && rtk mvn -q -Dtest=JwtTokenServiceTest -Dmaven.repo.local=../.m2/repository test`
  - Result: passed
  - Notes: confirmed the deterministic tampering fix stabilized the focused JWT test class.

- Command: `rtk just up && until rtk curl -fsS http://localhost:8000 >/dev/null 2>&1; do sleep 2; done && rtk just check`
  - Result: passed
  - Notes: local stack became reachable, backend tests passed (`164` tests), and frontend Playwright E2E passed (`12` passed, `4` skipped).

## Handoff / Resume Notes

- Last known good state: the harness docs and task runner are aligned with a local-first, progressive-disclosure workflow, validated against external literature, and approved by the final arbiter pass.
- Last known good state: `just frontend-test` and `just check` now fail fast with a clear readiness message when `http://localhost:8000` is unavailable, and the full `rtk just check` proof loop passes once the stack is up.
- Next recommended step: if the harness expands further, consider a lightweight smoke-check recipe for the local stack and broader static-analysis/security coverage.
- Open blocker or ambiguity: no blocking ambiguity remains for the current local-first harness; the main remaining limitation is intentionally deferred static-analysis/security breadth.

## Pre-flight

- [x] Read the design spec fully before starting
- [x] Create a git worktree for this feature branch when isolation matters
- [x] Verify `just check` passes on the fully prepared local stack

## Context and Orientation

This plan governs the repo-local harness rather than application behavior. The top-level onboarding map lives in `AGENTS.md`. The executable local entry points live in `justfile`. The source-of-truth design rationale lives in `docs/specs/2026-03-15-harness-engineering-design.md`. Reusable plan behavior lives in `docs/templates/plan-template.md`. This plan records rollout state, validation evidence, and handoff notes so future sessions do not need to reconstruct history from chat alone.

## File structure map

### Files to modify

| File | Purpose | Planned change |
|------|---------|----------------|
| `AGENTS.md` | Project operating instructions | Keep local-first workflow, task runner usage, and CLI delegation rules aligned with actual repo practice |
| `README.md` | Contributor overview | Keep quick-start and testing guidance aligned with the repo-preferred `just` workflow |
| `justfile` | Unified task runner | Add or refine recipes that make common local workflows deterministic and discoverable |
| `docs/specs/2026-03-15-harness-engineering-design.md` | Harness architecture source of truth | Keep design decisions, feedback loop wording, and artifact inventory consistent with implemented reality |
| `backend/src/test/java/com/demo/sso/service/JwtTokenServiceTest.java` | Backend JWT validation tests | Remove flaky tampering behavior that blocked full local verification |
| `docs/templates/plan-template.md` | Reusable implementation plan template | Enforce just-based verification, file maps, and agentic execution guidance |
| `docs/templates/spec-template.md` | Reusable design template | Keep status/structure aligned with the harness workflow if gaps are found during rollout |

### Files to create

| File | Responsibility |
|------|----------------|
| `docs/plans/2026-03-15-harness-engineering-implementation.md` | Executable rollout plan for the local-first harness |

## Chunk 1: Baseline and align the docs

### Tasks

- [x] **Task 1.1: Audit harness docs against implemented reality**
  - Acceptance criteria: every artifact mentioned in the design spec exists and stale references (`make`, CI, issue templates) are removed or intentionally deferred
  - Verify: `rtk grep 'make check|.github/workflows|ISSUE_TEMPLATE' docs AGENTS.md justfile` shows no unintended stale references

- [x] **Task 1.2: Normalize validation wording to `just`**
  - Acceptance criteria: all workflow docs and templates use `just check`, `just backend-test`, and `just frontend-test` consistently
  - Verify: `rtk grep 'just check|just backend-test|just frontend-test' docs AGENTS.md justfile`

- [x] **Task 1.3: Record local-first scope explicitly**
  - Acceptance criteria: docs state that cloud/remote automation is out of scope for the demo and local verification is the default
  - Verify: `rtk grep 'local-first|out of scope|local `copilot` CLI' AGENTS.md docs/specs/2026-03-15-harness-engineering-design.md`

### Chunk checkpoint

- [x] All tasks complete
- [x] `just check` passes
- [ ] Commit with message: `docs: align local-first harness guidance`

## Chunk 2: Operationalize local Copilot CLI delegation

### Tasks

- [x] **Task 2.1: Decide the minimum supported CLI delegation patterns**
  - Acceptance criteria: repo guidance explicitly covers at least focused-task delegation, plan critique, and code review / second-pass validation
  - Verify: `rtk grep 'bounded tasks|plan critique|code review|second-pass' AGENTS.md docs/specs/2026-03-15-harness-engineering-design.md`

- [x] **Task 2.2: Add deterministic entry points for CLI-driven work**
  - Acceptance criteria: contributors have a documented or scripted way to invoke local `copilot` CLI for focused tasks without inventing commands ad hoc
  - Verify: `just --list` shows the intended recipes if recipes are added, or docs show exact commands if documentation is chosen instead

- [x] **Task 2.3: Validate the CLI flow with one real second-pass review**
  - Acceptance criteria: a local `copilot` CLI run reviews either the spec or plan and produces actionable feedback that is either applied or explicitly rejected with reason
  - Verify: capture the CLI output in the session and reconcile it into the edited docs

### Chunk checkpoint

- [x] All tasks complete
- [x] `just check` passes
- [ ] Commit with message: `docs: operationalize local copilot cli delegation`

## Chunk 3: Close the planning/verification loop

### Tasks

- [x] **Task 3.1: Ensure the design spec status reflects reality**
  - Acceptance criteria: the spec status is appropriate for the current phase (`Approved for Planning` or `Implemented`) and does not contradict the presence of a plan
  - Verify: open the spec and confirm the status matches the repo state

- [x] **Task 3.2: Ensure templates support future end-to-end runs**
  - Acceptance criteria: spec and plan templates are sufficient for a future contributor to repeat the same design → plan → implement → verify flow locally
  - Verify: manually compare template sections against `brainstorming`, `writing-plans`, and `verification-before-completion` expectations

- [x] **Task 3.3: Perform a local smoke review of the harness**
  - Acceptance criteria: `AGENTS.md`, `justfile`, spec, and plan agree on the same local-first workflow and validation loop
  - Verify: `rtk grep 'justfile|just check|local `copilot` CLI|subagent-driven-development|executing-plans' AGENTS.md docs/specs docs/plans docs/templates`

### Chunk checkpoint

- [x] All tasks complete
- [x] `just check` passes
- [ ] Commit with message: `docs: finalize local-first harness loop`

## Chunk 4: Validate against external harness-engineering literature

### Tasks

- [x] **Task 4.1: Gather the requested articles and relevant linked references**
  - Acceptance criteria: the validation uses the six user-provided articles plus directly relevant linked follow-ups
  - Verify: the session contains fetched material from OpenAI, Anthropic, LangChain, Muraco, Martin Fowler, and HumanLayer

- [x] **Task 4.2: Run a structured multi-agent review**
  - Acceptance criteria: skeptic, constraint, user-advocate, and arbiter roles all produce explicit findings
  - Verify: reviewer outputs are captured in the session and summarized into accepted vs rejected objections

- [x] **Task 4.3: Reconcile accepted objections into the harness artifacts**
  - Acceptance criteria: `AGENTS.md`, `justfile`, the design spec, and the plan template reflect the accepted changes
  - Verify: grep and direct file reads show the updated guidance in place

### Chunk checkpoint

- [x] All tasks complete
- [x] `just check` passes
- [ ] Commit with message: `docs: validate harness against external literature`

## Chunk 5: Make the validation contract operational

### Tasks

- [x] **Task 5.1: Clarify the `just check` runtime prerequisite with a structured review**
  - Acceptance criteria: the repo has an explicit decision about whether to document, preflight, or auto-start the stack
  - Verify: reviewer outputs and arbiter decision are captured in the session

- [x] **Task 5.2: Add a fail-fast readiness preflight for Playwright-backed validation**
  - Acceptance criteria: `just frontend-test`, `just test`, and `just check` stop early with a clear `just up` instruction when `http://localhost:8000` is unreachable
  - Verify: rerun the relevant recipe with the stack down and confirm the explicit readiness message appears before Playwright launches

- [x] **Task 5.3: Sync the docs and design rationale with the new validation contract**
  - Acceptance criteria: `AGENTS.md`, the harness design spec, and contributor-facing docs describe the explicit readiness behavior consistently
  - Verify: direct file reads and grep checks show the new wording in place

### Chunk checkpoint

- [x] All tasks complete
- [x] `just check` passes on a prepared local stack
- [ ] Commit with message: `docs: operationalize harness readiness preflight`

## Final validation

- [x] `just check` passes (lint + all tests)
- [x] Manual smoke test against Docker Compose stack if any runtime-facing harness behavior changed
- [x] No TODO/FIXME left unresolved in modified docs
- [x] Documentation updated if needed
