# Universal Athlete — Mobile (M10) / v1 release candidate

React Native / Expo client for the Universal Athlete Platform. **Not Flutter.**

**M10 is hardening and feature-freeze** for the v1 release candidate: stability, config fail-closed behavior, auth/session correctness, and release readiness — not new product features.

Operational procedures (smoke checklists, common errors, EAS notes) live in [`docs/MOBILE_V1_RUNBOOK.md`](docs/MOBILE_V1_RUNBOOK.md).

## Stack

| Layer | Choice |
| --- | --- |
| Expo SDK | ~57 |
| React Native | 0.86 |
| React | 19 |
| TypeScript | strict (`~6`) |
| Navigation | Expo Router |
| Server state | TanStack Query |
| HTTP | Axios (`withCredentials`) |
| Validation | Zod |
| Forms | React Hook Form (+ Zod resolvers) |
| Auth | Cookie session + CSRF double-submit |
| Refresh | Single-flight `POST /api/v1/identity/refresh` |
| Native runtime | Expo Dev Build (`expo-dev-client`) — **Expo Go is not supported** for real auth |

Pinned package versions are in `package.json` (Expo `~57.0.12`, RN `0.86.2`, React `19.2.3`).

## Milestone coverage (M1R–M9)

| Milestone | Scope |
| --- | --- |
| **M1R** | App shell, env config, Axios client, cookies/CSRF, refresh single-flight, Expo Dev Build baseline |
| **M2** | Auth (register/login/verify), onboarding (profile → sports → goals), bootstrap V1 gate, logout cache clear |
| **M3** | Home / Today dashboard, explicit derived-state / readiness / guidance generation |
| **M4** | Training browse stack (overview, calendar, plan/day/occurrence, launch prep) |
| **M5** | Live workout execution (start/complete/skip, sets, session effort, training load) |
| **M6** | Recovery tab (check-in, history, analytics text, readiness/guidance detail) |
| **M7** | Adaptation proposals + exercise substitution / revert |
| **M8** | Performance tab (PRs, exercise history, training load history — textual, no charts) |
| **M9** | Training environments (Profile CRUD) + occurrence environment selection at launch |
| **M10** | RC hardening / feature-freeze (this release) |

Tabs: **Home**, **Training**, **Recovery**, **Performance**, **Profile**. Performance is a full feature surface (M8), not a placeholder.

## Compatibility matrix

| Contract | Version |
| --- | --- |
| Client contract | `V1` (`clientContractVersion` from bootstrap) |
| Readiness | `READINESS_V1` |
| Recommendation | `TRAINING_RECOMMENDATION_V1` |
| Backend Flyway | `V29` (minimum) |

Runtime negotiation is the existing bootstrap `clientContractVersion === 'V1'` check. Unknown versions route to `/incompatible`. No additional semver negotiation in M10.

Mobile minimum backend: **v1 release candidate with Flyway ≥ V29 and client contract V1**.

## Environment configuration

Set via `.env`, shell exports, EAS env/secrets, or CI. Rules are enforced in `src/config/env.ts`.

| Variable | Required | Description |
| --- | --- | --- |
| `EXPO_PUBLIC_UAP_ENV` | **Yes in release builds** | `development` \| `staging` \| `production` |
| `EXPO_PUBLIC_UAP_API_BASE_URL` | Yes for staging/production | API origin (trailing slash normalized away) |

Rules:

- **Release builds fail closed** if `EXPO_PUBLIC_UAP_ENV` is missing (no silent default).
- **Local Metro / `__DEV__` only**: missing env defaults to `development`.
- **development**: API URL defaults to `http://127.0.0.1:8080` when omitted.
- **staging / production**: API URL is required; no production fallback.
- **Localhost forbidden outside development** — `localhost`, `127.0.0.1`, and `10.0.2.2` are rejected when env is not `development`.

### Local API URL examples

