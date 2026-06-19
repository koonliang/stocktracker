<!--
SYNC IMPACT REPORT
==================
Version change: 1.0.0 → 2.0.0
Bump rationale: Removed lint compliance as a non-negotiable principle and
redefined test verification from automated pass/fail completion to automated test
creation with manual verification.

Modified principles:
  - I. Test Verification (NON-NEGOTIABLE) → I. Automated Tests & Manual Verification (NON-NEGOTIABLE)
  - II. Lint & Style Compliance (NON-NEGOTIABLE) → removed
  - III. Compilation Integrity (NON-NEGOTIABLE) → II. Compilation Integrity (NON-NEGOTIABLE)
  - IV. Simplicity & YAGNI → III. Simplicity & YAGNI
  - V. Specification-Driven Development → IV. Specification-Driven Development

Added sections: none

Removed sections:
  - Lint & Style Compliance principle
  - Lint gate from Quality Gates

Templates requiring updates:
  - .specify/templates/plan-template.md ✅ compatible (generic Constitution Check gate)
  - .specify/templates/spec-template.md ✅ compatible (manual Independent Test descriptions remain required)
  - .specify/templates/tasks-template.md ✅ updated (automated tests are created; verification is manual)
  - README.md ✅ updated (quality gate commands no longer list lint/format checks)
  - AGENTS.md ✅ compatible (delegates to current plan)

Follow-up TODOs: none
-->

# StockTracker Constitution

## Core Principles

### I. Automated Tests & Manual Verification (NON-NEGOTIABLE)

Every feature MUST include automated tests exercising its observable behavior.
Those tests MUST be committed with the feature, but verification of whether the
feature is complete is performed manually by the contributor or reviewer using
the feature's documented acceptance scenarios and relevant test evidence. A
feature without corresponding automated tests is not complete. Failing, skipped,
or disabled tests MUST be explicitly reviewed during manual verification before
merge.

**Rationale**: Automated tests preserve regression coverage, while manual
verification confirms that the implemented behavior matches the product intent
and acceptance criteria beyond raw test execution.

### II. Compilation Integrity (NON-NEGOTIABLE)

The project MUST compile (or, for interpreted stacks, pass equivalent type/syntax
checks such as `tsc --noEmit`, `mypy`, etc.) with zero errors before a feature is
considered complete. Broken builds MUST NOT be merged. If a build step is added,
removed, or modified, the change MUST document how to run it.

**Rationale**: A non-compiling main branch blocks every other contributor and
every deployment. Compilation is the cheapest feedback loop and MUST always be
green.

### III. Simplicity & YAGNI

Prefer the simplest design that satisfies the specified requirements. Do not add
abstractions, configuration knobs, or speculative features that are not required
by a current, documented need. Three similar lines are preferable to a premature
abstraction. Complexity MUST be justified against a concrete requirement.

**Rationale**: Unused flexibility carries real maintenance cost and obscures the
actual behavior. Simpler code is easier to test, verify manually, and reason
about.

### IV. Specification-Driven Development

Non-trivial features MUST flow through the Spec Kit workflow: specification →
plan → tasks → implementation. Specifications describe *what* and *why*; plans
and tasks describe *how*. Implementation MUST trace back to an approved spec;
scope changes MUST be reflected in the spec before code changes land.

**Rationale**: Spec-first work keeps intent, design, and implementation aligned
and makes review, estimation, and future amendments tractable.

## Quality Gates

Before any feature is marked complete, the following gates MUST be satisfied:

- **Automated test artifact gate**: automated tests covering the feature's
  observable behavior are created and committed with the implementation.
- **Manual verification gate**: the contributor or reviewer manually verifies the
  feature against its acceptance scenarios and records the test evidence used.
- **Compile gate**: the project builds or type-checks with zero errors.

A feature that fails any gate is not complete, regardless of perceived progress.
Gate failures MUST be fixed by addressing the underlying cause. Disabling tests
or skipping a build step to "unblock" work is prohibited without explicit,
documented approval.

## Development Workflow

- Features follow the Spec Kit sequence: `/speckit-specify` → `/speckit-plan` →
  `/speckit-tasks` → `/speckit-implement`.
- Each pull request MUST state which automated tests were added or changed, how
  manual verification was performed, and how compilation was verified.
- Code review MUST verify alignment with Principles I–IV; reviewers MAY block a
  PR that fails any principle.
- Changes to shared build or test-runner configuration are governance-relevant:
  they MUST be called out explicitly in the PR description.

## Governance

This constitution supersedes ad-hoc practices and informal conventions within
the project. In case of conflict between this document and other guidance, this
document wins until amended.

- **Amendments**: proposed via PR editing `.specify/memory/constitution.md`,
  including a Sync Impact Report and a version bump.
- **Versioning policy** (semantic):
  - **MAJOR**: backward-incompatible removal or redefinition of a principle or
    governance rule.
  - **MINOR**: new principle or section added, or materially expanded guidance.
  - **PATCH**: clarifications, wording, or non-semantic refinements.
- **Compliance review**: every PR review MUST confirm the Quality Gates were
  satisfied and that no principle was violated without explicit justification.
- **Runtime guidance**: contributors consult `CLAUDE.md` and the active feature
  plan under `specs/<feature>/plan.md` for day-to-day execution details.

**Version**: 2.0.0 | **Ratified**: 2026-04-24 | **Last Amended**: 2026-06-18
