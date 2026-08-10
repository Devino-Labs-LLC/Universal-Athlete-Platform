# Training Client Handoff v1 (Phase 7V)

Recommended mobile/web integration against the Training API.  
Companion surface map: [`TRAINING_API_V1.md`](./TRAINING_API_V1.md).

---

## Table of contents

1. [Contract basics](#1-contract-basics)
2. [Startup → Home](#2-startup--home)
3. [Recovery flow](#3-recovery-flow)
4. [Workout flow](#4-workout-flow)
5. [No-hidden-write semantics](#5-no-hidden-write-semantics)
6. [Explicit generation chain](#6-explicit-generation-chain)
7. [IDs to keep](#7-ids-to-keep)
8. [Mutation ordering](#8-mutation-ordering)
9. [Empty-state `present=false` (HTTP 200)](#9-empty-state-presentfalse-http-200)
10. [Action flags (UI only)](#10-action-flags-ui-only)
11. [Query budgets](#11-query-budgets)
12. [Date / time formats](#12-date--time-formats)

---

## 1. Contract basics

| Item | Value |
|------|--------|
| HTTP API prefix | `/api/v1/training` |
| Client contract | `clientContractVersion: V1` from `GET /api/v1/training/client/bootstrap` |
| Auth | Authenticated account session / bearer (same identity stack as other UAP APIs) |
| Error body | `ApiErrorResponse` (`code`, `message`, `timestamp`, `path`, details) |

`TrainingClientContractVersion.V1` is the **facade response contract**, distinct from the `/api/v1` URL version. Clients should assert `clientContractVersion === "V1"` at bootstrap and fail closed on unknown versions.

Bootstrap also returns feature toggles, list/history limits, algorithm version strings, preferred units, and rating scales — use these rather than hardcoding.

---

## 2. Startup → Home

```
authenticate
  → GET /api/v1/training/client/bootstrap
  → GET /api/v1/training/client/today          (?date=YYYY-MM-DD optional)
  → render Home from TrainingTodayDashboardResponse
```

Optional later navigation:

- Training hub: `GET /api/v1/training/client/training-overview`
- Recovery hub: `GET /api/v1/training/client/recovery-overview`

**Do not** call athlete-state / readiness / recommendation generate endpoints during Home load. Today is composition-only.

Home sections to render from `today`:

- `recovery` — check-in presence + ratings
- `athleteState` / `readiness` / `recommendation` — only if `*Present`
- `training` — occurrences + `primaryOccurrence`
- `trainingLoad`, `adaptation`, `recentPerformance`
- `actions` — enable/disable buttons (convenience only)

---

## 3. Recovery flow

```
GET /api/v1/training/client/recovery-overview?date=&trendDays=
  → create or update check-in
  → POST /api/v1/training/athlete-state/daily/{date}          (explicit snapshot)
  → POST /api/v1/training/readiness/daily/{date}              (or /readiness/assessments)
  → POST /api/v1/training/recommendations/daily/{date}        (or /recommendations)
  → refresh recovery-overview and/or today
```

### Check-in mutations

| Step | Call |
|------|------|
| Create | `POST /api/v1/training/recovery-check-ins` → `DailyRecoveryCheckInResponse` (`201`) |
| Update | `PATCH /api/v1/training/recovery-check-ins/{checkInId}` (send version / optimistic fields as required) |
| Read | `GET .../by-date/{date}` or facade `checkInPresent` |

Creating/updating a check-in **does not** create a snapshot, readiness assessment, or recommendation. Drive those with the explicit POSTs above (see [generation chain](#6-explicit-generation-chain)).

Use `today.actions.canCreateRecoveryCheckIn` / `canUpdateRecoveryCheckIn` / `canGenerate*` to shape UI; still handle API errors as source of truth.

---

## 4. Workout flow

```
GET /api/v1/training/client/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}/launch-context
  → optionally set environment
  → optionally generate / decide / apply adaptation
  → POST .../occurrences/{occurrenceId}/start
  → for each exercise: start execution → log sets → complete/skip execution
  → POST .../occurrences/{occurrenceId}/complete
  → POST .../occurrences/{occurrenceId}/session-effort
```

### Environment (optional, often before start / adaptation)

- `PUT .../occurrences/{occurrenceId}/environment`
- Locked after activity starts (`409 WORKOUT_OCCURRENCE_ENVIRONMENT_LOCKED`)

### Adaptation (optional)

1. Generate: `POST .../occurrences/{occurrenceId}/adaptation-proposals`  
   or recommendation-driven: `POST /recommendations/{recommendationId}/occurrences/{occurrenceId}/adaptation-proposals`
2. Decide items: `PATCH /adaptation-proposals/{proposalId}/items/{itemId}`
3. Apply: `POST .../adaptation-proposals/{proposalId}/apply`
4. Cancel / regenerate as needed

### Logging

Prefer set-level actuals:

1. `POST .../sets` (if needed) / `PATCH .../sets/{setId}` / `POST .../sets/{setId}/complete`
2. `POST .../exercises/{executionId}/complete` (requires sets complete)
3. Then complete the occurrence

Session effort is after a completed (or otherwise effort-eligible) occurrence — `201` on first submit; `409 WORKOUT_SESSION_EFFORT_ALREADY_EXISTS` if already submitted (then `PATCH`).

---

## 5. No-hidden-write semantics

Client facades are **read-only compositions**:

| Facade | Generates snapshot / readiness / recommendation? |
|--------|--------------------------------------------------|
| `GET /client/today` | **Never** |
| `GET /client/training-overview` | **Never** |
| `GET /client/recovery-overview` | **Never** |
| `GET .../launch-context` | **Never** |
| `GET /client/bootstrap` | **Never** |

If a check-in exists but no snapshot was generated, `today` returns:

- `recovery.checkInPresent = true`
- `athleteState.snapshotPresent = false`
- `readiness.readinessPresent = false`
- `recommendation.recommendationPresent = false`
- `actions.canGenerateAthleteStateSnapshot.allowed = true`
- `actions.canGenerateReadinessAssessment` disabled with `DAILY_ATHLETE_STATE_SNAPSHOT_REQUIRED`

No rows are created as a side effect of opening Home or Recovery.

Athlete-state generation itself is also explicit-only (no auto-refresh after check-in or load mutations).

---

## 6. Explicit generation chain

Strict dependency order for a calendar date:

```
DailyRecoveryCheckIn (optional but usual input)
        ↓ client POST
DailyAthleteStateSnapshot          POST /athlete-state/daily/{date}
        ↓ client POST
DailyReadinessAssessment           POST /readiness/daily/{date}
                                   (or POST /readiness/assessments with snapshotId)
        ↓ client POST
DailyTrainingRecommendation        POST /recommendations/daily/{date}
                                   (or POST /recommendations with readinessId)
        ↓ optional client POST
WorkoutAdaptationProposal          via recommendations/.../adaptation-proposals
                                   or occurrence adaptation-proposals
```

Rules:

1. Readiness requires a current athlete-state snapshot (`400 DAILY_READINESS_STATE_SNAPSHOT_REQUIRED`).
2. Recommendation requires readiness (`400 DAILY_TRAINING_RECOMMENDATION_READINESS_REQUIRED`).
3. Regenerating a snapshot (`POST .../regenerate`) does **not** auto-refresh downstream readiness/recommendation — regenerate those explicitly if the product needs freshness.
4. Facades only **report** whether each artifact is present.

---

## 7. IDs to keep

Persist / cache these from facades and mutation responses:

| ID | Why |
|----|-----|
| `athleteId` | Display / analytics context from `today.athlete` |
| `recoveryCheckInId` | Update check-in, revisions, baseline comparison |
| `dailyAthleteStateSnapshotId` | Readiness by snapshot id; versioning/compare |
| `readinessAssessmentId` | Recommendation by readiness id; history/compare |
| `recommendationId` | Detail, adaptation from recommendation |
| `trainingPlanId`, `workoutDayId`, `occurrenceId` | All workout nested routes + launch-context |
| `executionId` | Sets, substitute, performance recompute |
| `setId` | Set lifecycle |
| `adaptationProposalId` (+ item ids) | Decide / apply / cancel |
| `actualEnvironmentId` / `plannedEnvironmentId` | Feasibility & adaptation eligibility |
| `exercisePerformanceKey` | Performance history / PRs (when shown) |

From `primaryOccurrence` on Home, keep the plan/day/occurrence triple needed for launch-context and start.

---

## 8. Mutation ordering

### Day pipeline (recovery → advice)

1. Create/update check-in  
2. Generate snapshot  
3. Generate readiness  
4. Generate recommendation  
5. Refresh facades  

Skipping ahead fails with 400 dependency codes — do not race these.

### Workout pipeline

1. Launch-context (read)  
2. Set environment (if needed; before start when possible)  
3. Adaptation generate → item decisions → apply (before or carefully around start; respect locks)  
4. Start occurrence  
5. Per exercise: start → log/complete sets → complete/skip execution  
6. Complete occurrence (all executions terminal)  
7. Submit session effort  
8. Optional: training-load recompute / performance views  

Do not complete an occurrence with incomplete executions (`409 WORKOUT_OCCURRENCE_HAS_INCOMPLETE_EXERCISES`).  
Do not complete an execution with incomplete sets (`409 WORKOUT_EXERCISE_EXECUTION_HAS_INCOMPLETE_SETS`).

Optimistic / version conflicts (`409 *_VERSION_CONFLICT`, `OPTIMISTIC_LOCK_CONFLICT`): re-fetch and retry.

---

## 9. Empty-state `present=false` (HTTP 200)

Missing optional artifacts are **not** `404` on facades. Expect HTTP `200` with boolean presence flags and null detail payloads:

| Flag | Meaning when `false` |
|------|----------------------|
| `checkInPresent` | No recovery check-in for the date |
| `snapshotPresent` / athlete-state section | No current snapshot |
| `readinessPresent` | No readiness assessment |
| `recommendationPresent` | No recommendation |
| `activeProposalPresent` | No active adaptation proposal |
| `loadPresent` | No load aggregate for the window |
| `feasibilityPresent` (launch) | Feasibility section not populated |

`primaryOccurrence` may be `null` when nothing is scheduled/in-progress for the day — still `200`.

Use resource GETs (`GET /athlete-state/daily/{date}`, etc.) when you need hard 404 semantics for a specific artifact.

---

## 10. Action flags (UI only)

Facade `actions` / `TrainingClientActionFlag` (`allowed`, `reasonCode`) are **UI convenience hints**.

- They are **not** authorization.
- They are **not** a substitute for handling API status codes.
- A disabled flag means “likely blocked given current facts”; the server still enforces rules on mutate.
- Never hide a security decision behind `allowed: true`.

Examples of `reasonCode` values you may surface:

- `DAILY_ATHLETE_STATE_SNAPSHOT_REQUIRED`
- `DAILY_READINESS_ASSESSMENT_REQUIRED`
- `DAILY_*_ALREADY_EXISTS`
- `WORKOUT_OCCURRENCE_ENVIRONMENT_NOT_SET`
- `ACTIVE_WORKOUT_ADAPTATION_PROPOSAL_EXISTS`
- `INVALID_WORKOUT_OCCURRENCE_STATUS`
- `WORKOUT_SESSION_EFFORT_ALREADY_EXISTS`

---

## 11. Query budgets

Acceptance tests bound Hibernate prepared-statement counts for facade reads:

| Endpoint | Max prepared statements |
|----------|-------------------------|
| `GET /client/today` | ≤ **15** |
| `GET /client/training-overview` | ≤ **15** |
| `GET .../launch-context` | ≤ **15** |
| `GET /client/recovery-overview` | ≤ **12** |

Client implication: prefer these facades for screens instead of N+1 fan-out across many resource endpoints. Do not expect facades to expand into unbounded nested graphs.

---

## 12. Date / time formats

| Kind | Format | Examples |
|------|--------|----------|
| Calendar dates (path/query/`LocalDate`) | `YYYY-MM-DD` | `2026-07-31` |
| Instants (started/completed/achieved/generated) | ISO-8601 instant | `2026-07-31T14:22:05Z` |
| Timezone (calendar today / schedule activate) | IANA zone string | `America/New_York` |

Pass optional `date` on client facades as `YYYY-MM-DD`. Omit to use the athlete’s “today” resolution rules on the server. Out-of-range client dates → `400 INVALID_TRAINING_CLIENT_DATE`.

---

## Quick reference — primary client calls

| Intent | Method + path |
|--------|----------------|
| Bootstrap | `GET /api/v1/training/client/bootstrap` |
| Home | `GET /api/v1/training/client/today` |
| Recovery hub | `GET /api/v1/training/client/recovery-overview` |
| Training hub | `GET /api/v1/training/client/training-overview` |
| Launch workout | `GET /api/v1/training/client/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}/launch-context` |
| Create check-in | `POST /api/v1/training/recovery-check-ins` |
| Generate snapshot | `POST /api/v1/training/athlete-state/daily/{date}` |
| Generate readiness | `POST /api/v1/training/readiness/daily/{date}` |
| Generate recommendation | `POST /api/v1/training/recommendations/daily/{date}` |
| Start workout | `POST .../occurrences/{occurrenceId}/start` |
| Complete workout | `POST .../occurrences/{occurrenceId}/complete` |
| Session effort | `POST .../occurrences/{occurrenceId}/session-effort` |
