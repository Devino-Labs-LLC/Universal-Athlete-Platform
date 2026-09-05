# Athlete Readiness V3 — Implementation Plan

**Theme:** Coaches, Teams & Schools  
**Product question:** Can coaches, teams, and schools organize athletes and coaching staff while the athlete remains the subject of their own readiness/state data — receiving only explicitly authorized organizational views and capabilities?

**Baseline (canonical):**

| Item | Value |
| --- | --- |
| `main` / `develop` SHA (planning start) | `cb4113679736ad09dd494ef8279c835038fd2db2` |
| Athlete Readiness V2 | **COMPLETE** |
| Sonar project version / V2 baseline | **2.0.0** — [`docs/quality/SONAR_V2_BASELINE.md`](quality/SONAR_V2_BASELINE.md) |
| V2 Quality Gate | **PASSED** |

**Authoritative sources used**

| Precedence | Finding |
| --- | --- |
| ADRs | **None present** in the repository (`docs/adr/` absent). External reference to **ADR-029** is not on disk; V3 must formalize it (see §15). |
| Contracts | `docs/TRAINING_API_V1.md`, `docs/TRAINING_CLIENT_HANDOFF_V1.md`, `docs/V2_IMPLEMENTATION_PLAN.md` |
| Implementation | Modulith modules `identity`, `athlete`, `training` under `backend/uap-server` |
| Migrations | Flyway through athlete/training tables (`V1`–`V29` lineage); next V3 migrations continue the sequence |
| Clients | `apps/web`, `apps/mobile` — athlete-only IA |
| Quality | `docs/agents/security-code-quality.md`, V2 Sonar baseline |
| Figma / ClickUp | Not inspected for this plan; extend V1 visual language; do not invent a parallel design system |

Internal names remain Universal Athlete Platform / UAP. Commercial name is Athlete Readiness (Devino Labs LLC). No repository-wide rename.

**Planning mode only.** This document does **not** authorize Slice A implementation, runtime code, Flyway, API shipping, PR, merge to `main`, deploy, tag, publish, visibility changes, or Sonar admin changes.

---

## 0. Agents consulted

Lead / Architect coordinated planning and explicitly consulted:

| Role | Contribution |
| --- | --- |
| Lead / Architect | Bounded contexts, aggregates, ADR set, slice order, PO questions |
| Backend | Module/API/schema constraints from current Modulith + ownership patterns |
| Web | Sibling coach IA; preserve athlete `/app` shell |
| Mobile | Athlete-first V3; coach mobile deferred unless PO expands |
| Athlete Intelligence / Data | State Engine ownership; team readiness from stored consented readiness; no second calculator |
| QA / Test Automation | AuthZ matrix, IDOR, invite/consent/concurrency, Slice Done red gates |
| Security / Code Quality (Quality Gate Steward) | Threat model, consent/authZ requirements, campground, promotion blockers |
| DevOps / CI-CD | Verify shard inclusion for new packages; do not weaken Sonar gates |
| Documentation / Release | This plan + future ADR landing; honest baselines |
| External Integration | **Consulted only for seams** — V5 wearables out of scope; no V3 provider work |

---

## 1. V3 product objective

Introduce **Coaches, Teams & Schools** while preserving Athlete Readiness as an **athlete-centered** product.

### Core principle

> The athlete remains the subject of their own readiness/state data. Organizations receive **explicitly authorized** views and capabilities.

V3 must **not** turn the athlete into a passive data object owned by an organization.

### In scope

- Coaches managing athletes they are authorized to coach
- Teams organizing athletes and coaching staff
- Schools/organizations managing multiple teams and roles
- Athletes joining/leaving organizations under policy
- Invitations and membership lifecycle
- Coach/team dashboards (web-primary)
- Roster management
- Aggregated team readiness (consent-aware, non-misleading)
- Athlete-level coach views when consent/authority permits
- Planning/training workflows involving coaches **without** bypassing State Engine / readiness / recommendation / safety constraints
- Organization-scoped authorization
- Auditability for sensitive actions

### Non-negotiable preserved semantics

- GET `/client/bootstrap`, `/today`, `/training-overview`, `/recovery-overview`, and launch-context remain **read-only** (no hidden writes).
- Generation chain remains explicit: check-in → `POST /athlete-state/daily/{date}` → `POST /readiness/daily/{date}` → `POST /recommendations/daily/{date}`.
- Server-side authorization is the security boundary; UI visibility is never authorization.
- Foreign / unauthorized resource IDs continue to return **404** (no existence leak) unless a later ADR explicitly changes that posture.
- Athlete V1 frozen surfaces change only for defects, security/quality, or approved V3 work that touches them.
- Purple / AI accent tokens remain unused.

---

## 2. Current-state findings (repository reality)

### What exists

