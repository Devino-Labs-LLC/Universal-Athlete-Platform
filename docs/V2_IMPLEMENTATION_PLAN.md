# Athlete Readiness V2 — Implementation Plan

**Theme:** Individual Athlete Intelligence & Readiness
**Product question:** Can an athlete use Athlete Readiness independently — without a coach or team — and receive meaningful insight into preparedness, recovery, what they have done, and how they are progressing?

**Baseline:** Athlete V1 is frozen (`main` @ planning time). V2 extends canonical behavior. It is not a rewrite.

**Authoritative sources used**

| Precedence | Finding |
| --- | --- |
| ADRs | None present in the repository |
| Contracts | `docs/TRAINING_API_V1.md`, `docs/TRAINING_CLIENT_HANDOFF_V1.md` |
| Implementation | `backend/uap-server`, `apps/web`, `apps/mobile` |
| Migrations | Flyway `V1`–`V29` |
| Tests | Backend acceptance/HTTP suites; web RC map; mobile home/recovery/workout suites |
| Figma | Not inspected in this phase; extend V1 visual language |
| ClickUp | No ClickUp architecture/scope files in the repository |

Internal names remain Universal Athlete Platform / UAP. Commercial name is Athlete Readiness (Devino Labs LLC). No repository-wide rename.

---

## 1. V2 scope

In scope: athlete-facing readiness/recovery/training/progress intelligence; independent-athlete viability; mobile daily experience; web historical/profile depth; explicit generation UX; empty/partial-data honesty; tests and docs.

Out of scope unless later explicitly approved: V3 coach/team/org; V4 billing/launch; V5 provider integrations; V6 generative/predictive AI; V7 marketplace/social. Purple / AI tokens stay unused.

**Non-negotiable V1 semantics**

- GET `/client/bootstrap`, `/today`, `/training-overview`, `/recovery-overview`, and launch-context remain **read-only**. No hidden snapshot, readiness, recommendation, workout, billing, or integration writes.
- Generation chain stays explicit: check-in → `POST /athlete-state/daily/{date}` → `POST /readiness/daily/{date}` → `POST /recommendations/daily/{date}`.
- Clients never calculate a second readiness or recommendation score.
- Server-side authorization remains the security boundary.

---

## 2. Current-state findings

### Already complete (do not reimplement)

| Area | Evidence |
| --- | --- |
| Recovery check-in domain | Required 1–5: fatigue, muscle soreness, stress, mood, motivation. Optional: sleep quality (1–5), sleep duration (0–1440 min), body-area discomfort (1–5), notes (≤2000). Create/update, optimistic version, revisions, calendar, history, baselines. `CreateDailyRecoveryCheckInRequest`, Flyway `V25`. |
| Athlete state | State Engine snapshots, versions, history, compare. Explicit `POST` generate/regenerate only. Flyway `V26`. |
| Readiness calculation | Server `ReadinessCalculator`. Score may be null. Bands: `HIGH`, `MODERATE`, `LOW`, `INSUFFICIENT_DATA`. Contributions, limiting/strongest dimensions, `ReadinessReasonCode`, `ReadinessDataSufficiency`. Flyway `V27`. Algorithm version `READINESS_V1`. |
| Recommendations | Separate from readiness. Actions: `PROCEED_AS_PLANNED`, `MODIFY_SESSION`, `CONSIDER_RECOVERY_SESSION`, `NO_SCHEDULED_TRAINING`, `INSUFFICIENT_DATA`, `TRAINING_ALREADY_COMPLETED`. Adjustments include reduce intensity/volume/duration and optional recovery focus. Flyway `V28`/`V29`. |
| Session effort / load | POST (201) / PATCH / GET / revisions; `409 WORKOUT_SESSION_EFFORT_ALREADY_EXISTS`. Training-load history OCCURRENCE/DAILY/WEEKLY. Flyway `V24`. |
| Workout lifecycle | Plan → day → prescription → schedule activate/generate → occurrence start/complete/skip → set logging. Mobile execute + RPE. |
| Performance / PRs | Exercise history, personal records, occurrence performance. |
| Goals | Full athlete goal CRUD. Types include strength, endurance, speed, mobility, sport performance, general fitness. Onboarding + profile manage on both clients. Do **not** let UI labels become planning logic. |
| Profile / onboarding | Profile (name, DOB, sex, height, weight, dominance). Sports. Goals. Environments (equipment/context). Progressive onboarding: profile → sports → goals → Home. |
| Today facade | `GET /client/today` composes recovery, athlete-state presence, readiness, recommendation, training, load, adaptation, recent performance, action flags. HTTP 200 + `*Present=false`. |
| Web planning | Create plan, builder, schedule, calendar, catalog, environments. |
| Tests | Backend recovery/readiness/recommendation/load acceptance tests; web RC01–RC19; mobile home/recovery/workout suites. |

