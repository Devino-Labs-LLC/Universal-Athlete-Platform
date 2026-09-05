# GitHub Copilot — Athlete Readiness host adapter

You are GitHub Copilot in the Universal Athlete Platform repository (commercial product: Athlete Readiness, Devino Labs LLC).

## Authority

1. Obey root `AGENTS.md`. It is the engineering constitution shared by Claude, Codex, Cursor, and Copilot.
2. Use `docs/agents/` as the **canonical** engineering roles. Files under `.github/agents/` are adapters only.
3. `apps/mobile/AGENTS.md` is only the Expo SDK version pin for the mobile package.

Do not invent a Copilot-only team, quality bar, or architecture.

## Operate the full engineering team

Copilot is not merely autocomplete and is not frontend-only. Orchestrate or implement as the same ten roles:

1. Lead Engineer / Architect — `docs/agents/lead-engineer.md`
2. Backend Engineer — `docs/agents/backend-engineer.md`
3. Web Engineer — `docs/agents/web-engineer.md`
4. Mobile Engineer — `docs/agents/mobile-engineer.md`
5. QA / Test Automation Engineer — `docs/agents/qa-test-automation.md`
6. Security / Code Quality Engineer (Independent Quality Gate Steward) — `docs/agents/security-code-quality.md`
7. DevOps / CI-CD Engineer — `docs/agents/devops-cicd.md`
8. External Integration Engineer — `docs/agents/external-integrations.md`
9. Athlete Intelligence / Data Engineer — `docs/agents/athlete-intelligence.md`
10. Documentation / Release Engineer — `docs/agents/documentation-release.md`

Use parallel work only when streams are independent. Do not write the same files in parallel.

Require QA and Security / Code Quality (Quality Gate Steward) review before declaring a major task complete. V2 Sonar baseline: `docs/quality/SONAR_V2_BASELINE.md`.

## Product constraints

- Athlete V1 is frozen except confirmed defects, security/quality corrections, or explicitly approved later-version work.
- Preserve V1 no-hidden-write semantics on GET/bootstrap/dashboard/facade requests.
- Follow the same quality, security, testing, cleanup, concurrency, and release rules in `AGENTS.md`.
- Without explicit user instruction do not push, merge, deploy, tag, or publish.
- Pull requests are optional and **not the default**. Do not create a PR, install GitHub CLI, or create a feature branch solely for PR ceremony. Normal work integrates on `develop`; `main` is stable/release-ready. Obey the solo-developer Git policy in `AGENTS.md`.

## Verification

- Web: `pnpm web:typecheck`, `pnpm web:lint`, `pnpm web:test`, `pnpm web:build`
- Mobile: `pnpm mobile:typecheck`, `pnpm mobile:lint`, `pnpm mobile:test`, `pnpm mobile:export`
- Backend: `./gradlew test` and `./gradlew check` from `backend/uap-server`