| Area | Reality |
| --- | --- |
| Modules | Spring Modulith: `identity`, `athlete`, `training` only |
| AuthN | Cookie JWT → `AccountPrincipal`; claim set is account-scoped; Spring authority is effectively `ROLE_USER` for authenticated callers |
| Athlete | 1:1 with account (`uk_athletes_account_id`); no FK to `accounts` (cross-context UUID) |
| AuthZ | Account → owned athlete → `findByIdAndAthleteId` (and siblings) → **404** for foreign IDs |
| State Engine | `DailyAthleteState*` under `training` (explicit POST generate/regenerate) |
| Readiness / recommendations | Athlete + date scoped; algorithm versions `READINESS_V1`, `TRAINING_RECOMMENDATION_V1` |
| Plans | Athlete-owned `TrainingPlan` → days → occurrences → execution |
| “Planning Orchestrator” | **Doc label only** (V2 plan); no class/module by that name — planning = training plan/schedule/occurrence/adaptation use cases |
| Coach mentions | Enums/notes only (`MeasurementSource.COACH`, `COACH_DIRECTIVE`, `coachingNotes`) — **not** a role or API |
| Clients | Athlete-only web `/app/*` and mobile `(tabs)`; no org switcher, roster, consent, or coach shell |
| Shared packages | `packages/` empty; web/mobile schemas duplicated |
| Security audit / domain event bus | **Absent** (training revision/history tables are not actor/security audit) |
| Consent / invitation / org / team / role tables | **Absent** |

### What V2 explicitly deferred (now V3)

- Coach programming / coach endpoints
- Organization / team broadening
- Consent as a product surface

### Historical quality debt relevant to V3 (campground)

From [`docs/quality/SONAR_V2_BASELINE.md`](quality/SONAR_V2_BASELINE.md):

- `java:S4502` / `java:S3330` — `IdentitySecurityConfiguration` (CSRF / cookie HttpOnly) — review when V3 touches auth/invite transport
- `java:S2583` — JPA / adaptation paths — fix nearby when touched
- Overall Security **D** / Reliability **C** — visible debt, not “acceptable quality,” not automatic V3 blockers outside campground
- Client contract duplication — reduce only in touched V3 contracts

---

## 3. Architectural decisions (planned)

### 3.1 Athlete agency

Organizations never become the source of truth for athlete readiness/state. Membership grants **organizational capability**; sensitive wellness/readiness access requires **consent scopes** (and can be revoked).

### 3.2 Bounded contexts (avoid org god-module)

| Concern | Owner | Notes |
| --- | --- | --- |
| Account, credentials, sessions, JWT | **`identity`** | Extend carefully; AuthN only |
| Athlete profile, sports, goals, measurements | **`athlete`** | Person-as-athlete; no org graph |
| Plans, schedule, execution, State Engine, readiness, recommendations, adaptation | **`training`** | Add **ports** for membership/consent checks; do not import org JPA entities |
| Organization, Team, Membership, Invitation, contextual Role | **NEW `organization`** | Structure + access graph |
| Consent / sharing grants, scopes, revocation | **NEW `consent`** | Separate lifecycle/threat model from roster |
| Security audit trail | **`AuditPort`** (thin cross-cutting named interface; persistence under `organization` or tiny `audit`) | Append-only; no business rules |

**Modulith dependency sketch:**

```
identity
athlete        → identity :: auth
organization   → identity :: auth
consent        → identity :: auth, athlete :: context, organization :: membership
training       → identity :: auth, athlete :: context,
                 organization :: membership (port), consent :: grants (port)
```

Publish named interfaces early: `organization :: membership`, `consent :: grants`. No shared JPA entities across modules.

### 3.3 Multi-persona Account

**Decision (pending PO confirmation — recommended default):** one `Account`, multiple personas via membership.

| Rule | Recommendation |
| --- | --- |
| Login | Remains `Account` + cookie JWT |
| Athlete | At most one Athlete per Account (existing uniqueness) |
| Coach | Membership role on Org/Team — **not** a parallel 1:1 Coach aggregate by default |
| Athlete + coach same person | Allowed |
| Spring `ROLE_COACH` | Do **not** treat as sufficient authorization; every sensitive call re-checks membership + consent |
| Separate coach accounts | Reject for V3 unless PO requires hard separation |

### 3.4 State Engine vs Planning (formalize missing ADR-029)

Required model remains:

```
Knowledge → State → Planner → Recommendation → Execution → Evaluation → Learning
```

| Lane | Owner | Coach may |
| --- | --- | --- |
| **State Engine** | `training` / Athlete Intelligence (`DailyAthleteState*`) | **Read** consented derived outputs only; never inject fake check-ins or overwrite scores |
| **Planning** | `training` plan/schedule/occurrence/adaptation use cases (“Planning Orchestrator” = logical label, **not** a new Modulith module in V3) | **Write** when membership + consent allow |
| **Recommendation** | `training` calculators | Consumes planned work + state; coach does not replace calculator |
| **Team readiness** | Aggregation service reading **stored** consented readiness | Never a second readiness engine inside `organization` |