| Target | Typical `EXPO_PUBLIC_UAP_API_BASE_URL` |
| --- | --- |
| iOS Simulator | `http://127.0.0.1:8080` |
| Android Emulator | `http://10.0.2.2:8080` |
| Physical device | `http://<your-lan-ip>:8080` |

Example:

```bash
EXPO_PUBLIC_UAP_ENV=development \
EXPO_PUBLIC_UAP_API_BASE_URL=http://127.0.0.1:8080 \
pnpm start
```

### Cleartext HTTP (development only)

`app.config.ts` enables insecure local HTTP **only** when `EXPO_PUBLIC_UAP_ENV=development`:

- **iOS**: `NSAppTransportSecurity` exceptions for `localhost` / `127.0.0.1`
- **Android**: `usesCleartextTraffic: true`

Staging/production builds **must** use HTTPS endpoints. `loadAppConfig` rejects non-HTTPS API URLs when `EXPO_PUBLIC_UAP_ENV` is `staging` or `production`.

## Running

### From repository root

```bash
pnpm mobile:start       # Expo dev server
pnpm mobile:test        # Jest
pnpm mobile:typecheck   # tsc --noEmit
```

### From `apps/mobile`

```bash
pnpm start       # expo start
pnpm test        # jest --watchman=false
pnpm typecheck   # tsc --noEmit
pnpm lint        # eslint . (Expo flat config; non-interactive)
pnpm export      # expo export (static web export smoke)
npx expo-doctor  # Expo health check
```

From repository root: `pnpm mobile:lint`, `pnpm mobile:test`, `pnpm mobile:typecheck`, `pnpm mobile:export`.

### EAS build profiles (`eas.json`)

| Profile | Purpose | `EXPO_PUBLIC_UAP_ENV` in profile |
| --- | --- | --- |
| `development` | Dev client, internal | `development` |
| `preview` | Internal staging-like | `staging` |
| `production` | Production | `production` |

Provide `EXPO_PUBLIC_UAP_API_BASE_URL` via **EAS environment / secrets** (or other build-time env). Do **not** put API URLs or secrets in `eas.json`.

Do not submit to stores from M10 hardening alone — see the runbook.

## Auth: cookies, CSRF, refresh

Authentication uses Spring session cookies in the **native cookie jar** (`@react-native-cookies/cookies`).

- **Expo Go is not supported** for real auth — use an **Expo Development Build**.
- Web / static export uses an in-memory cookie stub so `pnpm export` can succeed; native cookie behavior is not available on web.

Behavior summary:

1. Login/register populate the native jar; Axios uses `withCredentials: true`.
2. Mutating requests attach `X-XSRF-TOKEN` from the `XSRF-TOKEN` cookie (CSRF exempt: register, verify-email, login).
3. On **401**, a **single-flight** `POST /api/v1/identity/refresh` runs; the original request retries at most once; refresh itself is never recursively retried.
4. Logout / logout-all / refresh failure clear cookies (best effort) and TanStack Query cache so Athlete A data cannot leak to Athlete B.

Full smoke checklist and troubleshooting: [`docs/MOBILE_V1_RUNBOOK.md`](docs/MOBILE_V1_RUNBOOK.md).

Optional HTTP jar smoke (backend on `:8080`):

```bash
pnpm exec node scripts/cookie-auth-smoke.mjs http://127.0.0.1:8080
```

## Feature-freeze policy (post-M10)

**Allowed**

- P0/P1 bug fixes and integration defects
- Production / staging config and release blockers
- Accessibility and security blockers

**Not allowed**

- New product features
- Charts / visualization polish
- Timezone or unit-preference backend expansion
- Plan/day environment preference editing
- Push notifications
- Offline sync
- AI features
- New readiness / recommendation contract versions

## Deferred (P2/P3) — out of v1 RC scope

