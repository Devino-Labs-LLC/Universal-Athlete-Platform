# Universal Athlete Mobile — V1 Runbook

React Native (Expo) client for Universal Athlete Platform v1.

## Compatibility matrix

| Contract | Version |
| --- | --- |
| Client contract | `V1` |
| Readiness | `READINESS_V1` |
| Recommendation | `TRAINING_RECOMMENDATION_V1` |
| Backend Flyway | `V29` |
| Mobile minimum backend | Backend v1 release candidate with Flyway ≥ V29 and client contract V1 |

Runtime negotiation remains the existing bootstrap `clientContractVersion` check. No additional semver negotiation in M10.

## Development startup

1. Start Spring Boot backend (`backend/uap-server`) on the expected port (default `8080`).
2. From repo root:

```bash
pnpm mobile:start
```

Use an **Expo Development Build** (not Expo Go) for native cookie persistence tests.

### API URL examples

| Target | Typical `EXPO_PUBLIC_UAP_API_BASE_URL` |
| --- | --- |
| iOS Simulator | `http://127.0.0.1:8080` |
| Android Emulator | `http://10.0.2.2:8080` |
| Physical device | `http://<your-lan-ip>:8080` |

Set:

```bash
export EXPO_PUBLIC_UAP_ENV=development
export EXPO_PUBLIC_UAP_API_BASE_URL=http://127.0.0.1:8080
```

Staging/production builds **must** set both `EXPO_PUBLIC_UAP_ENV` and a non-localhost `EXPO_PUBLIC_UAP_API_BASE_URL`. Release builds fail closed if `EXPO_PUBLIC_UAP_ENV` is missing.

## Authentication

- Cookie session: `uap_at` / `uap_rt` (+ `XSRF-TOKEN`)
- CSRF header: `X-XSRF-TOKEN`
- Refresh is single-flight; the refresh request is never recursively retried
- Original failed request retries at most once after refresh
- Logout / logout-all / session expiry clear cookies (best effort) and TanStack Query cache

## Core user flows

### New athlete

Register → verify email → login → profile → sports → goals → Home.

### Daily Home

Today dashboard → check-in → optional explicit athlete state / readiness / guidance generation.

### Workout

Training overview/calendar → occurrence → launch → choose environment (if allowed) → execute → log sets → complete → session effort.

### Recovery

Recovery tab → create/edit check-in → history → explicit derived-state/readiness/guidance actions.

### Adaptation

Generate proposal → review accept/override/reject → apply (never auto-apply) → optional regenerate when stale.

### Performance

Recent PRs, records, exercise history, load history (OCCURRENCE/DAILY/WEEKLY). Null session RPE must not render as zero.

### Environments

Profile → Training Environments → create/edit/default/archive → occurrence Choose Environment.

## Common development errors

| Symptom | Likely cause |
| --- | --- |
| Network errors on launch | Backend not listening / wrong API URL |
| CSRF 403 on mutations | Cookie/XSRF mismatch; restart app after backend restart |
| Endless login loop | Expired cookies + refresh failure; clear app data |
| Expo Go cookie issues | Use Development Build |
| Android cannot reach host machine | Use `10.0.2.2` instead of `127.0.0.1` |
| Staging/prod build crashes on boot | Missing `EXPO_PUBLIC_UAP_ENV` / API URL |

## Test commands

From repo root:

```bash
pnpm mobile:test
# or non-watch:
pnpm --filter uap_mobile exec jest --watchman=false --watchAll=false
pnpm mobile:typecheck
```

From `apps/mobile`:

```bash
pnpm test
pnpm typecheck
pnpm lint
pnpm export
npx expo-doctor
```

## Build commands

```bash
# Export JS bundles (CI smoke)
pnpm --filter uap_mobile exec expo export

# EAS profiles (see eas.json): development | preview | production
# Provide EXPO_PUBLIC_UAP_API_BASE_URL via EAS secrets / build env for preview+production.
eas build --profile development --platform ios
eas build --profile preview --platform android
eas build --profile production --platform all
```

Do **not** submit to stores from M10 hardening alone.

## Environment variables

| Variable | Required | Notes |
| --- | --- | --- |
| `EXPO_PUBLIC_UAP_ENV` | Yes in release | `development` \| `staging` \| `production` |
| `EXPO_PUBLIC_UAP_API_BASE_URL` | Yes for staging/production | **HTTPS required** outside development; localhost / `10.0.2.2` forbidden outside development |

