# Universal Athlete Platform — Web (W1 + W2 + W3)

Vite + React 19 + TypeScript foundation for the browser client (`uap_web`).

## Stack

- Vite 7, React 19, TypeScript (strict)
- React Router 7
- TanStack Query v5
- Axios, Zod, React Hook Form, `@hookform/resolvers`
- SCSS modules (no Tailwind)
- Vitest, React Testing Library, jsdom, `@testing-library/user-event`
- ESLint flat config

Path alias: `@/` → `src/`.

## Scripts

From repo root:

```bash
pnpm --filter uap_web dev
pnpm --filter uap_web build
pnpm --filter uap_web preview
pnpm --filter uap_web test
pnpm --filter uap_web typecheck
pnpm --filter uap_web lint
```

Or use root shortcuts: `web:dev`, `web:build`, `web:lint`, `web:test`, `web:typecheck`.

## Environment

Copy `.env.example` to `.env.local` (or export vars in your shell).

| Variable | Required | Values |
|----------|----------|--------|
| `VITE_UAP_ENV` | Release builds | `development` \| `staging` \| `production` |
| `VITE_UAP_API_BASE_URL` | Staging/production | HTTPS API origin |

Rules (`src/app/config/env.ts`):

- Release builds fail closed if `VITE_UAP_ENV` is missing.
- Staging/production require `VITE_UAP_API_BASE_URL`.
- `localhost`, `127.0.0.1`, and `10.0.2.2` are forbidden outside development.
- Development defaults to **empty** `apiBaseUrl` (`''`) so Axios uses same-origin relative URLs.

## Dev proxy / CORS decision

- Vite dev server runs on **port 3000** (matches backend CORS default `http://localhost:3000`).
- `/api` is proxied to `http://localhost:8080`.
- Axios uses `withCredentials: true` and relative `/api/...` paths in development.
- This keeps cookies and CSRF on the **same origin** (`localhost:3000`) while the backend serves API on `:8080` behind the proxy.

Production/staging should set `VITE_UAP_API_BASE_URL` to the deployed API origin and configure backend CORS for the web origin.

W1 also corrected backend default CORS **methods** to include `PUT` and `DELETE` (previously only `GET,POST,PATCH,OPTIONS`), which is required for training mutations behind credentialed CORS. Dev still defaults origin to `http://localhost:3000`.

## Cookie auth & CSRF

- Session cookies are managed by the browser (`withCredentials: true`).
- CSRF cookie: `XSRF-TOKEN`; header: `X-XSRF-TOKEN`.
- Token sources: readable `document.cookie`, then response header `X-XSRF-TOKEN` stored in memory (for cases where the cookie is HttpOnly or cross-origin headers arrive without readable cookies).
- CSRF attached on `POST`/`PUT`/`PATCH`/`DELETE`, exempt: login, register, verify-email.

## Refresh single-flight

On `401`, one refresh runs at `POST /api/v1/identity/refresh`:

- Skips login/register/verify-email/refresh paths.
- Uses `__uapRetried` metadata to retry the original request once.
- Concurrent 401s join the same refresh promise.
- Refresh failure calls `onSessionExpired` → local session teardown (`EXPIRED`).

## Routing

| Route | Purpose |
|-------|---------|
| `/` | Bootstrap gate |
| `/auth/login`, `/register`, `/verify-email` | Auth |
| `/app/*` | Authenticated shell |
| `/app/home` | Today dashboard |
| `/app/training` | Training overview landing (W3) |
| `/app/training/plans` | Plan list (W3) |
| `/app/training/plans/new` | Create plan (W3) |
| `/app/training/plans/:planId` | Plan builder (W3) |
| `/app/training/plans/:planId/edit` | Edit plan metadata (W3) |
| `/app/training/plans/:planId/schedule` | Schedule management (W3) |
| `/app/training/calendar` | Training calendar (W3) |
| `/app/training/plans/:planId/days/:dayId/occurrences/:occurrenceId` | Occurrence detail (W3) |
| `/app/recovery`, `/app/performance`, `/app/environments` | Placeholders |
| `/app/profile`, `/app/profile/edit`, `/app/profile/sports`, `/app/profile/goals` | Profile (W2) |
| `/onboarding/profile`, `/onboarding/sports`, `/onboarding/goals` | Onboarding (W2) |
| `/incompatible` | Client contract mismatch |

