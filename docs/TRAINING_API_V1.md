# Training API v1 — Surface Map (Phase 7V)

Practical inventory of training HTTP routes under `backend/uap-server` (`com.devinolabs.uap.training.infrastructure.web.*Controller`).  
Not OpenAPI: method + path, purpose, major DTO names, and important status/error codes.

**Base prefix:** `/api/v1/training`  
**Auth:** authenticated account principal (athlete-scoped ownership).  
**Errors:** `ApiErrorResponse` via `TrainingExceptionHandler` (`code`, `message`, `timestamp`, `path`, optional field details).

Common status patterns:

| Status | Typical meaning |
|--------|-----------------|
| `200` | Success (including many generate/lifecycle mutations) |
| `201` | Created |
| `204` | Deleted / archived (no body) |
| `400` | Validation / invalid state transition / bad range |
| `401` | Unauthenticated |
| `403` | Forbidden (rare; e.g. system catalogue modification) |
| `404` | Not found (also used for inaccessible foreign resources — no existence leak) |
| `409` | Conflict / locked / duplicate / incomplete prerequisites |
| `500` | Analysis / generation / facade load failure |

---

## Table of contents

1. [Client facades](#1-client-facades)
2. [Plans](#2-plans)
3. [Days](#3-days)
4. [Exercises (prescription)](#4-exercises-prescription)
5. [Schedule](#5-schedule)
6. [Calendar](#6-calendar)
7. [Occurrences](#7-occurrences)
8. [Executions](#8-executions)
9. [Sets](#9-sets)
10. [Performance / PRs](#10-performance--prs)
11. [Environments](#11-environments)
12. [Feasibility](#12-feasibility)
13. [Adaptations](#13-adaptations)
14. [Session effort / training load](#14-session-effort--training-load)
15. [Recovery check-ins](#15-recovery-check-ins)
16. [Recovery analytics](#16-recovery-analytics)
17. [Athlete-state](#17-athlete-state)
18. [Readiness](#18-readiness)
19. [Recommendations](#19-recommendations)
20. [Exercise definitions & substitutions](#20-exercise-definitions--substitutions)

---

## 1. Client facades

**Controller:** `TrainingClientController` — `/api/v1/training/client`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `GET /client/bootstrap` | Contract version, feature flags, limits, units, rating scales | → `TrainingClientBootstrapResponse` | `200`; `404 ATHLETE_PROFILE_NOT_FOUND`; `500 TRAINING_CLIENT_BOOTSTRAP_FAILED` |
| `GET /client/today?date=` | Home dashboard composition (read-only; no hidden writes) | → `TrainingTodayDashboardResponse` | `200` with `*Present=false` empty sections; `400 INVALID_TRAINING_CLIENT_DATE`; `500 TRAINING_DASHBOARD_LOAD_FAILED` |
| `GET /client/training-overview?date=` | Plans / upcoming / weekly load / recent sessions & PRs / environments / adaptations | → `TrainingOverviewResponse` | `200`; `400 INVALID_TRAINING_CLIENT_DATE`; `500 TRAINING_OVERVIEW_LOAD_FAILED` |
| `GET /client/recovery-overview?date=&trendDays=` | Recovery composition for a day | → `RecoveryOverviewResponse` | `200`; `400 INVALID_TRAINING_CLIENT_DATE` / `INVALID_TRAINING_CLIENT_TREND_DAYS`; `500 RECOVERY_OVERVIEW_LOAD_FAILED` |
| `GET /client/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}/launch-context` | Pre-workout launch bundle | → `WorkoutLaunchContextResponse` | `200`; `404 WORKOUT_OCCURRENCE_NOT_FOUND`; `500 WORKOUT_LAUNCH_CONTEXT_LOAD_FAILED` |

Query budgets (prepared statements, acceptance-tested): today / launch-context / training-overview ≤ **15**; recovery-overview ≤ **12**.

---

## 2. Plans

**Controller:** `TrainingPlanController` — `/api/v1/training/plans`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `POST /plans` | Create plan | `CreateTrainingPlanRequest` → `TrainingPlanResponse` | `201`; `400` invalid type/dates; `409 DUPLICATE_TRAINING_PLAN` |
| `GET /plans` | List plans (`status`, `planType` filters) | → `List<TrainingPlanResponse>` | `200` |
| `GET /plans/{planId}` | Get plan | → `TrainingPlanResponse` | `200`; `404 TRAINING_PLAN_NOT_FOUND` |
| `PATCH /plans/{planId}` | Update plan fields | `UpdateTrainingPlanRequest` → `TrainingPlanResponse` | `200`; `404`; `409 TRAINING_PLAN_ARCHIVED` |
| `PATCH /plans/{planId}/status` | Change plan status | `TrainingPlanStatusRequest` → `TrainingPlanResponse` | `200`; `400 INVALID_TRAINING_PLAN_STATUS` |
| `DELETE /plans/{planId}` | Delete plan | — | `204`; `409 TRAINING_PLAN_DELETE_NOT_ALLOWED` |

---

## 3. Days

**Controller:** `WorkoutDayController` — `/api/v1/training/plans/{planId}/days`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `POST .../days` | Create workout day | `CreateWorkoutDayRequest` → `WorkoutDayResponse` | `201`; `409 DUPLICATE_WORKOUT_DAY` |
| `GET .../days` | List days | → `List<WorkoutDayResponse>` | `200` |
| `GET .../days/{dayId}` | Get day | → `WorkoutDayResponse` | `200`; `404 WORKOUT_DAY_NOT_FOUND` |
| `PATCH .../days/{dayId}` | Update day | `UpdateWorkoutDayRequest` → `WorkoutDayResponse` | `200` |
| `PATCH .../days/{dayId}/status` | Change day status | status request → `WorkoutDayResponse` | `200`; `400 INVALID_WORKOUT_DAY_STATUS` |
| `PUT .../days/order` | Reorder days | `ReorderWorkoutDaysRequest` → `List<WorkoutDayResponse>` | `200`; `400 INVALID_WORKOUT_DAY_ORDER` |
| `DELETE .../days/{dayId}` | Delete day | — | `204`; `409 WORKOUT_DAY_DELETE_NOT_ALLOWED` |

---

## 4. Exercises (prescription)

**Controller:** `WorkoutExerciseController` — `/api/v1/training/plans/{planId}/days/{dayId}/exercises`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `POST .../exercises` | Add prescribed exercise | `CreateWorkoutExerciseRequest` → `WorkoutExerciseResponse` | `201`; `409 DUPLICATE_WORKOUT_EXERCISE` |
| `GET .../exercises` | List prescribed exercises | → `List<WorkoutExerciseResponse>` | `200` |
| `GET .../exercises/{exerciseId}` | Get exercise | → `WorkoutExerciseResponse` | `200`; `404 WORKOUT_EXERCISE_NOT_FOUND` |
| `PATCH .../exercises/{exerciseId}` | Update prescription | `UpdateWorkoutExerciseRequest` → `WorkoutExerciseResponse` | `200` |
| `PATCH .../exercises/{exerciseId}/status` | Change exercise status | status request → `WorkoutExerciseResponse` | `200`; `400 INVALID_WORKOUT_EXERCISE_STATUS` |
| `PUT .../exercises/order` | Reorder exercises | reorder request → `List<WorkoutExerciseResponse>` | `200`; `400 INVALID_WORKOUT_EXERCISE_ORDER` |
| `DELETE .../exercises/{exerciseId}` | Delete exercise | — | `204`; `409 WORKOUT_EXERCISE_DELETE_NOT_ALLOWED` |

---

## 5. Schedule

**Controller:** `TrainingPlanScheduleController` — `/api/v1/training/plans/{planId}/schedule`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `POST .../schedule/activate` | Activate schedule (+ optional initial generation) | `ActivateTrainingPlanScheduleRequest` → `TrainingPlanScheduleActivationResponse` | `200`; `409` schedule not configured / requires days / placement locked |
| `POST .../schedule/pause` | Pause schedule | → `TrainingPlanResponse` | `200`; `400 INVALID_TRAINING_PLAN_SCHEDULE_STATUS` |
| `POST .../schedule/resume` | Resume schedule | → `TrainingPlanResponse` | `200` |
| `POST .../schedule/complete` | Complete schedule | → `TrainingPlanResponse` | `200` |
| `POST .../schedule/generate` | Generate occurrences in range | `GenerateWorkoutOccurrencesRequest` → `WorkoutOccurrenceGenerationResponse` | `200`; `400 INVALID_WORKOUT_OCCURRENCE_GENERATION_RANGE`; `409 WORKOUT_OCCURRENCE_GENERATION_CONFLICT` |

---

## 6. Calendar

**Controller:** `TrainingCalendarController` — `/api/v1/training/calendar`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `GET /calendar?scheduledFrom&scheduledTo` | Athlete calendar entries (optional `status`, `trainingPlanId`) | → `List<AthleteCalendarEntryResponse>` | `200`; `400 INVALID_TRAINING_CALENDAR_RANGE` |
| `GET /calendar/today?timezone=` | Occurrences for “today” in timezone | → `AthleteTrainingTodayResponse` | `200`; `400 INVALID_TIMEZONE` |

---

## 7. Occurrences

**Controller:** `WorkoutOccurrenceController` — `/api/v1/training/plans/{planId}/days/{dayId}/occurrences`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `POST .../occurrences` | Create occurrence | create request → `WorkoutOccurrenceDetailResponse` | `201`; `409 DUPLICATE_WORKOUT_OCCURRENCE` |
| `GET .../occurrences` | List occurrences for day | → `List<WorkoutOccurrenceResponse>` | `200` |
| `GET .../occurrences/{occurrenceId}` | Get occurrence detail | → `WorkoutOccurrenceDetailResponse` | `200`; `404 WORKOUT_OCCURRENCE_NOT_FOUND` |
| `PATCH .../occurrences/{occurrenceId}` | Update occurrence | update request → `WorkoutOccurrenceDetailResponse` | `200` |
| `POST .../occurrences/{occurrenceId}/reschedule` | Reschedule | reschedule request → `WorkoutOccurrenceDetailResponse` | `200`; `409 WORKOUT_OCCURRENCE_RESCHEDULE_NOT_ALLOWED` |
| `POST .../occurrences/{occurrenceId}/start` | Start workout | → `WorkoutOccurrenceDetailResponse` | `200`; `400 INVALID_WORKOUT_OCCURRENCE_STATUS` |
| `POST .../occurrences/{occurrenceId}/complete` | Complete workout | → `WorkoutOccurrenceDetailResponse` | `200`; `409 WORKOUT_OCCURRENCE_HAS_INCOMPLETE_EXERCISES` / `REQUIRES_EXERCISES` |
| `POST .../occurrences/{occurrenceId}/skip` | Skip workout | → `WorkoutOccurrenceDetailResponse` | `200` |
| `POST .../occurrences/{occurrenceId}/cancel` | Cancel workout | → `WorkoutOccurrenceDetailResponse` | `200` |
| `DELETE .../occurrences/{occurrenceId}` | Delete occurrence | — | `204`; `409 WORKOUT_OCCURRENCE_DELETE_NOT_ALLOWED` |
| `PUT .../occurrences/{occurrenceId}/environment` | Set actual environment | environment request → `WorkoutOccurrenceResponse` | `200`; `409 WORKOUT_OCCURRENCE_ENVIRONMENT_LOCKED` |
| `DELETE .../occurrences/{occurrenceId}/environment` | Clear actual environment | → `WorkoutOccurrenceResponse` | `200`; `409 WORKOUT_OCCURRENCE_ENVIRONMENT_NOT_SET` |

---

## 8. Executions

**Controller:** `WorkoutExerciseExecutionController` — `.../occurrences/{occurrenceId}/exercises`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `GET .../exercises` | List executions | → `List<WorkoutExerciseExecutionResponse>` | `200` |
| `GET .../exercises/{executionId}` | Get execution | → `WorkoutExerciseExecutionResponse` | `200`; `404 WORKOUT_EXERCISE_EXECUTION_NOT_FOUND` |
| `POST .../exercises/{executionId}/start` | Start execution | → `WorkoutExerciseExecutionResponse` | `200`; `400 INVALID_WORKOUT_EXERCISE_EXECUTION_STATUS` |
| `PATCH .../exercises/{executionId}` | Update execution notes/fields | update request → `WorkoutExerciseExecutionResponse` | `200`; `400 WORKOUT_EXERCISE_EXECUTION_ACTUALS_ARE_SET_DERIVED` |
| `POST .../exercises/{executionId}/complete` | Complete execution | → `WorkoutExerciseExecutionResponse` | `200`; `409` requires set / has incomplete sets |
| `POST .../exercises/{executionId}/skip` | Skip execution | → `WorkoutExerciseExecutionResponse` | `200` |
| `POST .../exercises/{executionId}/substitute` | Substitute performed exercise | substitute request → `WorkoutExerciseExecutionResponse` | `200`; `409` substitution locked / already uses definition |
| `POST .../exercises/{executionId}/substitute/revert` | Revert substitution | → `WorkoutExerciseExecutionResponse` | `200`; `409 WORKOUT_EXERCISE_NOT_SUBSTITUTED` |
| `GET .../exercises/{executionId}/substitution-candidates` | Candidate substitutions | → `List<OccurrenceSubstitutionCandidateResponse>` | `200` |
| `GET .../exercises/{executionId}/substitutions` | Substitution history | → `List<WorkoutExerciseSubstitutionResponse>` | `200` |

---

## 9. Sets

**Controller:** `WorkoutExerciseSetController` — `.../exercises/{executionId}/sets`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `GET .../sets` | List sets | → `List<WorkoutExerciseSetResponse>` | `200` |
| `GET .../sets/{setId}` | Get set | → `WorkoutExerciseSetResponse` | `200`; `404 WORKOUT_EXERCISE_SET_NOT_FOUND` |
| `POST .../sets` | Add set | add request → `WorkoutExerciseSetResponse` | `201`; `409 WORKOUT_EXERCISE_SET_LIMIT_EXCEEDED` |
| `PATCH .../sets/{setId}` | Update / log set actuals | update request → `WorkoutExerciseSetResponse` | `200`; `400 INVALID_WORKOUT_EXERCISE_SET_STATUS` |
| `POST .../sets/{setId}/start` | Start set | → `WorkoutExerciseSetResponse` | `200` |
| `POST .../sets/{setId}/complete` | Complete set | → `WorkoutExerciseSetResponse` | `200` |
| `POST .../sets/{setId}/skip` | Skip set | → `WorkoutExerciseSetResponse` | `200` |
| `DELETE .../sets/{setId}` | Delete set | — | `204`; `409 WORKOUT_EXERCISE_SET_DELETE_NOT_ALLOWED` |
| `POST .../sets/reorder` | Reorder sets | reorder request → `List<WorkoutExerciseSetResponse>` | `200`; `409` reorder not allowed / duplicate order |

---

## 10. Performance / PRs

**Controllers:** `TrainingPerformanceController`, `WorkoutOccurrencePerformanceController`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `GET /performance/exercises/{exercisePerformanceKey}` | Exercise performance history | → `AthleteExercisePerformanceHistoryResponse` | `200`; `404 EXERCISE_PERFORMANCE_KEY_NOT_FOUND`; `400 INVALID_TRAINING_PERFORMANCE_RANGE` |
| `GET /performance/exercises/{exercisePerformanceKey}/personal-records` | PRs for one exercise key | → `List<PersonalRecordResponse>` | `200` |
| `GET /performance/personal-records` | List PRs (optional filters) | → `List<PersonalRecordResponse>` | `200` |
| `GET /performance/personal-records/recent` | Recent PRs (`days`, `limit`) | → `List<PersonalRecordResponse>` | `200` |
| `GET .../occurrences/{occurrenceId}/performance` | Occurrence performance summary | → `WorkoutOccurrencePerformanceResponse` | `200` |
| `POST .../exercises/{executionId}/performance/recompute` | Recompute execution metrics | → `ExerciseExecutionPerformanceResponse` | `200`; `409` metrics require completed execution/sets; `409 TRAINING_METRICS_RECOMPUTATION_CONFLICT` |

---

## 11. Environments

**Controller:** `TrainingEnvironmentController` — `/api/v1/training/environments`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `POST /environments` | Create environment | `CreateTrainingEnvironmentRequest` → `TrainingEnvironmentResponse` | `201`; `409 DUPLICATE_TRAINING_ENVIRONMENT` |
| `GET /environments` | Page/list environments | → `TrainingEnvironmentPageResponse` | `200` |
| `GET /environments/{environmentId}` | Get environment | → `TrainingEnvironmentResponse` | `200`; `404 TRAINING_ENVIRONMENT_NOT_FOUND` / `_NOT_ACCESSIBLE` |
| `PATCH /environments/{environmentId}` | Update environment | `UpdateTrainingEnvironmentRequest` → `TrainingEnvironmentResponse` | `200`; `409 TRAINING_ENVIRONMENT_ARCHIVED` |
| `DELETE /environments/{environmentId}` | Archive environment | — | `204` |
| `POST /environments/{environmentId}/default` | Set default environment | → `TrainingEnvironmentResponse` | `200`; `409 TRAINING_ENVIRONMENT_DEFAULT_CONFLICT` |

---

## 12. Feasibility

**Controller:** `FeasibilityController` — under `/api/v1/training/plans`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `GET /plans/{planId}/days/{dayId}/feasibility` | Day feasibility vs environment (required `trainingEnvironmentId`) | → `WorkoutDayFeasibilityResponse` | `200`; `400` invalid mode/limit; `500 WORKOUT_FEASIBILITY_ANALYSIS_FAILED` |
| `GET /plans/{planId}/feasibility` | Plan-level feasibility | → `TrainingPlanFeasibilityResponse` | `200`; `400 INVALID_FEASIBILITY_ENVIRONMENT_MODE` |
| `GET .../occurrences/{occurrenceId}/feasibility` | Occurrence feasibility (uses occurrence environment context) | → `WorkoutOccurrenceFeasibilityResponse` | `200` |

Optional query params (where supported): `suggestionLimit`, `includeAlternatives`, `usePreferredEnvironments`.

---

## 13. Adaptations

**Controller:** `WorkoutAdaptationProposalController` (+ recommended path on recommendations)

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `POST .../occurrences/{occurrenceId}/adaptation-proposals` | Generate proposal | `GenerateWorkoutAdaptationProposalRequest?` → `WorkoutAdaptationProposalResponse` | `201`; `409 ACTIVE_WORKOUT_ADAPTATION_PROPOSAL_EXISTS` / environment required |
| `GET /adaptation-proposals/{proposalId}` | Get proposal | → `WorkoutAdaptationProposalResponse` | `200`; `404` not found / not accessible |
| `GET /adaptation-proposals` | List proposals (`occurrenceId`, `status`, page/size) | → `List<WorkoutAdaptationProposalSummaryResponse>` | `200` |
| `PATCH /adaptation-proposals/{proposalId}/items/{itemId}` | Accept/exclude/retarget item | `UpdateWorkoutAdaptationProposalItemRequest` → `WorkoutAdaptationProposalResponse` | `200`; `409` locked / stale / version conflict / item mismatch |
| `POST /adaptation-proposals/{proposalId}/cancel` | Cancel proposal | → `WorkoutAdaptationProposalResponse` | `200`; `409 WORKOUT_ADAPTATION_PROPOSAL_TERMINAL` |
| `POST /adaptation-proposals/{proposalId}/regenerate` | Regenerate proposal | `GenerateWorkoutAdaptationProposalRequest?` → `WorkoutAdaptationProposalResponse` | `201` |
| `POST .../adaptation-proposals/{proposalId}/apply` | Apply accepted items to occurrence | → `WorkoutAdaptationApplicationResponse` | `200`; `409` unresolved / expired / locked |

Also: `POST /recommendations/{recommendationId}/occurrences/{occurrenceId}/adaptation-proposals` → `WorkoutAdaptationProposalResponse` (`201`) for recommendation-driven generation.

---

## 14. Session effort / training load

**Controller:** `TrainingLoadController`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `POST .../occurrences/{occurrenceId}/session-effort` | Submit session RPE/effort | `SubmitWorkoutSessionEffortRequest` → `WorkoutSessionEffortResponse` | `201`; `409 WORKOUT_SESSION_EFFORT_ALREADY_EXISTS` / `_NOT_ALLOWED` |
| `PATCH .../occurrences/{occurrenceId}/session-effort` | Update effort | `UpdateWorkoutSessionEffortRequest` → `WorkoutSessionEffortResponse` | `200`; `404 WORKOUT_SESSION_EFFORT_NOT_FOUND` |
| `GET .../occurrences/{occurrenceId}/session-effort` | Get effort | → `WorkoutSessionEffortResponse` | `200`; `404` |
| `GET .../occurrences/{occurrenceId}/session-effort/revisions` | Effort revision history | → `WorkoutSessionEffortRevisionListResponse` | `200` |
| `GET .../occurrences/{occurrenceId}/training-load` | Occurrence load summary | → `WorkoutOccurrenceLoadSummaryResponse` | `200`; `404 WORKOUT_LOAD_SUMMARY_NOT_FOUND` |
| `POST .../occurrences/{occurrenceId}/training-load/recompute` | Recompute occurrence load | → `WorkoutOccurrenceLoadSummaryResponse` | `200`; `409 WORKOUT_LOAD_CALCULATION_FAILED` |
| `POST /training-load/rebuild` | Rebuild athlete training load | → `RebuildTrainingLoadResponse` | `200`; `409 TRAINING_LOAD_REBUILD_CONFLICT` / `_FAILED` |
| `GET /training-load/history` | Load history (`startDate`, `endDate`, `granularity`, optional `trainingPlanId`) | → `TrainingLoadHistoryResponse` | `200`; `400 INVALID_TRAINING_LOAD_DATE_RANGE` / `_GRANULARITY` |

---

## 15. Recovery check-ins

**Controller:** `RecoveryCheckInController`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `POST /recovery-check-ins` | Create daily check-in | `CreateDailyRecoveryCheckInRequest` → `DailyRecoveryCheckInResponse` | `201`; `409 RECOVERY_CHECK_IN_ALREADY_EXISTS`; `400 EMPTY_RECOVERY_CHECK_IN` / rating errors |
| `PATCH /recovery-check-ins/{checkInId}` | Update check-in | `UpdateDailyRecoveryCheckInRequest` → `DailyRecoveryCheckInResponse` | `200`; `409 RECOVERY_CHECK_IN_VERSION_CONFLICT` |
| `GET /recovery-check-ins/{checkInId}` | Get by id | → `DailyRecoveryCheckInResponse` | `200`; `404 RECOVERY_CHECK_IN_NOT_FOUND` |
| `GET /recovery-check-ins/by-date/{date}` | Get by date | → `DailyRecoveryCheckInResponse` | `200`; `404` |
| `GET /recovery-check-ins` | List range | → `DailyRecoveryCheckInListResponse` | `200`; `400 INVALID_RECOVERY_CHECK_IN_DATE_RANGE` |
| `GET /recovery-check-ins/{checkInId}/revisions` | Revisions | → `DailyRecoveryCheckInRevisionListResponse` | `200` |
| `GET /recovery-check-ins/calendar` | Check-in calendar | → `RecoveryCheckInCalendarResponse` | `200`; `400 INVALID_RECOVERY_CALENDAR_DATE_RANGE` |
| `GET /recovery-check-ins/{checkInId}/baseline-comparison` | Baseline comparison by id | → `DailyRecoveryBaselineComparisonResponse` | `200` |
| `GET /recovery-check-ins/by-date/{date}/baseline-comparison` | Baseline comparison by date | → `DailyRecoveryBaselineComparisonResponse` | `200` |
| `GET /recovery-check-ins/history` | Recovery history (+ optional training load) | → `AthleteRecoveryHistoryResponse` | `200` |

---

## 16. Recovery analytics

**Controller:** `RecoveryAnalyticsController`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `GET /recovery-analytics/trends/{metricType}` | Metric trend | → `RecoveryMetricTrendResponse` | `200`; `400 INVALID_RECOVERY_METRIC_TYPE` / trend range |
| `GET /recovery-analytics/dashboard` | Baseline dashboard (`targetDate`, `baselineWindowDays`) | → `RecoveryBaselineDashboardResponse` | `200`; `400 INVALID_RECOVERY_BASELINE_WINDOW`; `500 RECOVERY_ANALYTICS_CALCULATION_FAILED` |
| `GET /recovery-analytics/discomfort-history` | Body-area discomfort history | → `BodyAreaDiscomfortHistoryResponse` | `200` |

---

## 17. Athlete-state

**Controller:** `DailyAthleteStateController` — `/api/v1/training/athlete-state`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `POST /athlete-state/daily/{date}` | **Explicitly** generate current snapshot | `GenerateDailyAthleteStateSnapshotRequest` → `DailyAthleteStateSnapshotResponse` | `200`; `409 DAILY_ATHLETE_STATE_VERSION_CONFLICT` / source inconsistent; `500` generation failed |
| `POST /athlete-state/daily/{date}/regenerate` | Regenerate new version | same request → `DailyAthleteStateSnapshotResponse` | `200` |
| `GET /athlete-state/daily/{date}` | Get current snapshot for date | → `DailyAthleteStateSnapshotResponse` | `200`; `404 DAILY_ATHLETE_STATE_SNAPSHOT_NOT_FOUND` |
| `GET /athlete-state/snapshots/{snapshotId}` | Get snapshot by id | → `DailyAthleteStateSnapshotResponse` | `200`; `404` |
| `GET /athlete-state/daily/{date}/versions` | List versions for date | → `List<DailyAthleteStateSnapshotVersionResponse>` | `200` |
| `GET /athlete-state/history` | History page | → `DailyAthleteStateHistoryResponse` | `200` |
| `GET /athlete-state/snapshots/compare` | Compare two snapshots | → `DailyAthleteStateSnapshotComparisonResponse` | `200`; `400 DAILY_ATHLETE_STATE_SNAPSHOT_COMPARE_INVALID` |

Dashboards never call these for you — generation is client-explicit.

---

## 18. Readiness

**Controller:** `DailyReadinessController` — `/api/v1/training/readiness`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `POST /readiness/assessments` | Generate from snapshot id | `GenerateDailyReadinessAssessmentRequest` → `DailyReadinessAssessmentResponse` | `200`; `400 DAILY_READINESS_STATE_SNAPSHOT_REQUIRED` |
| `POST /readiness/daily/{date}` | Generate from current snapshot for date | → `DailyReadinessAssessmentResponse` | `200`; `400` snapshot required |
| `GET /readiness/assessments/{assessmentId}` | Get assessment | → `DailyReadinessAssessmentResponse` | `200`; `404 DAILY_READINESS_ASSESSMENT_NOT_FOUND` |
| `GET /readiness/history` | History | → `DailyReadinessHistoryResponse` | `200`; `400 INVALID_DAILY_READINESS_DATE_RANGE` |
| `GET /readiness/assessments/compare` | Compare assessments | → `DailyReadinessAssessmentComparisonResponse` | `200`; `400 DAILY_READINESS_COMPARE_INVALID` |

---

## 19. Recommendations

**Controller:** `DailyTrainingRecommendationController` — `/api/v1/training/recommendations`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `POST /recommendations` | Generate from readiness id | `GenerateDailyTrainingRecommendationRequest` → `DailyTrainingRecommendationResponse` | `200`; `400 DAILY_TRAINING_RECOMMENDATION_READINESS_REQUIRED` |
| `POST /recommendations/daily/{date}` | Generate from current readiness for date | → `DailyTrainingRecommendationResponse` | `200` |
| `GET /recommendations/{recommendationId}` | Get recommendation | → `DailyTrainingRecommendationResponse` | `200`; `404 DAILY_TRAINING_RECOMMENDATION_NOT_FOUND` |
| `GET /recommendations/history` | History | → `DailyTrainingRecommendationHistoryResponse` | `200` |
| `GET /recommendations/compare` | Compare recommendations | → `DailyTrainingRecommendationComparisonResponse` | `200` |
| `POST /recommendations/{recommendationId}/occurrences/{occurrenceId}/adaptation-proposals` | Recommendation-driven adaptation | `GenerateWorkoutAdaptationProposalRequest?` → `WorkoutAdaptationProposalResponse` | `201`; `400` not adaptation eligible / occurrence mismatch; `409` occurrence not eligible / locked |

---

## 20. Exercise definitions & substitutions

**Controllers:** `ExerciseDefinitionController`, `ExerciseSubstitutionRelationshipController`

| Method + path | Purpose | DTOs | Important codes |
|---------------|---------|------|-----------------|
| `POST /exercise-definitions` | Create athlete definition | `CreateExerciseDefinitionRequest` → `ExerciseDefinitionResponse` | `201`; `409 DUPLICATE_EXERCISE_DEFINITION` |
| `GET /exercise-definitions` | Search/page definitions | → `ExerciseDefinitionPageResponse` | `200`; `400 INVALID_EXERCISE_DEFINITION_QUERY` |
| `GET /exercise-definitions/{exerciseDefinitionId}` | Get definition | → `ExerciseDefinitionResponse` | `200`; `404` |
| `PATCH /exercise-definitions/{exerciseDefinitionId}` | Update definition | `UpdateExerciseDefinitionRequest` → `ExerciseDefinitionResponse` | `200`; `403 SYSTEM_EXERCISE_DEFINITION_MODIFICATION_NOT_ALLOWED` |
| `DELETE /exercise-definitions/{exerciseDefinitionId}` | Archive definition | — | `204`; `409 EXERCISE_DEFINITION_ARCHIVED` |
| `POST /exercise-definitions/{sourceDefinitionId}/substitutions` | Create substitution relationship | `CreateExerciseSubstitutionRelationshipRequest` → `ExerciseSubstitutionRelationshipResponse` | `201`; `409 DUPLICATE_EXERCISE_SUBSTITUTION_RELATIONSHIP` |
| `GET /exercise-definitions/{sourceDefinitionId}/substitution-candidates` | Catalogue candidates | → `List<ExerciseSubstitutionCandidateResponse>` | `200` |
| `GET /exercise-definitions/{exerciseDefinitionId}/environment-compatibility/{environmentId}` | Compatibility check | → `ExerciseEnvironmentCompatibilityResponse` | `200` |
| `GET /exercise-substitution-relationships/{relationshipId}` | Get relationship | → `ExerciseSubstitutionRelationshipResponse` | `200`; `404` |
| `PATCH /exercise-substitution-relationships/{relationshipId}` | Update relationship | `UpdateExerciseSubstitutionRelationshipRequest` → `ExerciseSubstitutionRelationshipResponse` | `200`; `403` system modification not allowed |
| `DELETE /exercise-substitution-relationships/{relationshipId}` | Archive relationship | — | `204` |

---

## Controller inventory (22)

| Controller | Base / notes |
|------------|--------------|
| `TrainingClientController` | `/api/v1/training/client` |
| `TrainingPlanController` | `/api/v1/training/plans` |
| `WorkoutDayController` | `/api/v1/training/plans/{planId}/days` |
| `WorkoutExerciseController` | `.../days/{dayId}/exercises` |
| `TrainingPlanScheduleController` | `.../plans/{planId}/schedule` |
| `TrainingCalendarController` | `/api/v1/training/calendar` |
| `WorkoutOccurrenceController` | `.../days/{dayId}/occurrences` |
| `WorkoutExerciseExecutionController` | `.../occurrences/{occurrenceId}/exercises` |
| `WorkoutExerciseSetController` | `.../exercises/{executionId}/sets` |
| `TrainingPerformanceController` | `/api/v1/training/performance` |
| `WorkoutOccurrencePerformanceController` | `.../occurrences/{occurrenceId}` performance |
| `TrainingEnvironmentController` | `/api/v1/training/environments` |
| `FeasibilityController` | feasibility under `/plans` |
| `WorkoutAdaptationProposalController` | adaptation-proposals (mixed absolute paths) |
| `TrainingLoadController` | session-effort + training-load (absolute paths) |
| `RecoveryCheckInController` | `/api/v1/training/recovery-check-ins` |
| `RecoveryAnalyticsController` | `/api/v1/training/recovery-analytics` |
| `DailyAthleteStateController` | `/api/v1/training/athlete-state` |
| `DailyReadinessController` | `/api/v1/training/readiness` |
| `DailyTrainingRecommendationController` | `/api/v1/training/recommendations` |
| `ExerciseDefinitionController` | `/api/v1/training/exercise-definitions` |
| `ExerciseSubstitutionRelationshipController` | `/api/v1/training/exercise-substitution-relationships` |

See also: [`TRAINING_CLIENT_HANDOFF_V1.md`](./TRAINING_CLIENT_HANDOFF_V1.md) for recommended mobile/web flows.