### EAS secrets (do not put in `eas.json`)

| Secret / env | Profiles | Notes |
| --- | --- | --- |
| `EXPO_PUBLIC_UAP_API_BASE_URL` | preview, production | Staging/production HTTPS origin |
| Apple / Google credentials | production (submit) | Managed via EAS credentials, not repo |

`eas.json` only sets `EXPO_PUBLIC_UAP_ENV` per profile. Missing API URL fails closed at runtime for staging/production.

## Native cookie/CSRF acceptance checklist

Use Development Build + live backend (**not** Expo Go, **not** web export):

1. Login
2. Cookies issued (`uap_at` / `uap_rt` / `XSRF-TOKEN` present in native jar — do not log values)
3. `/identity/me` authenticated
4. CSRF-protected mutation succeeds (Cookie + `X-XSRF-TOKEN`)
5. Access expiry / refresh succeeds (single-flight; one retry)
6. Authenticated request after refresh succeeds
7. Terminate app
8. Restart → session restores from cookies
9. Logout → Query cache cleared; protected request fails
10. Cross-account: Athlete A logout → Athlete B login shows no Athlete A PII
11. Login again works

Document platform, API URL, CookieManager behavior, XSRF behavior, and persistence result for each release candidate.

### M4 positive-path status (2026-08-12)

Host/API contract against local Spring (`127.0.0.1:8080`) with RA1 (`ra1.user1@devinolabs.test`):

| Step | Result |
| --- | --- |
| Login | PASS (HTTP 200) |
| Cookie presence (`uap_at` / `uap_rt` / `XSRF-TOKEN`) | PASS (yes/yes/yes; values not logged) |
| Authenticated `/api/v1/identity/me` | PASS (HTTP 200, ACTIVE) |
| Athlete-domain GET (`/athletes/me`, `/training/client/today`, `/training/environments`) | PASS |
| CSRF mutation (`POST /training/environments`, `POST …/default`, `DELETE`, `POST /identity/logout`) | PASS; `X-XSRF-TOKEN` required (missing → `CSRF_INVALID`) |
| Refresh with access cookie removed + valid refresh | PASS (`POST /identity/refresh` 204 → `/me` 200) |
| Logout then `/me` | PASS (401) |

Native Dev Client UI / cold-start / Home routing: **NOT OBSERVED** in agent session (CoreSimulatorService unavailable). Operator must complete Login → Home, kill/relaunch restore, and logout relaunch on the running iOS Dev Client.

DEV diagnostics (presence-only): login cookie matrix, CSRF header attachment yes/no, teardown cookie presence.

## M4 release hardening checklist

### Config / builds

- [ ] `com.devinolabs.uap` bundle/application id confirmed in prebuild
- [ ] Development ATS/cleartext only for local HTTP
- [ ] Staging/production HTTPS fail-closed (`loadAppConfig`)
- [ ] EAS development / preview / production profiles
- [ ] `EXPO_PUBLIC_UAP_API_BASE_URL` set in EAS env for preview+production
- [ ] Android diagnostic prebuild (or EAS) succeeds
- [ ] iOS diagnostic prebuild (or EAS) succeeds; signing credentials available for device/store

### Native acceptance (Dev Client + live API)

- [ ] Cookie login / me / bootstrap V1 / Today
- [ ] CSRF mutation (check-in or environment)
- [ ] Kill/relaunch session restore
- [ ] Cross-account isolation after logout
- [ ] Training execute path (start → set log → complete → effort)
- [ ] Recovery check-in create/edit
- [ ] Performance empty/null semantics

### Gates

- [ ] typecheck / lint (0 errors) / Jest stable ×3 / export

## Feature freeze (post-M10 / M4)

Allowed: P0/P1 bugs, integration defects, production config, accessibility/security blockers.

Not allowed: new features, charts-for-polish, timezone/unit backend expansion, plan editing, notifications, offline sync, AI, new readiness/recommendation versions.

### Known M4 deferred items (P2)

- `@react-native-cookies/cookies` npm deprecation metadata → migrate later to a maintained manager if needed; not a current security blocker
- Optional OS theme toggle
- Calendar / launch visual polish leftovers from M3
- Full App Store / Play listing assets and store submission
