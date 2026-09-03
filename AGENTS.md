# Athlete Readiness — Engineering Constitution

This file is the **canonical, vendor-neutral** instruction set for every AI coding environment working in this repository.

Claude Code, OpenAI Codex, Cursor, GitHub Copilot, and any other approved coding agent **expose the same logical engineering team**. No AI vendor owns a discipline. Platform-native files under `.claude/`, `.codex/`, `.cursor/`, and `.github/` are adapters only.

Canonical role definitions live in [`docs/agents/`](docs/agents/).

`apps/mobile/AGENTS.md` is an Expo SDK version pin for the mobile package. It does **not** replace this file.

---

## Product

- Commercial direction: **Athlete Readiness**, powered by **Devino Labs LLC**.
- Internal repository, packages, and modules remain **Universal Athlete Platform / UAP** until an explicit rename is approved.
- Do not perform broad product/package/repository renames merely because the commercial name is changing.

### Athlete V1 freeze

Athlete V1 is **frozen**. It is the completed initial athlete web/mobile foundation.

Change V1 only for:

- confirmed defects;
- security corrections;
- quality corrections required for production;
- explicitly approved later-version work (V2, V3, …).

Do not casually rewrite completed V1 functionality.

---

## Logical engineering team

The orchestrator (whichever AI product is hosting the session) must select and coordinate these roles as needed:

1. Lead Engineer / Architect — [`docs/agents/lead-engineer.md`](docs/agents/lead-engineer.md)
2. Backend Engineer — [`docs/agents/backend-engineer.md`](docs/agents/backend-engineer.md)
3. Web Engineer — [`docs/agents/web-engineer.md`](docs/agents/web-engineer.md)
4. Mobile Engineer — [`docs/agents/mobile-engineer.md`](docs/agents/mobile-engineer.md)
5. QA / Test Automation Engineer — [`docs/agents/qa-test-automation.md`](docs/agents/qa-test-automation.md)
6. Security / Code Quality Engineer — [`docs/agents/security-code-quality.md`](docs/agents/security-code-quality.md)
7. DevOps / CI-CD Engineer — [`docs/agents/devops-cicd.md`](docs/agents/devops-cicd.md)
8. External Integration Engineer — [`docs/agents/external-integrations.md`](docs/agents/external-integrations.md)
9. Athlete Intelligence / Data Engineer — [`docs/agents/athlete-intelligence.md`](docs/agents/athlete-intelligence.md)
10. Documentation / Release Engineer — [`docs/agents/documentation-release.md`](docs/agents/documentation-release.md)

Multiple roles may collaborate on one task. Use parallel subagents only when workstreams are genuinely independent. Do not write the same files in parallel.

Require QA and Security / Code Quality review before declaring a major task complete.

---

## Source of truth (precedence)

When sources conflict, **investigate**. Do not invent a third interpretation.

1. Approved ADRs / architecture decisions (when present)
2. Current API / domain contracts (including `docs/TRAINING_API_V1.md` and related handoffs)
3. Current repository implementation
4. Database migrations (Flyway under `backend/uap-server`)
5. Automated tests
6. Approved Figma design
7. Current ClickUp implementation scope
8. Historical conversational context

---

## Required pre-work

Before changing code:

1. Inspect the current branch (`git status`, `git branch`).
2. Preserve all existing work. Never discard another AI or developer’s changes.
3. Read applicable repository instructions and the relevant `docs/agents/` role.
4. Inspect current implementation, callers, tests, and existing equivalents.
5. Determine authorization, database/API, and concurrency implications.

Do not code from assumptions.

---

## Engineering quality

- **No dead code** in touched execution paths: unused methods, classes, variables, imports, obsolete branches, unreachable logic, abandoned helpers, replaced implementations, commented-out code, stale experiments.
- **No duplicate implementations.** Search first. Do not create a second validator, auth check, date helper, API client, status calculator, DTO mapper, readiness calculation, query-key set, or UI primitive that already exists.
- If duplication exists in code you are touching, refactor toward one canonical implementation when safe.
- Git already stores history. Do not leave old code “just in case.”
- Write as though SonarQube / SonarCloud will inspect immediately: manageable cognitive complexity, no giant methods/components, no swallowed exceptions, no empty catches, no unsafe null/casts, no magic constants for domain concepts, no resource leaks, no debug logging of sensitive data, no broad static-analysis suppressions.
- Fix **root cause**, not symptoms. Determine what failed, why, which layer owns it, whether sibling paths are affected, and whether a regression test can reproduce it.
- **Clean as you go** in the same file, same execution path, or directly adjacent code. Do not turn a task into a repository-wide refactor.

---

## Security

- Server-side authorization is the security boundary. **UI visibility is never authorization.**
- Validate authenticated principal, actor role, ownership, tenant/team/org relationship, consent, and resource access before acting on a client-supplied ID.
- Prevent IDOR. Do not leak existence of foreign resources.
- Least privilege. Coaches, organizations, sponsors, parents, and other roles do **not** automatically receive all athlete information. Consent and authorization determine visibility.
- Never log or return passwords, auth tokens, secrets, provider credentials, or sensitive athlete data.
- Athlete Readiness is a performance/readiness platform, not a substitute for licensed medical care. Do not make unsupported medical diagnoses.

