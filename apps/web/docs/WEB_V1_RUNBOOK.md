# Universal Athlete Web — V1 Runbook

Vite + React SPA for Universal Athlete Platform v1 (`uap_web`).

## Compatibility matrix

| Contract | Version |
| --- | --- |
| Client contract | `V1` |
| Readiness | `READINESS_V1` |
| Recommendation | `TRAINING_RECOMMENDATION_V1` |
| Backend Flyway | `V29` |
| Web minimum backend | Backend v1 RC with Flyway ≥ V29 and client contract V1 |

Runtime negotiation uses bootstrap `clientContractVersion === 'V1'`. No additional semver negotiation.

Supported browser expectation: current Chromium-based desktop browsers. Cross-engine (Safari/Firefox) QA remains a pre-release operational gate.

## Development startup

1. Use Node.js **22.13 or newer** (the workspace pins pnpm 11).
2. Start Spring Boot backend (`backend/uap-server`) on port `8080`.
3. From repo root:

```bash
pnpm web:dev
```

Vite serves on **port 3000** and proxies `/api` → `http://localhost:8080`.

### Environment variables

| Variable | Required | Notes |
| --- | --- | --- |
| `VITE_UAP_ENV` | Yes in release builds | `development` \| `staging` \| `production` |
| `VITE_UAP_API_BASE_URL` | Yes for staging/production | Must be `https://…`; localhost forbidden outside development |

Copy `.env.example` → `.env.local` for local overrides.

### Fail-closed release rules (`src/app/config/env.ts`)

- Vite release builds and production runtime startup **reject** missing `VITE_UAP_ENV`.
- Release builds **reject** `VITE_UAP_ENV=development`.
- Staging/production require `VITE_UAP_API_BASE_URL`.
- Staging/production reject malformed/non-HTTPS URLs, loopback hosts, and embedded URL credentials.
- Development may omit API URL (empty string → same-origin relative `/api` via Vite proxy).

## Browser authentication

- Cookie session: `uap_at` / `uap_rt` (+ readable `XSRF-TOKEN` when not HttpOnly)
- Axios `withCredentials: true`
- CSRF header: `X-XSRF-TOKEN` on `POST`/`PUT`/`PATCH`/`DELETE` (login/register/verify-email exempt)
- Refresh is single-flight at `POST /api/v1/identity/refresh`
- Original failed request retries at most once (`__uapRetried`)
- Refresh failure tears down local session (`EXPIRED`) and clears TanStack Query cache (cancel → clear)
- Logout / logout-all clear CSRF memory + QueryClient

## Local proxy / CORS

- Frontend origin: `http://localhost:3000`
- Backend CORS default must allow that origin with credentials
- Methods include `GET,POST,PUT,PATCH,DELETE,OPTIONS`
- Allowed request headers include `Content-Type` and `X-XSRF-TOKEN`; the CSRF response header is exposed
- Prefer Vite proxy in development so cookies stay same-origin

## Production SPA deployment

Static hosting of `apps/web/dist` via `pnpm run start` (`serve -s`, bind `0.0.0.0:$PORT`).  
`vite preview` is for local checks only — not the Railway production process.

1. SPA fallback: unknown paths → `index.html` (`serve -s`)
2. Cache-Control:
   - hashed `/assets/*` → long-lived immutable
   - `index.html` → short / no-cache
3. HTTPS only (platform/edge)
4. Set `VITE_UAP_ENV=production` and `VITE_UAP_API_BASE_URL=https://…` at **build** time
5. Backend CORS allowlist = exact web origin(s); no wildcard with credentials
6. Secure cookies (`Secure`, `HttpOnly` for auth cookies)

### Railway web service

| Setting | Value |
|---------|-------|
| Root Directory | `/apps/web` |
| Build Command | `pnpm run build` |
| Start Command | `pnpm run start` |
| Healthcheck | `/` |

### Edge security headers (host/CDN)

Documented expectations — set at the edge, not inside the SPA bundle:

- `Strict-Transport-Security`
- `Content-Security-Policy` (restrict scripts/styles/connect to known origins)
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options` / CSP `frame-ancestors`
- `Referrer-Policy`
- `Permissions-Policy`

## Core user flows

### New athlete

Register → verify email → login → profile → sports → goals → Home.

### Planning (desktop)

Training → create plan → workout days → prescriptions → schedule activate → generate → calendar → occurrence detail.

### Catalog / environments

Exercise Catalog (SYSTEM read-only; custom create/edit/archive) · Environments (CRUD, default, compatibility).

### Analytics

Recovery (history, baselines 7/14/28, trends, readiness, guidance, athlete-state) · Performance (PRs, exercise history, load OCCURRENCE/DAILY/WEEKLY). Null session RPE must not render as zero.

## Common issues

| Symptom | Likely cause |
| --- | --- |
| Network errors on boot | Backend down / wrong API URL |
| CSRF 403 on mutations | Missing `XSRF-TOKEN` / cookie mismatch; hard refresh after backend restart |
| Endless login loop | Refresh failure; clear site data |
| Staging/prod boot crash | Missing `VITE_UAP_ENV` or API URL; `development` used in release build |
| CORS errors without proxy | Hitting `:8080` directly from `:3000` without CORS allowlist |
| Cross-account flash | Fixed by cancelQueries + clear + onboarding snapshot clear on REFRESHING |
| Contract mismatch page | Bootstrap `clientContractVersion` ≠ `V1` |
| pnpm fails before running a script | Active Node is older than 22.13; switch to the workspace-supported Node runtime |

## Testing

From repo root:

```bash
pnpm web:test
pnpm web:typecheck
pnpm web:lint
pnpm web:build
```

For a release build, provide explicit build-time values, for example:

```bash
VITE_UAP_ENV=production \
VITE_UAP_API_BASE_URL=https://api.example.com \
pnpm web:build
```

RC-focused tests live under `src/__tests__/rc/`.

### RC coverage map (W6)

| ID | Focus | Primary coverage |
| --- | --- | --- |
| RC01 | Unauthenticated → login | `RC01.unauthenticatedRouteGuard.test.tsx` |
| RC02 | Authenticated refresh → shell | `AuthSessionProvider.test.tsx` |
| RC03 | Refresh failure teardown | `refreshSingleFlight.test.ts` + `RC03.clearLocalAuthStateTeardown.test.ts` |
| RC04 | Cross-account isolation | `RC04.crossAccountIsolation.test.ts` + `AthleteOnboardingProvider.test.tsx` |
| RC05 | Onboarding resume | `resumeMidOnboarding.test.ts` |
| RC06 | Today populated | `HomePage.test.tsx` + home dashboard fixtures/schema tests |
| RC07 | Planner schedule contracts | `trainingApi.test.ts` + `ScheduleManagementPage.test.tsx` |
| RC08 | Snapshot isolation | `snapshotIsolation.test.ts` + occurrence schema tests |
| RC09 | Calendar/occurrence deep-link | `PrimaryWorkoutCard.deepLink.test.tsx` + router route coverage |
| RC10 | Custom exercise lifecycle | `scopePolicy.test.ts` + exercise page/API suites |
| RC11 | Environment lifecycle | environment page/form/API suites |
| RC12 | Recovery null safety | recovery schemas/formatters/page suites |
| RC13 | Readiness null-score | readiness schema/page/label suites |
| RC14 | PR canonical identity | `PersonalRecordsTable.test.tsx` + performance query-key/API suites |
| RC15 | Load unrated ≠ zero | `RC15.unratedLoadNotZero.test.ts` |
| RC16 | Malformed API safe failure | `RC16.malformedApiResponse.test.ts` |
| RC17 | Dirty form preservation | `RC17.dirtyFormKeepsValues.test.tsx` |
| RC18 | Production config fail-closed | `RC18.productionConfigFailClosed.test.ts` |
| RC19 | Search-param reload/back-forward safety | `RC19.malformedSearchParams.test.tsx` + recovery/load page tests |
| RC20 | Logout / Back cannot reveal protected | `RC20.logoutClearsProtectedData.test.ts` + `RC20.logoutBackGuard.test.tsx` |

## Browser cookie/CSRF acceptance checklist

Use live backend + Chromium:

1. Login
2. Auth cookies issued
3. `GET /api/v1/identity/me` succeeds
4. CSRF-protected POST succeeds
5. PATCH/PUT succeeds
6. A safe fixture DELETE succeeds
7. Refresh path succeeds after access expiry (or forced 401)
8. Browser reload restores session
9. Logout → protected route redirects to login; Back does not reveal protected content
10. Login again → bootstrap V1 → Today loads

Document browser, frontend origin, backend origin, cookie behavior, XSRF, CORS, refresh, and reload persistence for each RC.

## Feature freeze (post-W6)

**WEB V1 FEATURE FROZEN**

Allowed: P0/P1 defects, security fixes, production configuration, deployment blockers, accessibility blockers, browser integration defects.

Not allowed: new planner features, new analytics, live web workout execution, polish-only charts, AI, offline, notifications, new backend preference fields.

## Remaining release gates (non-code / ops)

- Live browser cookie/CSRF E2E smoke against deployed backend
- Production domain, TLS, Flyway apply
- Privacy policy / terms / support contact
- Monitoring / error tracking
- Cross-browser QA (Safari/Firefox) if required by release policy