Guards:

- Unauthenticated → login
- Incompatible bootstrap contract → `/incompatible`
- Authenticated + bootstrap V1 ready + onboarding complete → `/app/home`
- Authenticated + bootstrap V1 ready + onboarding incomplete → `/onboarding/*`

## Bootstrap

`GET /api/v1/training/client/bootstrap` must return `clientContractVersion === 'V1'`.

Statuses: `IDLE`, `BOOTSTRAPPING`, `UNAUTHENTICATED`, `AUTHENTICATED_READY`, `INCOMPATIBLE_CLIENT`, `BOOTSTRAP_ERROR`.

## Onboarding (W2)

Athlete onboarding gates the app after auth + bootstrap V1:

| State | Route |
|-------|-------|
| `PROFILE_REQUIRED` | `/onboarding/profile` |
| `SPORTS_REQUIRED` | `/onboarding/sports` |
| `GOALS_REQUIRED` | `/onboarding/goals` |
| `COMPLETE` | `/app/home` |

`AthleteOnboardingProvider` loads profile/sports/goals when authenticated. Profile 404 (`ATHLETE_PROFILE_NOT_FOUND`) is treated as `profile: null`, not an error.

Guard order: auth → bootstrap V1 → onboarding → app.

## Home (W2)

`GET /api/v1/training/client/today` drives a desktop dashboard grid:

- Greeting (profile first name + authoritative `date`)
- Primary workout, readiness, recommendation, recovery, training load, adaptation, recent performance
- Quick actions for explicit derived-state generation when `actions.*.allowed`

Derived-state mutations (mobile-aligned paths):

- `POST /api/v1/training/athlete-state/daily/{date}`
- `POST /api/v1/training/readiness/daily/{date}`
- `POST /api/v1/training/recommendations/daily/{date}`

Training execution, recovery check-in forms, and adaptation review flows remain deferred to W4+.

## Training (W3)

Desktop training planner and calendar under `/app/training`:

- **Landing** — `GET /api/v1/training/client/training-overview?date=`
- **Plans** — CRUD-ish plan metadata, day/exercise builder, move up/down reorder via `PUT .../order`
- **Exercise chooser** — paginated `GET /api/v1/training/exercise-definitions` (active only)
- **Schedule** — activate/pause/resume/complete/generate under `/plans/{planId}/schedule`
- **Calendar** — month view + side panel via `GET /api/v1/training/calendar`
- **Occurrences** — read-only detail, reschedule/delete when `SCHEDULED` and untouched
- **Environments** — thin picker list via `GET /api/v1/training/environments?activeOnly=true`

Prescription edits invalidate plan/day/exercise queries only — occurrence snapshots are not assumed to change.

W4+ deferred: workout execution/set logging (mobile owns), environment CRUD UI, adaptation review.

## Profile (W2)

- `/app/profile` — account, athlete summary, sports/goals lists, logout
- `/app/profile/edit`, `/app/profile/sports`, `/app/profile/goals` — management

## Onboarding (deferred W1 note — superseded)

Athlete onboarding was not implemented in W1. W2 adds the full flow above.

## Build notes

- Production build runs `tsc -b && vite build`.
- **Source maps are disabled** in production (`vite.config.ts` → `build.sourcemap: false`) to reduce leaked source exposure. Enable locally only when debugging production bundles.

## Security / deployment

- Never log passwords, tokens, cookies, CSRF values, or athlete notes (see `core/logging/logger.ts`).
- Deploy over HTTPS.
- Set explicit `VITE_UAP_ENV` and API URL for staging/production builds.
- Ensure backend CORS allows credentials from the deployed web origin.
- Keep CSRF double-submit enabled for mutating API calls.

## Tests

Vitest suites cover env fail-closed rules, date-only handling, error mapping, CSRF, refresh single-flight, session cache clearing (including athlete query keys), bootstrap contract checks, onboarding state/routes, athlete schemas, profile form dirty-safe hydrate, home dashboard schema/labels, derived-state API paths, home page rendering, login a11y basics, logger redaction, and W3 training schemas/prescription formatting/calendar range/reorder helpers/training error codes/invalidation/exercise chooser/prescription form/landing smoke.

```bash
pnpm --filter uap_web test
```