| Gap | Notes |
| --- | --- |
| Charts | Performance/recovery remain textual summaries and lists |
| Timezone / units | Backend has no athlete timezone or unit preferences; profile is metric-only |
| Plan env editor | Plan/day default/override env IDs are read-only; occurrence env set at launch only |
| Push notifications | Not in v1 |
| Offline sync | Not in v1 (set reorder / occurrence cancel also deferred) |
| Store assets | Icons, screenshots, listing copy — separate from M10/M4 hardening |
| Cookie manager package | `@react-native-cookies/cookies` is metadata-deprecated; migrate post-RC if maintenance risk materializes |

## Architecture (concise)

```
src/app/                 Expo Router screens & layouts
src/config/              Environment + QueryClient
src/providers/           App, auth session, onboarding, bootstrap
src/theme/               Design tokens + ThemeProvider
src/core/api/            Axios client, CSRF, cookies, errors
src/core/components/     Shared UI primitives
src/features/auth/       Identity API, schemas, forms
src/features/onboarding/ Onboarding state resolver & routes
src/features/profile/    Athlete profile / sports / goals
src/features/home/       Today dashboard
src/features/training/   Browse + execution (`execution/`)
src/features/recovery/   Recovery overview, check-in, history
src/features/adaptation/ Proposals + substitution
src/features/performance/ PRs, exercise history, training load
src/features/environments/ Training environments CRUD
__tests__/               Jest (incl. `__tests__/rc/` RC suite)
```

Path aliases map `@/src/app/...` into `src/` (see `tsconfig`).

### Core flows

1. **Bootstrap**: restore session → onboarding state → when complete, `GET /api/v1/training/client/bootstrap` → require contract `V1` → tabs or `/incompatible`.
2. **Home**: `GET /api/v1/training/client/today`; generation of athlete state / readiness / guidance is **explicit only** (no auto-chain on mount).
3. **Training**: overview → calendar / plan / day / occurrence → launch → execute (M5) → adaptation / substitute (M7) → environment picker (M9).
4. **Recovery**: overview → check-in / history / analytics → readiness & guidance detail.
5. **Performance**: recent PRs, all records, exercise history, load history (`OCCURRENCE` / `DAILY` / `WEEKLY`). Null session RPE must not render as zero.
6. **Profile**: account, athlete summary, sports/goals, training environments, client contract + environment diagnostics.

### Milestone technical notes (kept short)

**M5 execution** — Base path `/api/v1/training/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}`. PATCH set actuals auto-starts `NOT_STARTED → IN_PROGRESS`. Launch Start/Continue navigates to execute without calling start; the athlete starts explicitly on the execute screen. Invalidation is scoped (no global `invalidateQueries()`).

**M6 recovery** — Nested stack under `/(tabs)/recovery`. Check-in uses RHF; PATCH sends bare JSON values/null with `expectedVersion` (same patch style as environments). Optional post-save prompt to generate/regenerate daily athlete state — no auto-chain to readiness/guidance.

**M7 adaptation** — Manual or guidance-linked proposal generation; apply is never automatic. Home AdaptationCard, launch prep, training overview, and execute substitute CTAs deep-link into proposal / substitute screens.

**M8 performance** — Textual trends and lists only; no chart library. Occurrence terminal + session-effort mutations invalidate `performanceKeys.all`; set-level updates do not.

**M9 environments** — Profile stack for environment CRUD + default; occurrence env via launch prep when `canChangeEnvironment.allowed`. BODYWEIGHT is selectable equipment but never auto-inserted. Plan/day env preference editor remains deferred.

## Testing

```bash
# from apps/mobile
pnpm test
pnpm typecheck
pnpm lint
```

Coverage includes env fail-closed rules, CSRF/refresh, auth/onboarding, today/home, training browse + execution, recovery, adaptation, performance, environments, logout cache clearing, and the focused RC suite under `__tests__/rc/`.

## Notes

- Passwords are never persisted. Secure storage is restricted to non-sensitive keys.
- There is **no resend verification API** — in development, read the token from backend logs / notifier output and paste it on Verify email.
- Password policy: 12–128 characters, upper, lower, digit, special.
- Static export uses the web cookie stub; use native Dev Builds for end-to-end auth validation.
