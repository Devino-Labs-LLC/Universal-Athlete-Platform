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

## Solo-developer Git and pull-request policy

This repository is currently maintained primarily by a single developer.

Pull requests are **not prohibited**. They are also **not the default** development workflow.

### Default workflow

Pull requests are **not required by default**.

For normal solo development, prefer the simplest appropriate workflow:

1. Inspect the current branch and working tree.
2. Implement the scoped work.
3. Run required tests and quality gates.
4. Review the diff.
5. Commit when authorized.
6. Push the authorized branch directly when authorized.
7. Allow GitHub Actions / Sonar to verify the pushed commit where the workflow actually runs on that push.

Do not create a pull request simply because a remote branch was pushed.
Do not create a feature branch solely so a pull request can exist.
Do not introduce PR-based review ceremony when a direct commit or push is sufficient.

### Pull requests are optional, not forbidden

A pull request may still be appropriate later, for example when:

- another developer begins contributing;
- the user wants isolated code review;
- a risky or unusually large change benefits from a separate review boundary;
- branch-protection rules require a PR;
- an external contributor is involved;
- the user explicitly wants a PR for a release or change;
- a GitHub feature being used intentionally requires a PR workflow.

In those situations, **do not automatically create the PR**. Explain briefly:

1. why a PR would be beneficial or required;
2. which branch would be involved;
3. whether any additional tooling is actually necessary.

Then **ask the user** before creating it.

### Tool installation

Do **not** install GitHub CLI (`gh`), GitHub extensions, Git workflow utilities, package-manager tools, or IDE plugins solely to create or manage a pull request unless the user explicitly approves that installation.

If a requested operation cannot be performed with existing repository or already-installed tooling, report the limitation and ask before installing software. Do not work around this by silently downloading an alternative executable.

### Branch policy

This repository uses a lightweight solo-maintainer branch model.

- **`main`** is the stable / release-ready application baseline. Do not use it for normal ongoing implementation. Promotion into `main` is an explicit release or integration decision and requires explicit user authorization. A pull request is **not** required for that promotion.
- **`develop`** is the normal integration and development branch. Active version work, release-candidate hardening, GitHub Actions verification, and Sonar analysis run here. Direct push to `develop` is acceptable when the user explicitly authorizes that push.
- **`feat/*` (and similar) are optional.** Use a feature branch only when isolation provides real value: risky work, parallel agent work, large separated changes, experimentation, or hotfix/release isolation. Do not create a feature branch merely because conventional team workflows use one. Do not require a pull request to merge a feature branch into `develop`.

Use the current authorized branch unless:

- branch isolation materially reduces risk;
- parallel agent work requires isolation;
- the user requests a branch;
- a release or hotfix strategy requires one;
- repository protection rules require one.

If creating a branch changes the expected workflow materially, explain the reason.

### CI / Sonar does not imply a pull request

GitHub Actions and Sonar verification do **not** inherently require a pull request.

The Verify workflow is intended to run on **push to `develop`** (normal pre-main verification) and **push to `main`** (stable/release baseline). Pull-request events may still run the same workflow when a PR is intentionally used; PRs are not required to start CI.

When remote CI/Sonar verification is needed and direct push is authorized:

- push the authorized branch (`develop` for normal development);
- let the configured workflow run;
- inspect the workflow result;
- remediate legitimate failures;
- push corrections if authorized.

Do not create a PR merely to trigger CI.

If CI is accidentally configured so Sonar or Verify only runs on pull-request events, evaluate whether a `develop` / `main` push trigger is more appropriate. Do not modify workflow triggers blindly; preserve intentional release and security behavior. Report the finding and ask before changing triggers or creating a PR.

Do not configure every arbitrary feature branch to run expensive Sonar analysis unless there is a justified reason.

### Commit, push, PR, merge, and deploy are separate permissions

Never treat these as interchangeable.

- Authorization to **commit** does not authorize **push**.
- Authorization to **push** does not authorize creating a PR, merge, deploy, tag, publish, or production changes.
- Authorization to **create a PR** does not authorize **merging** it.
- Authorization to **merge** does not authorize **deployment** unless an explicitly approved pipeline makes that consequence clear.

When in doubt about a consequential action, preserve the current state and report the next action rather than assuming permission.

## Git / release safety

Without **explicit** user instruction do **not**:

- push;
- create, reopen, update, or otherwise manage a pull request;
- merge;
- deploy;
- create or move tags;
- publish releases;
- mutate production databases;
- rotate secrets;
- change production infrastructure;
- submit app-store releases.

Implementation completion does not authorize release, push, or a pull request.

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
