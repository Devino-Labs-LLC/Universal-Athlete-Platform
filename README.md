# Athlete Readiness

**Athlete Readiness**, powered by **Devino Labs LLC**, is a performance and readiness platform for individual athletes. This repository is the Universal Athlete Platform (UAP) codebase that implements it.

The product helps athletes understand preparedness, recovery, training, and progress. It is **not** a substitute for licensed medical care and does not make medical diagnoses.

Internal package and module names remain Universal Athlete Platform / UAP.

## Architecture

Clients are thin. The Spring Boot server owns identity, authorization, persistence, and derived athlete state (the State Engine). Web and mobile consume the published HTTP API. Read-only facades do not secretly create snapshots, readiness assessments, recommendations, workouts, billing records, or integration records.

```
apps/web          React SPA  →  HTTPS /api
apps/mobile       Expo app   →  HTTPS /api
backend/uap-server Spring API, domain, Flyway
```

Server-side authorization is the security boundary. UI visibility is never authorization.

## Stack

| Surface | Stack |
| --- | --- |
| Backend | Java 21, Spring Boot, Flyway, MySQL (`backend/uap-server`) |
| Web | React, TypeScript, Vite (`apps/web`) |
| Mobile | React Native, Expo, TypeScript (`apps/mobile`) |

## Local development

Prerequisites: Node.js 22.13+, pnpm 11 (workspace pin), Java 21, a local MySQL instance configured as in `backend/uap-server/.env.example`.

1. Copy example env files. Do not commit real `.env` files.
2. Start the API from `backend/uap-server` (default `http://127.0.0.1:8080`).
3. Web: from the repo root, `pnpm web:dev` (Vite on port 3000, `/api` proxied to the API).
4. Mobile: from the repo root, `pnpm mobile:start` with an Expo development build.

Production web builds require `VITE_UAP_ENV=production` and an HTTPS `VITE_UAP_API_BASE_URL`. Do not use localhost API URLs outside development. See `apps/web/docs/WEB_V1_RUNBOOK.md` and `apps/mobile/docs/MOBILE_V1_RUNBOOK.md`.

## Verification

Use the repository scripts. Do not invent replacements.

| Area | Commands |
| --- | --- |
| Web | `pnpm web:typecheck`, `pnpm web:lint`, `pnpm web:test`, `pnpm web:build` |
| Mobile | `pnpm mobile:typecheck`, `pnpm mobile:lint`, `pnpm mobile:test`, `pnpm mobile:export` |
| Backend | `./gradlew test` and `./gradlew check` from `backend/uap-server` |

GitHub Actions **Verify** (`.github/workflows/verify.yml`) runs those gates and the Sonar quality gate on trusted same-repository pushes to `develop` and `main`.

## Security

Report vulnerabilities privately. Do not open a public issue that includes secrets, credentials, athlete data, or exploit details. See [SECURITY.md](SECURITY.md).

## License

Copyright (c) 2026 Devino Labs LLC. All rights reserved. See [LICENSE](LICENSE).
You may view and evaluate this repository. Copying, modifying, distributing, sublicensing, selling, or incorporating the software into another product requires prior written permission from Devino Labs LLC. This repository is not open source.

`apps/mobile/LICENSE` is Expo / 650 Industries template attribution only. It is not the governing license for this repository.

If this repository is temporarily public, that does not grant those rights. See [docs/TEMPORARY_PUBLIC_VISIBILITY.md](docs/TEMPORARY_PUBLIC_VISIBILITY.md).