### Partially implemented (V2 work)

| Area | Gap |
| --- | --- |
| Dashboard intelligence | Today facade now loads stored `limitingDimensions` / `adjustmentTypes`. Home distinguishes empty vs generate-needed vs scheduled-none. Remaining polish is a11y/regression, not a second calculation. |
| Readiness explanation | Presentation-only limiting-factor copy on Home and detail. Clients never compute a score. |
| Athlete-state consumption | Presence + generate actions exist. Dimension/trend detail remains on recovery/analytics, not a second Home engine. |
| Training history | History screens exist; mobile now has screen-level empty/error/row coverage. |
| Progress | Performance web + mobile compose counts from existing APIs. Charts stay hidden until three weekly summaries exist. No client trend direction. |
| Independent athlete | Mobile now has a minimum create/activate personal-plan path (`/(tabs)/training/create-plan`). Not a port of the web planner. |
| Empty / partial UX | Home, progress, history, and plan-empty states distinguish no history / insufficient / true empty. |

### Backend complete / client incomplete (historical; now closed in V2)

These were true at planning time. Current repository state:

- Mobile personal-plan create / day / schedule activate — implemented as the minimum path, not a web planner port.
- Athlete-language readiness explanation on Home — implemented from stored today-facade dimensions.
- Home readiness/guidance cards tap through to existing detail routes; Insights pipeline is visible on incomplete Home days.
- Guidance `explanationKey` maps to athlete copy; unknown keys use an explicit fallback, never raw snake_case as the only message.
- Distinct generate-vs-missing empty states use `actions.canGenerate*` + `*Present`.
- Bootstrap `features.*` flags remain parsed and unused as a V2 UI gate (intentional; no new product surface).

### Client complete / backend incomplete

- **Today facade drop (fixed):** `GET /client/today` now loads stored `limitingDimensions` and `adjustmentTypes` via the same repository methods as recovery overview. GET remains read-only. Populated-day query cap is 17.
- **Launch-context `adjustmentTypes` (resolved, intentional):** Launch-context is a pre-workout action bundle. V1 docs do not require populating `recommendationContext.adjustmentTypes`. No launch UI consumes the array. Today/recovery remain the dashboard surfaces for stored adjustment types. Leave empty unless launch UI later shows adjustment chips. Query budget stays ≤ 15. Do not copy the today-facade fill.
- Do not add energy or hydration — they are not in the State Engine model.

### Completely missing (and should stay missing in V2)

| Signal / feature | Decision |
| --- | --- |
| Energy rating | Not in domain. **Deferred.** Do not add a parallel wellness metric. |
| Hydration | Not in domain. **Deferred.** |
| Pain as a separate metric | Use existing discomfort areas. Do not add a second pain model. |
| Client-side readiness/recommendation engines | Forbidden. |
| Hidden generate-on-GET | Forbidden. |
| Coach/team/org expansion | V3. |
| Wearables / music / nutrition | V5. |
| Generative AI coach | V6. |

### Explicitly deferred

