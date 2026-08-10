# Universal Athlete — Mobile (M2)

React Native mobile client for the Universal Athlete Platform, built with **Expo SDK ~57**, **React Native 0.86.2**, **React 19.2.3**, and **TypeScript (strict)**.

M2 adds production auth forms, athlete onboarding (profile → sports → goals), bootstrap gating, and a full Profile tab. M1R covered cookie-backed auth, CSRF-aware API access, session refresh, client bootstrap validation, and the diagnostic Home screen.

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
src/features/training/   Bootstrap/today API + Zod schemas
src/features/home/       Diagnostic Home + TanStack Query hook
__tests__/               Jest unit/integration tests
```

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

Tests live under `__tests__/` and cover environment loading, date-only parsing, CSRF rules, error mapping, refresh single-flight, auth schemas/error copy/login form validation, onboarding resolver + routing + resume, profile/sports/goals schemas, logout cache clearing, logging redaction, and the Home diagnostic screen.

```bash
pnpm test
```

## Notes

- Passwords are never persisted. Secure storage is restricted to non-sensitive keys (optional last email is disabled by default).
- Placeholder tabs (Training, Recovery, Performance) remain intentional beyond M2 scope.
- Static export uses the web cookie stub; use native dev builds for end-to-end auth validation.
