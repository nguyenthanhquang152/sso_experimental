# [Feature Name] Implementation Plan

> **For agentic workers:** REQUIRED: Use `subagent-driven-development` (if subagents available) or `executing-plans` to implement this plan. This is a living document. Update `Progress`, `Surprises & Discoveries`, `Decision Log`, `Validation Log`, and `Handoff / Resume Notes` whenever work advances or stalls.

**Goal:** One sentence describing what this plan achieves.

**Architecture:** Two to three sentences describing the implementation approach and boundaries.

**Design spec:** `docs/specs/YYYY-MM-DD-<topic>-design.md`

**Tech stack:** List relevant technologies.

**Current status:** Planned | In progress | Blocked | Implemented

**Current focus:** One sentence describing the highest-priority next move.

---

## Progress

- [ ] Example completed or pending step
- [ ] Keep this section current at every stopping point

## Surprises & Discoveries

- Observation: ...
  - Evidence: ...

## Decision Log

- Decision: ...
  - Rationale: ...
  - Date/Author: ...

## Validation Log

- Command: ...
  - Result: ...
  - Notes: ...

## Handoff / Resume Notes

- Last known good state: ...
- Next recommended step: ...
- Open blocker or ambiguity: ...

## Pre-flight

- [ ] Read the design spec fully before starting
- [ ] Create a git worktree for this feature branch when isolation matters
- [ ] Verify the intended local validation command(s) succeed before major edits
- [ ] Record any environment or runtime prerequisite needed for verification

## Context and Orientation

Describe the current state relevant to this task as if the reader knows nothing. Name the key files and directories by full repository-relative path. Define any non-obvious terms you use.

## File structure map

### Files to modify

| File | Purpose | Planned change |
|------|---------|----------------|
| `path/to/file.ext` | ... | ... |

### Files to create

| File | Responsibility |
|------|----------------|
| `path/to/new-file.ext` | ... |

## Chunk 1: [Name]

### Tasks

- [ ] **Task 1.1:** Description
  - Acceptance criteria: what must be true when done
  - Verify: exact command and expected observable result

- [ ] **Task 1.2:** Description
  - Acceptance criteria: what must be true when done
  - Verify: exact command and expected observable result

### Chunk checkpoint

- [ ] All tasks complete
- [ ] Validation recorded in `Validation Log`
- [ ] Progress, decisions, and handoff notes updated
- [ ] Commit with message: `feat: chunk 1 description`

## Chunk 2: [Name]

(Same structure as Chunk 1)

---

## Final validation

- [ ] Run the exact local validation commands required for this change and record them in `Validation Log`
- [ ] Perform a manual smoke test when runtime-facing behavior changed
- [ ] No TODO/FIXME left unresolved in touched files
- [ ] Documentation updated if needed
- [ ] `Handoff / Resume Notes` reflects the true end state