- Changing `clientContractVersion` from `V1` unless a facade field is truly required.
- New Flyway version unless a schema gap is proven (none required for explanation/empty-state/progress composition).
- Web live workout execution (V1 intentionally omitted; mobile remains the execution surface).
- Goals driving plan generation (only through existing Planning Orchestrator — do not invent frontend planning rules).
- Training-experience / preferred-units profile fields beyond bootstrap units and environments.

---

## 3. Classification summary

| V2 requirement | Status |
| --- | --- |
| Athlete readiness dashboard | Implemented for V2 (today fill + Home empty/insight copy) |
| Recovery check-in V2 | Already complete for supported signals; client UX polish only |
| Athlete state | Backend complete; client consume-and-generate only |
| Readiness | Backend complete; athlete-facing explanation implemented |
| Readiness explanation | Implemented (presentation-only; unknown keys stay honest) |
| Daily training recommendation | Already complete; athlete-readable `explanationKey` added |
| Session effort / RPE | Already complete |
| Training history | Implemented as composition of existing history surfaces |
| Athlete progress | Implemented (count-based; no fabricated trends) |
| Athlete goals | Already complete (manage); planning influence deferred |
| Personal training / independent athlete | Minimum mobile create/activate path implemented |
| Athlete profile | Already complete for V2 purpose |
| Onboarding | Already complete; do not expand questionnaire |
| Mobile-first daily | Implemented (tap-through, Insights, personal-plan CTA) |
| Web denser history | Implemented (progress panel + existing history pages) |
| Error / empty / partial-data UX | Implemented on touched V2 surfaces |
| Privacy / safety | Preserve; no coach broadening |

---

## 4. Dependencies

```
V2.1 Plan (this document)
    → V2.2 Recovery polish only if a real client gap remains (do not add metrics)
    → V2.3 Readiness explanation + insufficient-data honesty (clients consume existing API)
    → V2.4 Recommendation presentation polish (labels already exist)
    → V2.5 History summaries (compose existing overview/load/performance)
    → V2.6 Progress surface (same APIs; refuse charts on insufficient data)
    → V2.7 Independent athlete: smallest mobile create/activate path using existing plan APIs
    → V2.8 QA / security / Sonar / docs
```

No architecture contradiction blocks implementation. The generation chain and facade read-only rules stay.

---

## 5. Expected schema / API changes

**No new contract fields or Flyway versions for V2.2–V2.6.** Today already *declares* `dataSufficiency`, `limitingDimensions`, `adjustmentTypes`, action flags, and `*Present` booleans. The V2.3 backend correction is to populate those existing arrays from stored children (same reads as recovery overview). Today query budget rises from 15 to 17 prepared statements on a populated day.

Possible later (only if V2.7 cannot use current plan/day/occurrence APIs):

- No new intelligence tables.
- No auto-generate on dashboard GET.
- If a starter-workout convenience endpoint is proposed, it must be an **explicit POST** and an approved contract change — prefer composing existing `POST /plans`, days, exercises, occurrences, `schedule/activate`.

---

## 6. Implementation phases

### V2.1 — Gap analysis & contracts (this document)

Done when this plan matches repository evidence.

### V2.2 — Recovery & athlete state

- Keep the existing signal set. Do not add energy/hydration.
- Ensure check-in create/update, history, and explicit generate actions remain the daily input.
- Improve copy that conflates “no check-in” with “state not generated”.

### V2.3 — Readiness

- Populate today-facade `limitingDimensions` (and recommendation `adjustmentTypes`) from stored children. Keep GET read-only.
- Presentation-only mapper from stored band / sufficiency / limiting dimensions to athlete language.
- Same mapper on web and mobile (duplicated at the client layer; no shared package exists).
- Never compute a numeric score on the client.
- Insufficient data: say so; do not fake 0.
- Mobile: tap Home readiness/guidance cards through to existing detail routes.

### V2.4 — Recommendation

- Keep existing actions/adjustments.
- Ensure Home/recovery show action + generate CTA using `canGenerate*` flags.
- Human-readable recommendation explanations instead of raw `explanationKey`.
- No client-side advice.

