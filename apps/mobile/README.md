# Universal Athlete — Mobile (M5)

React Native mobile client for the Universal Athlete Platform, built with **Expo SDK ~57**, **React Native 0.86.2**, **React 19.2.3**, and **TypeScript (strict)**.

M5 adds **live workout execution and set logging**: start/complete/skip occurrence, per-exercise set logging (PATCH actuals auto-start sets), session effort, and training load summary. M4 added the Training tab browse stack and launch prep. M3 added the production Home / Today dashboard.

## Stack versions

| Package | Version |
| --- | --- |
| Expo SDK | ~57.0.12 |
| React Native | 0.86.2 |
| React | 19.2.3 |
| TypeScript | ~6.0.3 |
| TanStack Query | ^5 |
| Axios | ^1.19 |
| Zod | ^4 |
| Jest + jest-expo | ^29 / ^57 |

## Monorepo usage

From the repository root:

```bash
pnpm --filter uap_mobile install
pnpm --filter uap_mobile start
```

Or from this directory:

```bash
pnpm install
pnpm start
```

## Environment variables

Set these via `.env`, shell exports, or your CI/dev build profile:

| Variable | Required | Description |
| --- | --- | --- |
| `EXPO_PUBLIC_UAP_ENV` | No (defaults to `development`) | `development`, `staging`, or `production` |
| `EXPO_PUBLIC_UAP_API_BASE_URL` | Yes for staging/production | API origin, e.g. `http://127.0.0.1:8080` |

Rules (`src/config/env.ts`):

- **development**: defaults to `http://127.0.0.1:8080` when the URL is omitted
- **staging / production**: throws if the URL is missing (no production fallback)
- Trailing slashes are normalized away

Example:

```bash
EXPO_PUBLIC_UAP_ENV=development EXPO_PUBLIC_UAP_API_BASE_URL=http://127.0.0.1:8080 pnpm start
```

## Local API connectivity

| Target | URL |
| --- | --- |
| iOS Simulator | `http://127.0.0.1:8080` |
| Android Emulator | `http://10.0.2.2:8080` |
| Physical device | `http://<your-lan-ip>:8080` |

### Cleartext HTTP (development only)

`app.config.ts` enables insecure local HTTP **only** when `EXPO_PUBLIC_UAP_ENV=development`:

- **iOS**: `NSAppTransportSecurity` exceptions for `localhost` and `127.0.0.1`
- **Android**: `usesCleartextTraffic: true`

Do not rely on cleartext outside development. Staging/production builds should use HTTPS endpoints via `EXPO_PUBLIC_UAP_API_BASE_URL`.

## Expo Dev Build required (cookies)

Authentication uses Spring session cookies stored in the **native cookie jar** via `@react-native-cookies/cookies`.

- **Expo Go is not supported** for real auth flows — the native cookie module is unavailable there.
- Use an **Expo Development Build** (`expo-dev-client`) on iOS/Android simulators or devices.
- **Web / static export** uses an in-memory cookie stub so `expo export` can succeed; native cookie behavior is not available on web.

Cookie strategy:

1. Login/register responses populate the native jar (Set-Cookie handling is platform-dependent; the native store is authoritative).
2. Axios sends `withCredentials: true`; mutating requests attach `X-XSRF-TOKEN` from the `XSRF-TOKEN` cookie.
3. Logout calls the server, then `CookieManager.clearAll(true)`.
4. Refresh failure clears cookies and marks the session **EXPIRED**.

## Architecture

Expo Router lives in `src/app/` (required when using the `src/` layout). Shared application code uses path aliases so imports like `@/src/app/config/env` resolve to `src/config/env.ts`:

```
src/app/                 Expo Router screens & layouts
src/config/              Environment + QueryClient
src/providers/           App, auth session, bootstrap
src/theme/               Slate/teal tokens + ThemeProvider
src/core/api/            Axios client, CSRF, cookies, errors
src/core/components/     Screen, buttons, loading/error/empty
src/features/auth/       Identity API, Zod schemas, form fields, error copy
src/features/onboarding/ Onboarding state resolver, routes, provider wiring
src/features/profile/    Athlete profile/sports/goals API, schemas, hooks
src/features/training/   Bootstrap/today API, browse APIs, schemas, screens
src/features/home/       Today dashboard (cards, hooks, generation actions)
__tests__/               Jest unit/integration tests
```

### Training browse stack (M4)

The Training tab (`/(tabs)/training`) is a nested Stack:

