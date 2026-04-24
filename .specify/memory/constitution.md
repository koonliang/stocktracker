<!--
SYNC IMPACT REPORT
==================
Version change: none (template) → 1.0.0
Bump rationale: Initial ratification of the project constitution (MAJOR established).

Modified principles:
  - [PRINCIPLE_1_NAME] → I. Test Verification (NON-NEGOTIABLE)
  - [PRINCIPLE_2_NAME] → II. Lint & Style Compliance (NON-NEGOTIABLE)
  - [PRINCIPLE_3_NAME] → III. Compilation Integrity (NON-NEGOTIABLE)
  - [PRINCIPLE_4_NAME] → IV. Simplicity & YAGNI
  - [PRINCIPLE_5_NAME] → V. Specification-Driven Development

Added sections:
  - Quality Gates
  - Development Workflow
  - Governance

Removed sections: none (all template placeholders resolved)

Templates requiring updates:
  - .specify/templates/plan-template.md ✅ compatible (generic Constitution Check gate)
  - .specify/templates/spec-template.md ✅ compatible (no principle-specific edits needed)
  - .specify/templates/tasks-template.md ✅ compatible (add test/lint/build tasks per principles I–III when generating)
  - CLAUDE.md ✅ compatible (delegates to current plan)

Follow-up TODOs: none
-->

# StockTracker Constitution

## Core Principles

### I. Test Verification (NON-NEGOTIABLE)

Every feature MUST have automated tests exercising its observable behavior, and the
full test suite MUST pass before the feature is considered complete. New or changed
code without a corresponding passing test is not shippable. Failing, skipped, or
disabled tests MUST be resolved or explicitly justified in the PR description prior
to merge.

**Rationale**: Tests are the primary evidence that a feature actually works and does
not regress existing behavior. Without a green suite, "done" is unverifiable.

### II. Lint & Style Compliance (NON-NEGOTIABLE)

The project linter(s) MUST run clean on all changed code before a feature is
considered complete. Warnings introduced by a change MUST be fixed or, where
unavoidable, suppressed locally with an inline justification. Linter configuration
is the single source of truth for style; ad-hoc style debates are out of scope.

**Rationale**: Consistent style and automated static checks catch whole classes of
defects cheaply and keep the codebase readable as it grows.

### III. Compilation Integrity (NON-NEGOTIABLE)

The project MUST compile (or, for interpreted stacks, pass equivalent type/syntax
checks such as `tsc --noEmit`, `mypy`, etc.) with zero errors before a feature is
considered complete. Broken builds MUST NOT be merged. If a build step is added,
removed, or modified, the change MUST document how to run it.

**Rationale**: A non-compiling main branch blocks every other contributor and every
deployment. Compilation is the cheapest feedback loop and MUST always be green.

### IV. Simplicity & YAGNI

Prefer the simplest design that satisfies the specified requirements. Do not add
abstractions, configuration knobs, or speculative features that are not required by
a current, documented need. Three similar lines are preferable to a premature
abstraction. Complexity MUST be justified against a concrete requirement.

**Rationale**: Unused flexibility carries real maintenance cost and obscures the
actual behavior. Simpler code is easier to test, lint, and reason about — directly
supporting Principles I–III.

### V. Specification-Driven Development

Non-trivial features MUST flow through the Spec Kit workflow: specification → plan
→ tasks → implementation. Specifications describe *what* and *why*; plans and tasks
describe *how*. Implementation MUST trace back to an approved spec; scope changes
MUST be reflected in the spec before code changes land.

**Rationale**: Spec-first work keeps intent, design, and implementation aligned and
makes review, estimation, and future amendments tractable.

## Quality Gates

Before any feature is marked complete, the following gates MUST all pass locally
and in CI:

- **Test gate**: full automated test suite executes and passes (no failures, no
  unexplained skips).
- **Lint gate**: configured linters/formatters run clean on all changed files.
- **Compile gate**: the project builds or type-checks with zero errors.

A feature that fails any gate is not complete, regardless of perceived progress.
Gate failures MUST be fixed by addressing the underlying cause — disabling a test,
silencing a lint rule globally, or skipping a build step to "unblock" work is
prohibited without explicit, documented approval.

## Development Workflow

- Features follow the Spec Kit sequence: `/speckit-specify` → `/speckit-plan` →
  `/speckit-tasks` → `/speckit-implement`.
- Each pull request MUST state how the three Quality Gates were verified (commands
  run, results observed).
- Code review MUST verify alignment with Principles I–V; reviewers MAY block a PR
  that fails any principle.
- Changes to shared configuration (linters, build, test runners) are governance
  changes: they MUST be called out explicitly in the PR description.

## Governance

This constitution supersedes ad-hoc practices and informal conventions within the
project. In case of conflict between this document and other guidance, this
document wins until amended.

- **Amendments**: proposed via PR editing `.specify/memory/constitution.md`,
  including a Sync Impact Report and a version bump.
- **Versioning policy** (semantic):
  - **MAJOR**: backward-incompatible removal or redefinition of a principle or
    governance rule.
  - **MINOR**: new principle or section added, or materially expanded guidance.
  - **PATCH**: clarifications, wording, or non-semantic refinements.
- **Compliance review**: every PR review MUST confirm the Quality Gates passed and
  that no principle was violated without explicit justification.
- **Runtime guidance**: contributors consult `CLAUDE.md` and the active feature
  plan under `specs/<feature>/plan.md` for day-to-day execution details.

**Version**: 1.0.0 | **Ratified**: 2026-04-24 | **Last Amended**: 2026-04-24