### 3.5 Coach-authored inputs — classification

| Input | Classification | Notes |
| --- | --- | --- |
| Team practice / facility constraint | Knowledge / constraint | Feeds planning feasibility; not State Engine |
| Coach session assignment | Assignment → plan/occurrence write | Explicit mutation; audited |
| Competition calendar | Knowledge / constraint | Team-scoped calendar entity or plan metadata — decide in Slice F ADR detail |
| Athlete availability | Knowledge (athlete- or coach-reported) | Consent may apply if shared |
| Coach “recommend rest” note | Recommendation overlay / communication | Must not silently mutate State Engine |
| Completed team session | Execution evidence | Via existing occurrence completion paths when authorized |

Preserve distinctions: **state ≠ recommendation ≠ plan ≠ assignment ≠ execution**.

### 3.6 TrainingPlan ownership under coaching

**Recommended default (needs PO confirmation):** plans remain **athlete-owned**; coach is an authorized **collaborator** (writes audited). Alternative (coach-owned assignment trees) is higher complexity and deferred unless PO requires it.

---

## 4. Domain model

### 4.1 Canonical concepts

| Concept | Definition | Aggregate |
| --- | --- | --- |
| **Organization** (School / club / multi-team body) | Top-level tenant for teams and staff | `Organization` |
| **Team** | Roster + coaching staff under one organization | `Team` |
| **Organization membership** | Account ↔ Organization with org-level role | `Membership` (org-scoped) |
| **Team membership** | Account ↔ Team with team-level role; athletes linked via athleteId when applicable | `Membership` (team-scoped) |
| **Role / authority** | Enum on membership (not free-form JWT role soup) | Part of Membership |
| **Invitation** | Pending offer to join org/team with proposed role | `Invitation` |
| **Consent / sharing grant** | Athlete-granted, purpose-scoped visibility to a grantee (team/org membership) | `ConsentGrant` |
| **Roster assignment** | Effective team athlete membership in `ACTIVE` status | Derived from Membership |
| **Team season / lifecycle** | Optional soft lifecycle (`ACTIVE` / `ARCHIVED`) on Team; full “season” entity **deferred** unless PO requires | Team status fields first |
| **Coach-to-athlete relationship** | Emergent from team membership + role + consent — **not** a separate unrestricted edge by default | — |
| **Organization ownership/admin** | `OWNER` / `ORG_ADMIN` membership roles | Membership |

### 4.2 Lifecycle (useful state machines)

**Invitation:** `PENDING` → `ACCEPTED` \| `DECLINED` \| `REVOKED` \| `EXPIRED` (optional `SUPERSEDED` if re-invite replaces).

**Membership:** `ACTIVE` → `REMOVED` \| `LEFT` (rejoin requires new invitation; old membership id does not resurrect access).

**ConsentGrant:** `ACTIVE` → `REVOKED` (re-grant creates a **new** grant version/id).

**Organization / Team:** `ACTIVE` → `ARCHIVED` (no hard delete in V3).

### 4.3 IDs and ownership rules

- All primary keys: UUID.
- Cross-module references: UUID value objects only (same pattern as `athlete` ↔ `identity`).
- Team always belongs to exactly one Organization.
- Athlete membership on a team requires an Athlete profile (unless PO allows athlete-less roster placeholders — **not recommended** for V3).
- Coach-only accounts (no Athlete row): **PO question**; technically allowed if membership does not require athleteId.

### 4.4 Transactional boundaries

- Accept invitation → create membership (+ optional default consent prompt) in **one TX**; do **not** also assign training plans in the same TX.
- Revoke membership / consent → status flip + audit in same TX; subsequent reads re-check DB (not JWT alone).
- Coach plan edits use existing optimistic `version` on training aggregates.
- No distributed “invite accept generates readiness” transactions.

---

## 5. Authorization matrix (server-side)

UI visibility is **not** authorization. Deny by default. Inaccessible → **404**.

### 5.1 Roles (V3)

| Role | Scope | Notes |
| --- | --- | --- |
| **Athlete** | Self + team memberships as athlete | Owns wellness data |
| **Coach** | Team | Day-to-day coaching |
| **Head Coach** | Team | Elevated team coaching + limited staff invites |
| **Team Admin** | Team | Roster/settings for one team |
| **Organization Admin** | Organization | Multi-team admin |
| **Organization Owner** | Organization | Highest org authority; transfer ownership is sensitive |
| **Staff / read-only** | Team or Org | Optional; include if PO wants observers without invite powers |

Exact role enum names to be locked in ADR + Flyway check constraints.

### 5.2 Capability matrix

Legend: **Y** = allowed when membership active; **C** = allowed only with required consent scope(s); **—** = not allowed; **Own** = athlete-self only.

