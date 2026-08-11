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
| `EXPO_PUBLIC_UAP_API_BASE_URL` | Yes for staging/production | HTTPS recommended; localhost forbidden outside development |

## Native cookie/CSRF acceptance checklist

Use Development Build + live backend:

1. Login
2. Cookies issued
3. `/identity/me` authenticated
4. CSRF-protected mutation succeeds
5. Access expiry / refresh succeeds
6. Authenticated request after refresh succeeds
7. Terminate app
8. Restart → session restores
9. Logout → protected request fails
10. Login again works

Document platform, API URL, CookieManager behavior, XSRF behavior, and persistence result for each release candidate.

## Feature freeze (post-M10)

Allowed: P0/P1 bugs, integration defects, production config, accessibility/security blockers.

Not allowed: new features, charts-for-polish, timezone/unit backend expansion, plan editing, notifications, offline sync, AI, new readiness/recommendation versions.