1. **Overview (default)** — **GET** `/api/v1/training/client/training-overview?date=` — next workout, upcoming list, active plans, recent completed, weekly load, outstanding adaptations. Pull-to-refresh on overview only.
2. **Calendar** — **GET** `/api/v1/training/calendar?scheduledFrom=&scheduledTo=` — visible week strip (7 days), prev/next week, day-filtered occurrence list.
3. **Plan detail** — **GET** `/api/v1/training/plans/{planId}` + `/days` — list workout days.
4. **Day detail** — **GET** `/api/v1/training/plans/{planId}/days/{dayId}/exercises` — ordered prescriptions.
5. **Occurrence detail** — **GET** `/api/v1/training/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}` — status, environment, execution summary. CTA routes to launch prep.
6. **Launch prep** — **GET** `/api/v1/training/client/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}/launch-context` — eligibility, environment, feasibility, recommendation, adaptation, prescriptions. Start/Continue routes to execute; Review Adaptation routes to an M7 placeholder.
7. **Execute (M5)** — live workout logging at `/occurrences/{occurrenceId}/execute` — see [Workout execution (M5)](#workout-execution-m5).

Route layout under `src/app/(tabs)/training/`:

```
_layout.tsx                 Stack navigator
index.tsx                   TrainingOverviewScreen
calendar.tsx                TrainingCalendarScreen
plans/[planId]/index.tsx    TrainingPlanDetailScreen
plans/[planId]/days/[dayId]/index.tsx
plans/[planId]/days/[dayId]/occurrences/[occurrenceId]/index.tsx
plans/[planId]/days/[dayId]/occurrences/[occurrenceId]/launch.tsx
plans/[planId]/days/[dayId]/occurrences/[occurrenceId]/execute.tsx   (M5 live execution)
plans/[planId]/days/[dayId]/occurrences/[occurrenceId]/adaptation-review.tsx (M7 placeholder)
```

Feature code lives in `src/features/training/` (`api/`, `hooks/`, `models/browseSchemas.ts`, `components/`, `screens/`, `utils/`). M5 execution code lives in `src/features/training/execution/`.

### Workout execution (M5)

Base path: `/api/v1/training/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}`

| Action | Method | Path |
| --- | --- | --- |
| Start workout | POST | `.../start` |
| Complete workout | POST | `.../complete` |
| Skip workout | POST | `.../skip` |
| List sets | GET | `.../exercises/{executionId}/sets` |
| Add set | POST | `.../exercises/{executionId}/sets` (body optional `{}`) |
| Update set actuals | PATCH | `.../exercises/{executionId}/sets/{setId}` |
| Complete set | POST | `.../exercises/{executionId}/sets/{setId}/complete` |
| Skip set | POST | `.../exercises/{executionId}/sets/{setId}/skip` |
| Delete set | DELETE | `.../exercises/{executionId}/sets/{setId}` (NOT_STARTED only, ≥1 set) |
| Session effort | POST/PATCH/GET | `.../session-effort` |
| Training load | GET | `.../training-load` |

**PATCH auto-start:** Updating set actuals via PATCH promotes `NOT_STARTED → IN_PROGRESS` and auto-promotes parent occurrence/execution. The mobile client does **not** call `POST .../sets/{setId}/start` separately.

**Invalidation:** Scoped helpers in `execution/models/invalidation.ts` invalidate occurrence, launch, calendar (prefix), overview (prefix), today, sets, load, and effort as appropriate. No global `invalidateQueries()`.

**Deferred:** Set reorder, exercise substitute (M7), occurrence cancel, offline sync.

**UX:** Start workout is explicit on the execute screen (SCHEDULED state). Launch prep Start/Continue navigates to execute without calling start — the athlete starts from execute when ready.

### Home / Today dashboard (M3)

After onboarding completes and bootstrap V1 loads, the Home tab (`/(tabs)/index`) renders `HomeScreen`:

1. **GET** `/api/v1/training/client/today` — full today DTO (recovery, readiness, recommendation, training, load, adaptation, PRs, action flags)
2. Card hierarchy: header → quick actions / primary workout → readiness → guidance → recovery → load → adaptation → recent performance
3. **Explicit generation only** (no auto-chaining on mount):
   - **POST** `/api/v1/training/athlete-state/daily/{date}` — body `{ baselineWindowDays: 7 }`
   - **POST** `/api/v1/training/readiness/daily/{date}`
   - **POST** `/api/v1/training/recommendations/daily/{date}`
4. Each successful generation invalidates the today query; pull-to-refresh refetches read-only data
5. Workout / adaptation CTAs route into the Training stack (overview → launch prep); recovery check-in routes to Recovery

### Bootstrap + onboarding flow (M2)

1. `src/app/index.tsx` → `/bootstrap`
2. Restore session (`GET /api/v1/identity/me`)
3. Load athlete profile, sports, and goals; derive onboarding state client-side (backend has no onboarding flag):
   - `PROFILE_REQUIRED` — `GET /api/v1/athletes/me` → 404
   - `SPORTS_REQUIRED` — profile OK, zero sports
   - `GOALS_REQUIRED` — ≥1 sport, zero goals
   - `COMPLETE` — profile + ≥1 sport + ≥1 goal
4. Incomplete onboarding → `/(onboarding)/profile|sports|goals`
5. When onboarding is **COMPLETE**, load bootstrap (`GET /api/v1/training/client/bootstrap`)
6. Require `clientContractVersion === 'V1'`
7. Route to `/(auth)/login`, onboarding step, `/(tabs)`, or `/incompatible`

Training bootstrap is **not** fetched during onboarding. This avoids failures when the athlete profile does not exist yet.

### Auth + verify email (M2)

- Register/login/verify screens use `react-hook-form` + Zod (`registerRequestSchema` enforces 12–128 char password policy matching backend).
- Identity errors map to stable user copy (`src/features/auth/errorMessages.ts`).
- **No resend verification API.** In development, read the token from backend logs (`InMemoryVerificationNotifier`) or your configured notifier output, then paste it on **Verify email**.
- Password policy: 12–128 characters, upper, lower, digit, special.

### Athlete profile contract notes

- Profile create/update uses metric fields only (`heightCm`, `weightKg`). There are **no timezone or unit preference** fields in M2.
- Sports and goals use backend enums exactly (`SportType`, `ParticipationLevel`, `SeasonStatus`, `GoalType`, `GoalPriority`).

### Logout security (M2)

Logout and logout-all call the backend (best effort), clear cookies, and `queryClient.clear()` so Athlete A cached data cannot appear for Athlete B after account switch.

### Bootstrap flow (M1R baseline)

### API client highlights

- 30s timeout, JSON headers, `withCredentials: true`
- CSRF on POST/PUT/PATCH/DELETE (exempt: register, verify-email, login)
- Single-flight refresh on 401 via `POST /api/v1/identity/refresh`, one retry per request

## Scripts

```bash
pnpm typecheck   # tsc --noEmit
pnpm test        # jest (jest-expo preset)
pnpm lint        # expo lint
pnpm export      # static web export
pnpm start       # Expo dev server
```

## Auth / cookie smoke procedures

### A. HTTP jar smoke (backend contract)

With a running backend on port `8080` (MySQL required):

```bash
pnpm exec node scripts/cookie-auth-smoke.mjs http://127.0.0.1:8080
```

Validates register → login → cookies (`uap_at` / `uap_rt`) → `/me` → CSRF refresh → logout → bootstrap V1 → today.

Note: live register may stop at `EMAIL_NOT_VERIFIED` unless the verification token is available from your notifier/dev tooling.

### B. Native Expo Dev Build smoke (required for CookieManager)

Expo Go is **not** supported. Use a development build:

```bash
EXPO_PUBLIC_UAP_ENV=development \
EXPO_PUBLIC_UAP_API_BASE_URL=http://127.0.0.1:8080 \
npx expo run:ios
# Android emulator:
EXPO_PUBLIC_UAP_API_BASE_URL=http://10.0.2.2:8080 npx expo run:android
```

Then (M2 smoke):

1. Register with a policy-compliant password → verify email using dev token from backend logs → login
2. Complete onboarding: profile → sport → goal
3. Confirm bootstrap V1 loads only after onboarding completes, then Home/today diagnostics work
4. Profile tab shows account, athlete summary, sports/goals, client contract + environment
5. Logout / logout-all clears session and returns to login; login as a different user shows no stale cache
6. Edit profile or add another sport/goal from Profile without forced re-onboarding (unless you delete the last sport or goal)

Cookie mechanism: `@react-native-cookies/cookies` + Axios Cookie/`X-XSRF-TOKEN` interceptors.  
CSRF: double-submit `XSRF-TOKEN` cookie → `X-XSRF-TOKEN` header.  
Refresh: single-flight `POST /api/v1/identity/refresh`.

## Testing

Tests live under `__tests__/` and cover environment loading, date-only parsing, CSRF rules, error mapping, refresh single-flight, auth schemas/error copy/login form validation, onboarding resolver + routing + resume, profile/sports/goals schemas, logout cache clearing, logging redaction, today dashboard schemas, Home cards/greeting/mutations, pull-to-refresh, training browse schemas, calendar range helpers, overview screen states, occurrence card CTAs, prescription formatting, and M5 execution schemas, set formatting, error mapping, invalidation helpers, SetRow/SetEditor, and WorkoutExecutionScreen states.

```bash
pnpm test
```

## Notes

- Passwords are never persisted. Secure storage is restricted to non-sensitive keys (optional last email is disabled by default).
- Placeholder tabs (Recovery, Performance) remain intentional beyond M5 scope. Adaptation apply (M7) is a stubbed route only. Workout execution (M5) is implemented on the execute screen.
- Static export uses the web cookie stub; use native dev builds for end-to-end auth validation.