| Capability | Athlete | Coach | Head Coach | Team Admin | Org Admin | Org Owner | Staff RO |
| --- | --- | --- | --- | --- | --- | --- | --- |
| View roster (identity) | Own team Y* | Y | Y | Y | Y | Y | Y |
| Invite athlete | — | Y† | Y | Y | Y | Y | — |
| Remove athlete | Leave self | Y† | Y | Y | Y | Y | — |
| Invite coach/staff | — | — | Y† | Y | Y | Y | — |
| Manage roles | — | — | Limited† | Y | Y | Y | — |
| View team readiness aggregates | — | C | C | C | C | C | C |
| View athlete-level readiness | Own | C | C | C | C | C | C |
| View recovery/check-in detail | Own | C | C | C‡ | C‡ | C‡ | C‡ |
| View training history | Own | C | C | C | C | C | C |
| Assign / recommend training | — | C+W | C+W | —§ | —§ | —§ | — |
| Modify athlete-owned plans | Own | C+W | C+W | — | — | — | — |
| Create team sessions | — | C+W | C+W | —§ | — | — | — |
| Export data | Own | C+E | C+E | C+E | C+E | C+E | — |
| View audit history | Own limited¶ | — | — | Team¶ | Org¶ | Org¶ | — |
| Manage org/team configuration | — | — | Team limited | Team | Org | Org | — |

\* Athlete sees teammates’ **roster identity** only if product allows peer visibility — **default: no peer wellness; teammate identity PO decision**.  
† Subject to org policy flags if introduced.  
‡ Prefer least privilege: admins do **not** automatically get recovery detail without consent.  
§ Admins configure; coaching writes stay with coaching roles unless PO merges roles.  
¶ Audit visibility for sensitive events; athletes see grants/revokes affecting them.  
**C+W** = consent scopes covering training collaboration write. **C+E** = export purpose scope.

### 5.3 Anti-patterns to prevent

IDOR; cross-team / cross-org access; role escalation via client body; invitation hijacking; stale authZ after membership/consent removal; unauthorized wellness access; roster enumeration oracles; CSRF disabled “for invites.”

---

## 6. Consent and athlete data sharing

### 6.1 First-class design rules

- Joining a team grants **membership**, not unlimited wellness visibility.
- Sensitive categories require **explicit consent grants** with purpose/scope.
- Revocation is immediate on the authorization path (DB/versioned cache), not “when cookie expires.”
- Coach cannot re-grant unilaterally after athlete revoke.
- Do **not** invent FERPA/COPPA/HIPAA compliance claims. Record legal/product review needs.

### 6.2 Proposed sharing scopes (least privilege)

| Scope | Example contents | Default on join |
| --- | --- | --- |
| `ROSTER_IDENTITY` | Display name, sport labels needed for roster | Configurable; often yes for active roster |
| `AVAILABILITY` | Practice availability flags | Explicit |
| `READINESS_CATEGORY` | Band only (`HIGH`/`MODERATE`/`LOW`/`INSUFFICIENT_DATA`) | Explicit |
| `READINESS_SCORE` | Numeric score + limiting dimensions summary | Explicit (stricter than category) |
| `LIMITING_DIMENSIONS` | Dimension detail | Explicit |
| `RECOVERY_CHECK_IN_DETAIL` | Raw check-in fields/notes | Explicit; highest sensitivity |
| `TRAINING_ADHERENCE` | Completion / skip rates | Explicit |
| `PERFORMANCE_HISTORY` | PRs / load history | Explicit |
| `TRAINING_COLLABORATION` | Coach may edit/assign plans & sessions | Explicit |
| `EXPORT` | Bulk export inclusion | Explicit; never implied |

### 6.3 Historical data after revoke / removal

**Recommended default (PO confirmation required):**

- After revoke: coach loses **new reads** of consented categories immediately.
- Organization may retain **non-sensitive membership audit** (joined/left timestamps, role history).
- Retaining historical wellness snapshots for org analytics after revoke: **default no** for V3; if needed, separate legal/product review + ADR.
- Athlete retains full self-history regardless of org membership.

### 6.4 Minors / schools

Possible FERPA/COPPA (and similar) relevance for school deployments is a **legal/product review requirement**, not a V3 engineering compliance assertion. Parent/guardian consent flows are **out of V3** unless PO explicitly pulls them in.

---

## 7. Team readiness semantics

### 7.1 Principles

- Avoid inventing misleading science.
- Do **not** invent a single team “score” unless the domain can justify it. **V3 default: no composite team score.**
- Aggregation uses **stored** athlete readiness assessments already generated by athletes (or their explicit generation chain) — GET team readiness does **not** generate missing athlete readiness.
- Respect consent scopes and **minimum cohort** privacy rules.

### 7.2 V3 recommended exposures

