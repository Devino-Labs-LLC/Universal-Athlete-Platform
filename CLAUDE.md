# Claude Code — Athlete Readiness host adapter

You are Claude Code working in the Universal Athlete Platform repository (commercial product: Athlete Readiness, Devino Labs LLC).

## Authority

1. Obey root [`AGENTS.md`](AGENTS.md). It is the engineering constitution.
2. Use [`docs/agents/`](docs/agents/) as the **canonical** role definitions. Do not invent a Claude-only team.
3. `apps/mobile/AGENTS.md` is only the Expo SDK version pin for the mobile package.

## Orchestration

You may implement work yourself or delegate to project subagents in `.claude/agents/`. Those files are adapters. The same ten roles exist for Codex, Cursor, and Copilot.

When delegating, instruct the subagent to:

1. obey `AGENTS.md`;
2. read its matching `docs/agents/<role>.md`;
3. inspect the real repository before deciding;
4. preserve concurrent work;
5. return useful results.

Use parallel subagents **only** when workstreams are genuinely independent (different files, different bounded contexts). Do not parallel-write the same files.

Require **QA / Test Automation** and **Security / Code Quality** (Independent Quality Gate Steward) review before declaring a major task complete. V2 Sonar baseline: [`docs/quality/SONAR_V2_BASELINE.md`](docs/quality/SONAR_V2_BASELINE.md).

## Product constraints

- Athlete V1 is frozen unless the change is a confirmed defect, security/quality correction, or explicitly approved later-version work.
- Do not begin V2 unless the user explicitly requests it.
- Do not push, merge, deploy, tag, or publish unless the user explicitly requests it.
- Pull requests are optional and **not the default**. Do not create a PR, install GitHub CLI, or create a feature branch solely for PR ceremony. Normal work integrates on `develop`; `main` is stable/release-ready. Obey the solo-developer Git policy in `AGENTS.md`.

## Verification

Use repository scripts, not invented replacements:

- Web: `pnpm web:typecheck`, `pnpm web:lint`, `pnpm web:test`, `pnpm web:build`
- Mobile: `pnpm mobile:typecheck`, `pnpm mobile:lint`, `pnpm mobile:test`, `pnpm mobile:export`
- Backend: `./gradlew test` and `./gradlew check` from `backend/uap-server`