### V2.5 — Training history & effort

- Athlete-oriented recent-session summary from training-overview / today load.
- Preserve RPE null ≠ 0.
- Mobile remains the execution + RPE surface.

### V2.6 — Progress

- Compose consistency, completion, load/RPE trends, PRs, recovery/readiness history **only when counts suffice**.
- Empty: “More training history is needed” — not a fake chart.

### V2.7 — Independent athlete

- Smallest coherent path: mobile can create a personal plan, add at least one day/exercises, activate/generate or create an occurrence, then execute (existing flow).
- Do not build coach programming.
- Web planner remains the dense authoring surface.

### V2.8 — Hardening

- Regression, authorization (athlete-only wellness/readiness), concurrency on check-in/effort/generate, a11y, query budgets, docs sync.

---

## 7. Test strategy

- **Backend:** no new calculation path unless a proven defect. Existing acceptance tests remain the score/band/lifecycle source of truth. Add tests only if a contract field is added.
- **Web:** dashboard empty-state matrix; readiness explanation from fixture dimensions; progress empty vs sufficient; no score invention.
- **Mobile:** same explanation/empty-state matrix; independent create-plan happy path + unauthorized/conflict; RPE/history navigation.
- **E2E (canonical V1 sequence, not a new invented pipeline):** auth → dashboard → check-in → explicit state → readiness → recommendation → start workout → log → complete → RPE → history/progress reflects session.

---

## 8. Risks

| Risk | Mitigation |
| --- | --- |
| Rewriting V1 home/recovery | Extend cards and labels; do not replace State Engine or facades |
| Adding unused wellness fields | Energy/hydration stay deferred |
| Hidden writes to “help” first-day athletes | Keep explicit generate; improve CTAs only |
| Second readiness mapper that becomes a calculator | Labels/explanations only; score/band come from API |
| Mobile planner scope creep | One personal plan + one day + activate/occurrence; not web PlanBuilder |
| Coach data leakage | No new coach endpoints; keep athlete-scoped repositories |
| Misleading charts | Minimum observation counts before trend UI |
| Working-tree / multi-agent | Do not discard unrelated files; no `reset --hard` |

---

## 9. V2 exit criteria (from product prompt)

- Independent athlete can get value without a coach/team.
- Recovery input remains clear, validated, persisted, historical.
- State Engine remains the only derived-state owner.
- Readiness is explainable, consistent, and honest about missing data.
- Recommendation follows the existing lifecycle.
- Workouts can be completed and effort recorded.
- History and useful progress are visible.
- Mobile daily experience is the primary path; web holds denser history/profile/planning.
- No critical/high defects; authorization review; Sonar-clean touched paths; gates green; docs match behavior.

V2 is **not** complete when this plan is written. It is complete when the exit criteria are met and independently reviewed.

---

## 10. First implementation slice (after this plan)

1. Client readiness explanation + distinct Home empty states (V2.3) **and** fill today-facade stored dimensions — done.
2. Mobile Home tap-through, Insights pipeline on incomplete days, athlete-readable `explanationKey` copy — done.
3. Progress composition on Performance (web + mobile) from existing APIs — done.
4. Minimal mobile personal-plan create/activate — done.
5. Launch-context empty `adjustmentTypes` documented as intentional — done.
6. Sonar/CI workflow added (`.github/workflows/verify.yml` + `sonar-project.properties`). Authenticated quality-gate result still requires GitHub `SONAR_ORGANIZATION`, `SONAR_PROJECT_KEY`, and secret `SONAR_TOKEN`.
7. Screen-level coverage added for readiness, guidance, history, progress, and personal-plan.
8. V2.8 hardening and live web UX verification remain before declaring V2 complete.
9. Security residual: personal-plan create is four explicit POSTs. A mid-flight failure can leave a draft plan; do not add a compose endpoint in V2. Empty catalog now fails closed before any write. Sonar CI compiles Java binaries without re-running tests.

No push, merge, deploy, or tag without explicit user instruction.