| Metric | V3? | Notes |
| --- | --- | --- |
| Counts by readiness band | **Yes** | Among consented + eligible members |
| Number ready / limited / unavailable / insufficient | **Yes** | Map bands + missing consent/data honestly |
| Limiting-dimension distribution | **Yes** (optional) | Only with `LIMITING_DIMENSIONS` or aggregate-safe derivative |
| Participation / availability summary | **Yes** if availability scope exists | |
| Mean/median numeric score | **Optional** | Prefer secondary; suppress when N &lt; threshold |
| Training-load team summaries | **Later / limited** | Prefer Slice F+ after training collaboration settles |
| Trends over time | **Optional** | Requires stable history + consent continuity rules |
| Single team score | **No (default)** | Unless PO + Athlete Intelligence ADR justifies |

### 7.3 Insufficient-data / privacy behavior

- If consented sample &lt; `minCohortSize` (proposed default **3**, PO-configurable): return `INSUFFICIENT_DATA` / suppressed cells — **not** a fake 0.
- Exclude: non-members, removed members, missing consent, missing assessment for the date, wrong team.
- Drill-down that collapses to one athlete requires that athlete’s consent — never via aggregate loophole.

---

## 8. Coach workflows

### 8.1 Coach onboarding

`Account` → receive org/team invitation → accept → membership `ACTIVE` → roster access (identity) → optional consent waits for athletes.

### 8.2 Athlete invitation

Coach/admin invites → athlete accepts → membership established → **consent decision** (explicit UI) → roster active → coach sees only granted scopes.

### 8.3 Daily team review

Open team → team readiness summary (consent-aware) → identify limited/unavailable/insufficient → drill into authorized athlete views → no generation on page load.

### 8.4 Training planning

Coaches interact with existing planning lane under `training`:

| Action | Allowed if | Constraint |
| --- | --- | --- |
| Assign a workout / create occurrence | Membership + `TRAINING_COLLABORATION` | Explicit POST; athlete-owned plan preferred |
| Recommend a session | Same | Must not bypass recommendation calculator as source of readiness truth |
| Create a template / team session definition | Coaching roles | Team-scoped template entity or shared plan pattern — detail in Slice F |
| Modify athlete-generated work | Same + optimistic lock | Audited collaborator write |
| Schedule team sessions | Same | Feasibility/environment rules still apply |

Coaches must **not** bypass: State Engine, readiness, recommendation, planning authority, or safety constraints.

---

## 9. API contract proposal

Keep APIs under **`/api/v1`**. GET remains read-only. No hidden generation/mutation from page loads.

Exact paths are proposals; Lead/Backend lock names in slice contracts.

### 9.1 Candidate resources

| Area | Examples |
| --- | --- |
| Organizations | `POST/GET/PATCH /api/v1/organizations`, `GET /api/v1/organizations/{organizationId}` |
| Teams | `POST/GET/PATCH /api/v1/organizations/{organizationId}/teams`, `GET /api/v1/teams/{teamId}` |
| Memberships | `GET /api/v1/teams/{teamId}/memberships`, `DELETE` (remove/leave) |
| Invitations | `POST /api/v1/teams/{teamId}/invitations`, `POST /api/v1/invitations/{token}/accept`, decline/revoke |
| Roster | `GET /api/v1/teams/{teamId}/roster` |
| Consent | `GET/POST /api/v1/athletes/me/consents`, `POST .../revoke` |
| Team readiness | `GET /api/v1/teams/{teamId}/readiness?date=` |
| Coach athlete view | `GET /api/v1/teams/{teamId}/athletes/{athleteId}/overview` (projection by consent) |
| Coach training | Extends `/api/v1/training/...` with team context **or** `/api/v1/teams/{teamId}/training/...` façades that delegate to training module |

### 9.2 Endpoint design checklist (every endpoint)

For each shipped endpoint document: method; route; actor; authorization requirement; request; response; transaction behavior; idempotency; audit event; error cases (`401`, `404`, `409`, validation).

### 9.3 Idempotency highlights

| Action | Behavior |
| --- | --- |
| Accept invitation (retry) | Idempotent success **or** `409` — pick one in ADR; test both double-submit and delayed retry |
| Create invitation | Dedupe pending invite per (team, email/account, role) |
| Revoke consent | Idempotent if already revoked |
| Coach plan PATCH | Optimistic `expectedVersion` / `version` as today |

### 9.4 Athlete self APIs

Existing `/api/v1/athletes/me/**` and `/api/v1/training/**` remain for athlete-self. Coach paths are **additional**, never “pass any athleteId on /me.”

---

## 10. Database proposal (Flyway)

Non-destructive. Archive over delete. Continue Flyway sequence after current max.

### 10.1 Proposed tables (illustrative)

| Table | Keys / uniqueness | Notes |
| --- | --- | --- |
| `organizations` | PK `id`; status; timestamps; `version` | |
| `teams` | PK `id`; FK `organization_id`; unique `(organization_id, slug)` or name-per-org | status |
| `organization_memberships` | unique `(organization_id, account_id)` where active | role, status, timestamps |
| `team_memberships` | unique `(team_id, account_id)` where active; optional `athlete_id` | role, status |
| `invitations` | PK `id`; `token_hash` unique; team/org target; role; email/account bind; expires_at; status | raw token never stored |
| `consent_grants` | PK `id`; athlete_id; grantee membership/team; scope set; status; version/revoked_at | |
| `security_audit_events` | PK `id`; append-only; actor_account_id; action; subject refs; correlation_id; created_at | no wellness payloads |
| Optional `team_session_templates` | Slice F | Only if product needs team templates distinct from athlete plans |