---

## Configuration

- Required configuration **fails fast**.
- Never introduce silent production-like fallbacks (`ENV || "https://prod…"`) for secrets, signing keys, credentials, database config, payment/provider credentials, encryption keys, production hosts, or security settings.
- Optional settings may have intentional, documented defaults.
- Do not fake provider or integration success.

Current verification-relevant commands (use these; do not invent replacements):

| Area | Commands |
| --- | --- |
| Web | `pnpm web:typecheck`, `pnpm web:lint`, `pnpm web:test`, `pnpm web:build` |
| Mobile | `pnpm mobile:typecheck`, `pnpm mobile:lint`, `pnpm mobile:test`, `pnpm mobile:export` |
| Backend | `./gradlew test` and `./gradlew check` from `backend/uap-server` |

Production web builds must set `VITE_UAP_ENV=production` and an HTTPS `VITE_UAP_API_BASE_URL`. Do not use localhost API URLs outside development.

---

## Database

- Flyway is the canonical migration mechanism unless architecture explicitly changes.
- Migrations must be deterministic, ordered, reviewable, and safe against realistic existing data.
- Use transactions where partial writes could corrupt state.
- Do not perform destructive production-data changes without explicit approval.

---

## Concurrency and idempotency

For important mutations ask: **what happens if this runs twice?**

Consider double-click/tap, request retry, stale clients, concurrent API calls, background-job retry, and webhook replay.

Use unique constraints, transactional guards, optimistic concurrency (`expectedVersion` / version checks), locking, or idempotency keys **where justified**. Do not add locks everywhere.

---

## V1 API contract — no hidden writes

Preserve V1 no-hidden-write semantics.

GET, bootstrap, dashboard, and other read-only facade calls must **not** secretly create:

- athlete state snapshots;
- readiness assessments;
- recommendations / guidance;
- workouts / occurrences;
- billing records;
- integration records.

Mutations remain **explicit** unless an approved newer contract changes this architecture. Do not casually break existing clients.

---

## External providers

Keep provider-specific clients, credentials, DTOs, mapping, and sync state behind integration boundaries.

Canonical Athlete Readiness domain models must not depend directly on provider-specific types (HealthKit, Health Connect, Garmin, WHOOP, Fitbit, Polar, nutrition, music, or other approved services).

Handle outages, rate limits, retries, duplicate callbacks, token expiration, and partial sync. Do not fake success.

---

## Frontend quality

Web and mobile work must handle loading, empty, success, validation, API failure, unauthorized, forbidden, not found, conflict, disabled actions, repeated taps/clicks, accessibility, and realistic content sizes.

Reuse existing typography, spacing, color tokens, buttons, forms, cards, and navigation. Do not introduce a parallel design system or styling framework for convenience.

Purple / AI accent tokens are reserved for future AI concepts. Do not use them on current athlete surfaces.

---

## Tests

Tests are production code.

Never:

- delete a legitimate failing test to make CI green;
- weaken assertions to accept a regression;
- skip a test because implementation is inconvenient;
- hide failure behind `|| true`;
- mock away the behavior being tested;
- add arbitrary sleeps where deterministic synchronization exists.

Bug fixes should receive a regression test that fails before the fix when practical.

New behavior needs happy-path, invalid input, authorization/ownership, boundary, state-transition, repeated-call, and failure coverage as applicable.

---

## Multi-agent / worktree safety

The repository may be edited by Claude, Codex, Cursor, Copilot, and human developers concurrently.

Never:

- `git reset --hard`;
- discard another developer/agent’s work;
- mass-revert unrelated files;
- run broad formatting across unrelated areas;
- assume all working-tree changes belong to the current agent.

Minimize shared-file churn.

---

## Git / release safety

Without **explicit** user instruction do **not**:

- push;
- merge;
- deploy;
- create or move tags;
- publish releases;
- mutate production databases;
- rotate secrets;
- change production infrastructure;
- submit app-store releases.

Implementation completion does not authorize release.

---

## Completion standard

A task is complete only when:

- requested behavior is implemented and root cause is resolved;
- architecture remains consistent;
- no known dead or duplicate code was introduced in touched paths;
- adjacent discovered defects were cleaned when safe;
- tests exist and relevant tests pass;
- applicable typecheck, lint, and builds pass;
- security, concurrency, and Sonar-style cleanup were reviewed;
- documentation matches behavior;
- no unrelated changes were destroyed.

Do not claim completion while critical gates remain red.

### Required completion report

1. Objective
2. Root Cause / Design Decision
3. Agents Used
4. Files Changed
5. API Changes
6. Database Changes
7. Security
8. Tests
9. Verification (exact commands and results)
10. Sonar / Cleanup
11. Risks / Remaining Work