Indexes: foreign keys; `(token_hash)`; `(team_id, status)` for roster; `(athlete_id, status)` for consents; audit `(organization_id, created_at)`.

### 10.2 Concurrency / integrity

- Unique active membership prevents duplicate joins.
- Invitation accept uses transactional conditional update (`PENDING` → `ACCEPTED`).
- Role changes validate allow-list transitions.
- Team/org archive blocks new invites; existing memberships become read-only per policy.

---

## 11. Web experience

Web is the **primary** surface for richer coach/admin workflows.

### 11.1 Information architecture

Keep athlete `/app/*` + `AppShell`/`Sidebar` intact. Add a **sibling** coach/admin tree, e.g. `/coach/*` or `/org/*`, with a **separate shell** (org/team switcher, coach nav).

Proposed areas:

- Organization switcher (if multi-org)
- Team directory
- Roster
- Athlete directory (org-scoped, authorized)
- Team readiness dashboard
- Athlete detail (consent-aware)
- Invitations
- Team settings / staff & roles
- Athlete-side: pending invites + consent manager (can live under athlete profile **or** light `/app` entries — do not convert Home into admin)

### 11.2 Explicit non-goals for web V3

- Do not turn athlete Home into a generic admin dashboard.
- Do not use purple/AI tokens.
- Do not require a shared `packages/` extraction to start (evaluate duplication when adding schemas — campground in touched contracts).

---

## 12. Mobile experience

Athlete mobile remains first-class.

### 12.1 V3 mobile in scope (recommended)

- View/accept/decline team invitations
- Consent / sharing controls
- See which coaches/teams have which scopes
- Team context badges where helpful
- Team-assigned sessions **appearing in athlete training** when assigned (consume training APIs)

### 12.2 Web-only initially (recommended)

- Full coach roster management
- Org/team admin settings
- Team readiness dashboards
- Dense staff/role management
- Bulk export

### 12.3 Coach mobile

**Deferred by default** for V3 (PO may expand). If added, use a separate Expo route group — do not overload athlete `(tabs)`.

---

## 13. Events and audit

No event bus exists today. V3 introduces **server-written audit records** first; optional domain events can mirror audit for in-process listeners later.

### 13.1 Candidate event names

Align with SCREAMING_SNAKE / past-tense domain style already used for generation reasons:

| Name | Typical class |
| --- | --- |
| `ORGANIZATION_CREATED` | Audit + domain |
| `TEAM_CREATED` | Audit + domain |
| `ATHLETE_INVITED` | Audit |
| `MEMBERSHIP_ACCEPTED` | Audit + domain |
| `MEMBERSHIP_REVOKED` | Audit + domain |
| `ROLE_CHANGED` | Audit |
| `CONSENT_GRANTED` | Audit + domain |
| `CONSENT_REVOKED` | Audit + domain |
| `WORKOUT_ASSIGNED` | Audit + domain |

### 13.2 Do not conflate

| Concern | Purpose |
| --- | --- |
| Transactional domain events | In-process reactions (invalidate caches, notify) |
| Audit events | Security/compliance trail; append-only; tamper-evident expectations |
| Analytics events | Product metrics; may be sampled; not authZ source of truth |

Training revision tables remain **clinical/history of content**, not security audit.

---

## 14. Security / threat model (Quality Gate Steward)

Full steward requirements: New Code Reliability/Security/Maintainability **A**; coverage **≥ 80%**; duplication **≤ 3%**; hotspots **100%**; Quality Gate **PASSED**. No gate gaming (`NOSONAR`, broad exclusions, fake coverage, deleted tests).

### 14.1 Threats (design obligations)

| ID | Threat | Mitigation direction |
| --- | --- | --- |
| T1 | Cross-org / cross-team access | Tenant binding; deny default; 404 |
| T2 | Coach impersonation / confused deputy | No silent act-as-athlete; membership+consent; audit |
| T3 | Invitation token abuse | CSPRNG; hash-at-rest; TTL; single-use; rate limit; no raw token logs |
| T4 | Membership escalation | Server state machine; unique constraints |
| T5 | Consent bypass | Purpose-scoped checks on every sensitive read/export |
| T6 | Wellness leakage | Field-level least privilege; redact logs |
| T7 | Roster enumeration | Uniform not-found; scoped lists; rate limits |
| T8 | Stale authorization | Re-check membership/consent each sensitive request |
| T9 | Audit tampering | Append-only server audit; no client-writable audit fields |
| T10 | Export / download risks | Explicit export authZ + consent; audit; ACL re-check |
| T11 | Aggregate re-identification | Min cohort; suppress small cells |

### 14.2 Campground

When V3 touches `IdentitySecurityConfiguration` or `S2583` JPA/adaptation paths, leave them cleaner than found. Do not mass-refactor unrelated debt in planning or unrelated slices.

### 14.3 Promotion blockers (steward)

Quality Gate fail; New Code Blocker/Critical sec/reliability; client-only authZ; existence oracles; consent gaps; weak invites; stale post-revoke access; CSRF/cookie regressions; gate gaming; Verify red on touched surfaces; hidden writes on reads; unreviewed hotspots.

---

## 15. ADRs required (non-trivial)

Create `docs/adr/` when writing these (Slice A documentation gate). Do **not** create ADRs for trivial naming.

| ADR | Decision to lock |
| --- | --- |
| **ADR-029 — State Engine vs Planning ownership** | Formalize missing external ADR: State Engine = `DailyAthleteState*` + readiness/recommendation chain in `training`; Planning = plan/schedule/occurrence/adaptation in `training`; “Planning Orchestrator” = logical label, not a new module in V3; coach writes planning only |
| **Multi-persona Account** | One Account; Athlete 0..1; coach via membership |
| **Organization + Consent module split** | New `organization` + `consent`; dependency/ports |
| **Authorization & IDOR posture** | Matrix; membership + consent; preserve 404-for-inaccessible |
| **Consent / sharing model** | Scopes; revoke semantics; defaults on join |
| **TrainingPlan ownership under coaching** | Athlete-owned + collaborator (recommended) vs coach-owned trees |
| **Audit** | Mandatory events; append-only; athlete transparency level |

Defer ADRs: billing (V4), wearables (V5), AI coach (V6), marketplace (V7), parent/guardian (unless pulled into V3).

---

## 16. Testing strategy (QA)

### 16.1 Principles

Server is the boundary; 404 not 403 for foreign resources; consent additive; no hidden writes; Slice Done ≠ happy-path demo.

### 16.2 Required suites (grow per slice)

- Authorization matrix (table-driven)
- Org/team IDOR isolation
- Invitation lifecycle + duplicate accept concurrency
- Consent grant/revoke/re-grant + immediate fail-closed
- Membership removal / stale access
- Team readiness honesty (insufficient-data, min-n, no hidden generate)
- Web Vitest RC-style coach/athlete flows; mobile Jest for invite/consent
- HTTP multi-actor “E2E” chains (browser E2E only if harness later approved)

### 16.3 CI / Verify

New `organization` / `consent` packages **must** be included in Verify Gradle shards (today `core` runs `identity` + `athlete` + `training.domain`). Orphaned tests are a Slice A Done failure.

### 16.4 Quality Gate Steward

Independent review after each substantial slice and before `develop` → `main`. Compare Overall trends to V2 baseline; campground when applicable.

---

## 17. Implementation slices

Prefer small **vertical** slices. Refined order puts **consent before wellness reads** and **coach planning writes before team aggregates that depend on stable collaboration** — team readiness still must not run before consent.

| Slice | Product outcome | Backend | Web | Mobile | DB | Tests / security / DoD |
| --- | --- | --- | --- | --- | --- | --- |
| **A — Foundation** | Org & Team exist; ADRs landed | Modulith `organization` (+ `consent` skeleton); CRUD org/team; ports sketched | Hidden/feature-flagged routes OK | Athlete app regression only | `organizations`, `teams` | Cross-tenant IDOR; Verify shard wired; QG New Code; **DoD:** no wellness APIs yet; ADRs merged |
| **B — Membership & invitations** | People can join with roles | Membership + invitation lifecycle; `membership` port | Athlete invite accept/decline; admin invite create | Invite accept/decline | memberships, invitations | Lifecycle + concurrency + token abuse; **DoD:** no duplicate memberships; revoke works |
| **C — Consent & sharing** | Athlete controls sharing | `consent` grants/scopes/revoke; `grants` port | Consent manager | Consent toggles | `consent_grants` | Scope matrix; revoke-then-read; **DoD:** no readiness without scope |
| **D — Coach roster & athlete views** | Coach can see authorized roster/detail | Roster + consent-aware projections | Coach shell: roster + athlete detail | Optional read-only team list | indexes as needed | IDOR + field matrix; **DoD:** no non-consented wellness |
| **E — Coach training workflows** | Coach assigns/collaborates on plans/sessions | Training use cases + authZ ports; audit `WORKOUT_ASSIGNED` | Coach planning UX (scoped) | Athlete sees assignments in training | optional templates | AuthZ + optimistic lock; **DoD:** State Engine untouched as write target |
| **F — Team readiness** | Honest team readiness dashboard | Aggregate from stored consented readiness | Team readiness UI | — (web-first) | none or tiny cache table if justified | Min-n / insufficient; no hidden writes; **DoD:** no team mega-score unless ADR |
| **G — UX completion** | Cohesive coach + athlete org UX | API polish only | Full IA, a11y, empty/error | Invite/consent polish | — | RC cache isolation; **DoD:** athlete Home not admin-ized |
| **H — Hardening / release gate** | V3 releasable | Audit completeness; stale authZ battery | Hardening | Hardening | — | Full matrix freeze; Sonar vs baseline; campground closed on touched debt; **DoD:** steward + QA sign-off |

**Do not** start F before C. **Do not** write coach data into State Engine POSTs.

---

## 18. Definition of done (V3)

V3 is complete only when:

1. Athletes can join/leave teams under invitation + membership policy.
2. Coaches/admins can manage roster and roles within authorization matrix.
3. Consent scopes gate athlete-level wellness/readiness/training collaboration.
4. Team readiness is honest, consent-aware, and non-misleading (no unjustified mega-score).
5. Coach planning writes go through training planning lane with audit; State Engine remains sole derived-state owner.
6. Web coach/admin sibling IA ships; athlete experience remains distinct; mobile athlete invite/consent ships.
7. Security threat mitigations for T1–T11 addressed for shipped surfaces.
8. QA matrix, IDOR, invite, consent, concurrency, stale-access, aggregate tests green.
9. Sonar New Code gates met; Overall trend reviewed vs V2 baseline; Verify green on `develop`; promotion to `main` separately authorized.
10. Docs/ADRs/contracts match behavior; V4–V7 not silently absorbed.

---

## 19. Explicit V3 / V4 / V5 / V6 / V7 boundaries

| Version | Scope | V3 stance |
| --- | --- | --- |
| **V3** | Coaches, Teams & Schools | **This plan** |
| **V4** | Commercial billing / organization subscriptions | Extension seam: `Organization` may later link billing account id — **do not implement** |
| **V5** | Wearables / provider integrations | Extension seam: integrations module remains separate; State Engine consumes mapped domain knowledge only — **do not implement** |
| **V6** | Generative / predictive AI coach | Purple/AI tokens reserved; no AI coach — **do not implement** |
| **V7** | Marketplace / social / ecosystem | **do not implement** |

---

## 20. Unresolved questions (product-owner approval)

1. Confirm **one Account / multi-persona** vs mandatory separate coach accounts.
2. Allow **coach-only** accounts (no Athlete row)?
3. Org model: Org→Teams only, or nested orgs / multi-org coaches in V3?
4. Athlete on **multiple teams** simultaneously?
5. Teammate **peer visibility** of roster identity (not wellness)?
6. **TrainingPlan ownership**: athlete-owned collaborator vs coach-owned assignment trees?
7. Consent **defaults on join** — which scopes auto-granted, if any?
8. After leave/revoke: retain any historical wellness for org, or purge from org views?
9. Invitation: email-bound vs open link; expiry defaults; role immutable at invite?
10. Minors / parent-guardian in **V3** or later (legal review)?
11. May coaches **force** plan override that ignores recommendation, or advisory-only?
12. Team readiness: confirm **no single team score**; choose `minCohortSize` default.
13. **Mobile coach** in V3 or web-only?
14. Must audit events be **athlete-visible** (transparency) in V3?
15. Staff/read-only role in V3 or defer?
16. Team **season** entity now vs archive-only lifecycle?

---

## 21. Risks

| Risk | Mitigation |
| --- | --- |
| Org god-module | Split `organization` vs `consent`; ports into training |
| Consent bolted on late | Slice C before D/F |
| Second readiness engine | Aggregates read stored assessments only |
| JWT role stale privileges | Membership/consent re-check every sensitive call |
| Athlete Home polluted | Sibling coach IA |
| Verify skips new tests | Shard wiring in Slice A DoD |
| Legal overclaim | Review flags only; no compliance assertions |
| Scope creep into V4–V7 | Explicit boundaries §19 |
| Touching CSRF/cookie debt poorly | Campground + steward review |

---

## 22. External Integration note

V3 does not require provider work. If org roster later syncs from SIS/HR systems (post-V3), keep provider DTOs behind integration boundaries — same rule as V5 wearables.

---

## 23. First authorized implementation step (after this plan)

**Not authorized by this document.** When PO/Lead explicitly authorize **Slice A**:

1. Land ADR-029 + multi-persona + module-split ADRs.
2. Create Modulith `organization` (+ `consent` skeleton).
3. Flyway for `organizations` / `teams`.
4. Wire Verify shards.
5. IDOR tests green; no wellness endpoints yet.

Stop here until that authorization is explicit.

---

## 24. Git / safety for this planning task

- Branch: `develop`
- Planning/docs only
- No runtime code, migrations, API implementation, PR, merge to `main`, deploy, tag, publish, visibility, or Sonar admin changes

Suggested commit message for this plan:

`docs: define Athlete Readiness V3 architecture and delivery plan`
