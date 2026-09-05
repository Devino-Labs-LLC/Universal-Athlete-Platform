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
| ADRs | [`docs/adr/`](adr/README.md) — ADR-029–035 **Accepted** (V3 decision lock). ADR-001–028 not present in-repo; numbers reserved/assumed external — do not reuse. |
| Contracts | `docs/TRAINING_API_V1.md`, `docs/TRAINING_CLIENT_HANDOFF_V1.md`, `docs/V2_IMPLEMENTATION_PLAN.md` |
| Implementation | Modulith modules `identity`, `athlete`, `training` under `backend/uap-server` |
| Migrations | Flyway through athlete/training tables (`V1`–`V29` lineage); next V3 migrations continue the sequence |
| Clients | `apps/web`, `apps/mobile` — athlete-only IA |
| Quality | `docs/agents/security-code-quality.md`, V2 Sonar baseline |
| Figma / ClickUp | Not inspected for this plan; extend V1 visual language; do not invent a parallel design system |

Internal names remain Universal Athlete Platform / UAP. Commercial name is Athlete Readiness (Devino Labs LLC). No repository-wide rename.

**Decision lock:** Product Owner decisions in §20 are **approved**. ADRs 029–035 are **Accepted**.

**Still not authorized:** Slice A **runtime** implementation (Flyway, APIs, UI, workflow edits). This document remains planning/governance until Slice A is explicitly authorized.

---

## 0. Agents consulted

Lead / Architect coordinated planning and explicitly consulted:

| Role | Contribution |
| --- | --- |
| Lead / Architect | Bounded contexts, aggregates, ADR set, slice order; PO decision lock |
| Backend | Module/API/schema constraints from current Modulith + ownership patterns |
| Web | Sibling coach IA; preserve athlete `/app` shell |
| Mobile | Athlete-first V3; full coach console web-only (PO locked) |
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

## 3. Architectural decisions (locked)

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

**Decision (PO approved — ADR-030):** one `Account`, multiple personas via membership.

| Rule | Locked decision |
| --- | --- |
| Login | Remains `Account` + cookie JWT |
| Athlete | At most one Athlete per Account (existing uniqueness); optional |
| Coach | Membership role on Org/Team — **not** a parallel 1:1 Coach aggregate |
| Athlete + coach same person | **Allowed** |
| Coach-only accounts | **Allowed** — membership does not require an Athlete row; `athleteId` only where applicable |
| Multi-org / multi-team | One Account may hold memberships across multiple Organizations and multiple Teams |
| Spring `ROLE_COACH` | Do **not** treat as sufficient authorization; every sensitive call re-checks membership + consent |

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

**Decision (PO approved — ADR-034):** `TrainingPlan` remains **athlete-owned**. Coaches are authorized **collaborators / assigners**. Do **not** introduce a competing coach-owned training-plan tree in V3. Assignments never mutate State Engine/readiness; assignment and recommendation may disagree as distinct facts; athlete may decline/unable; conflicts are auditable.

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
| **Team lifecycle** | `ACTIVE` → `ARCHIVED` only in Slice A+; **no Season aggregate** in V3 unless a later training/team workflow proves need | Team status |
| **Coach-to-athlete relationship** | Emergent from team membership + role + consent — **not** a separate unrestricted edge | — |
| **Organization ownership/admin** | `ORG_OWNER` / `ORG_ADMIN` membership roles | Membership |

**Topology (PO locked):** flat Organization → Teams only; no nested organizations.

### 4.2 Lifecycle (locked state machines)

See §20.2 for the normative Pre-Slice-A contract. Summary:

| Aggregate | States |
| --- | --- |
| **Invitation** | `PENDING` → `ACCEPTED` \| `DECLINED` \| `REVOKED` \| `EXPIRED` |
| **Membership** | `ACTIVE` → `REMOVED` \| `LEFT` (rejoin requires new invitation; old membership id does not resurrect access) |
| **ConsentGrant** | `ACTIVE` → `REVOKED` (re-grant = **new** grant id) |
| **Organization / Team** | `ACTIVE` → `ARCHIVED` (no hard delete in V3) |

### 4.3 IDs and ownership rules

- All primary keys: UUID.
- Cross-module references: UUID value objects only (same pattern as `athlete` ↔ `identity`).
- Team always belongs to exactly one Organization.
- Athlete roster membership requires an Athlete profile and carries `athleteId`.
- Coach/admin memberships **do not** require an Athlete row (`athleteId` absent).
- Athletes may belong to **multiple ACTIVE teams** (including across organizations).

### 4.4 Transactional boundaries

- Accept invitation → create membership in **one TX** (no sensitive consent auto-grant; no training assignment in the same TX).
- Revoke membership / consent → status flip + audit in same TX; subsequent reads re-check DB (not JWT alone).
- Coach plan/assignment edits use existing optimistic `version` on training aggregates.
- No distributed “invite accept generates readiness” transactions.

---

## 5. Authorization matrix (server-side)

UI visibility is **not** authorization. Deny by default. Inaccessible → **404**.

### 5.1 Roles (V3 — locked enum)

| Role enum | Scope | Notes |
| --- | --- | --- |
| `ATHLETE` | Team (athlete roster membership) | Owns wellness data; requires `athleteId` |
| `COACH` | Team | Day-to-day coaching |
| `HEAD_COACH` | Team | Elevated team coaching + limited coach/staff invites |
| `TEAM_ADMIN` | Team | Roster/settings for one team |
| `ORG_ADMIN` | Organization | Multi-team admin |
| `ORG_OWNER` | Organization | Highest org authority; transfer ownership is sensitive |

**Deferred:** Staff / read-only role. Do not invent additional roles in Slice A–G without a new ADR.

### 5.2 Capability matrix

Legend: **Y** = allowed when membership active; **C** = allowed only with required consent scope(s); **—** = not allowed; **Own** = athlete-self only.

| Capability | Athlete | Coach | Head Coach | Team Admin | Org Admin | Org Owner |
| --- | --- | --- | --- | --- | --- | --- |
| View roster (roster-safe identity) | Teammates Y* | Y | Y | Y | Y | Y |
| Invite athlete | — | Y† | Y | Y | Y | Y |
| Remove athlete | Leave self | Y† | Y | Y | Y | Y |
| Invite coach | — | — | Y† | Y | Y | Y |
| Manage roles | — | — | Limited† | Y | Y | Y |
| View team readiness aggregates | — | C | C | C | C | C |
| View athlete-level readiness | Own | C | C | C | C | C |
| View recovery/check-in detail | Own | C | C | C‡ | C‡ | C‡ |
| View training history | Own | C | C | C | C | C |
| Assign / create team-session assignments | — | C+W | C+W | —§ | —§ | —§ |
| Modify athlete-owned plans (collaborator) | Own | C+W | C+W | — | — | — |
| Export data | Own | C+E | C+E | C+E | C+E | C+E |
| View athlete transparency / limited audit | Own transparency¶ | — | — | Team admin audit¶ | Org audit¶ | Org audit¶ |
| Manage org/team configuration | — | — | Team limited | Team | Org | Org |

\* Active teammates receive **roster-safe identity only** — not email/contact, wellness, readiness, recovery, or training detail.  
† Subject to org policy flags if introduced later.  
‡ Admins do **not** automatically get recovery detail without consent.  
§ Admins configure; coaching writes stay with coaching roles.  
¶ Athletes see transparency subset (ADR-035); org/team admins see operational audit appropriate to scope — **not** interchangeable with athlete transparency.  
**C+W** = `TRAINING_COLLABORATION` (or equivalent). **C+E** = export purpose scope.

### 5.3 Authorization-denial semantics (locked — ADR-032)

| Situation | Response |
| --- | --- |
| Unauthenticated | **401** |
| Authenticated, inaccessible foreign/tenant/consent-denied resource | **404** (no existence oracle; do not use 403 for foreign resources) |
| Owned-resource conflict / illegal transition / optimistic lock | **409** or validation **4xx** as appropriate |
| Invitation token revoked/expired/wrong-account | Fail closed, non-oracle (prefer 404 or uniform safe code) + rate limits |

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
| Roster-safe identity (membership effect, not a ConsentGrant) | Display name / roster labels needed for roster | **Yes** for ACTIVE membership (peers + authorized staff) |
| `AVAILABILITY` | Practice availability flags | **No** — explicit grant |
| `READINESS_CATEGORY` | Band only | **No** — explicit |
| `READINESS_SCORE` | Numeric score + summary | **No** — explicit |
| `LIMITING_DIMENSIONS` | Dimension detail | **No** — explicit |
| `RECOVERY_CHECK_IN_DETAIL` | Raw check-in fields/notes | **No** — explicit |
| `TRAINING_ADHERENCE` | Completion / skip rates | **No** — explicit |
| `PERFORMANCE_HISTORY` | PRs / load history | **No** — explicit |
| `TRAINING_COLLABORATION` | Coach may edit/assign plans & sessions | **No** — explicit |
| `EXPORT` | Bulk export inclusion | **No** — explicit |

Roster-safe identity is **not** email/contact and is **not** wellness/readiness/recovery/training detail.

### 6.3 Historical data after revoke / removal

**Decision (PO approved — ADR-033):**

- Revocation / removal / leave ends organization/coach access to **sensitive** athlete data **immediately**.
- Security/audit records may remain **internally** retained.
- Historical sensitive wellness/readiness data must **not** remain available through coach/org **product views** after authorization ends.
- Athlete retains full self-history regardless of org membership.

### 6.4 Minors / schools

School organizations may exist. Parent/guardian and minor-specific product support is **deferred**. V3 must **not** claim support/compliance for minor-specific legal regimes. Legal/product review is a **future dependency**.

---

## 7. Team readiness semantics

### 7.1 Principles

- Avoid inventing misleading science.
- **No default single composite Team Readiness Score** (PO locked).
- Prefer readiness-band / distribution / count semantics.
- Aggregation uses **stored** athlete readiness only — GET team readiness does **not** generate missing athlete readiness.
- Respect consent scopes and **minimum cohort** privacy rules.

### 7.2 V3 exposures (locked)

| Metric | V3? | Notes |
| --- | --- | --- |
| Counts by readiness band | **Yes** | Among consented + eligible members |
| Number ready / limited / unavailable / insufficient | **Yes** | Map bands + missing consent/data honestly |
| Limiting-dimension distribution | **Yes** (optional) | Only with appropriate consent / aggregate-safe derivative |
| Participation / availability summary | **Yes** if availability scope granted | |
| Mean/median numeric score | **Optional secondary** | Suppress when N &lt; `minCohortSize` |
| Training-load team summaries | **Later / limited** | After Slice E collaboration settles |
| Trends over time | **Optional** | Requires consent continuity rules |
| Single composite team score | **No** | Do not add without a new ADR + PO approval |

### 7.3 Insufficient-data / privacy behavior

- **`minCohortSize = 5`** for published team readiness aggregates (PO locked).
- If consented eligible sample &lt; 5: return `INSUFFICIENT_DATA` / suppressed cells — **not** a fake 0.
- Suppress smaller cohorts/cells where output could materially re-identify an athlete.
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
| Assign a workout / create team-session assignment | Membership + `TRAINING_COLLABORATION` | Explicit POST; athlete-owned plan; audited |
| Athlete decline / unable | Athlete owns response | Conflict/warning + outcome auditable |
| Assignment vs recommendation | Always | May disagree; both remain visible as distinct facts |
| Modify athlete-generated work (collaborator) | Same + optimistic lock | Audited; never mutates State Engine |
| Schedule team sessions | Same | Feasibility/environment rules still apply |

Coaches must **not** bypass: State Engine, readiness calculation, recommendation calculator ownership, or safety constraints. Assignments never overwrite State Engine/readiness.

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
| Accept invitation (same authorized account, already accepted) | **Idempotent success**; still guarantees **exactly one** membership |
| Accept with revoked/expired/wrong-account token | Fail closed, non-oracle |
| Create invitation | Email/account-bound; opaque CSPRNG token; store **hash only**; single-use; default expiry **7 days**; proposed role **immutable** after issue (change = revoke + new invite); dedupe pending per (team, email/account, role) |
| Revoke consent | Idempotent if already revoked |
| Coach plan / assignment PATCH | Optimistic `expectedVersion` / `version` as today |

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

### 12.1 V3 mobile in scope (PO locked)

- View/accept/decline team invitations
- Consent / sharing controls
- Membership / team context
- Team-assigned sessions appearing in athlete training when assigned

### 12.2 Web-only in V3 (PO locked)

- Full coach console (roster, admin, team readiness, dense staff/role management, bulk export)

### 12.3 Coach mobile

**Out of V3.** Full coach console remains web-only. Do not overload athlete `(tabs)` with coach chrome.

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

**Athlete-facing transparency (PO locked — ADR-035):** expose a subset for consent grant/revoke; membership join/leave/remove; coach collaboration/assignment where appropriate. Do **not** expose the raw internal security audit stream.

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

## 15. ADRs (Accepted)

Canonical tree: [`docs/adr/`](adr/README.md). Numbering: ADR-029 externally reserved; ADR-001–028 not reused; V3 lock continues at ADR-030+.

| ADR | Title | Status |
| --- | --- | --- |
| [029](adr/029-state-engine-vs-planning-ownership.md) | State Engine vs Planning ownership | Accepted |
| [030](adr/030-multi-persona-account.md) | Multi-persona Account | Accepted |
| [031](adr/031-organization-and-consent-bounded-contexts.md) | Organization + Consent bounded-context split | Accepted |
| [032](adr/032-authorization-and-idor-posture.md) | Authorization / IDOR posture | Accepted |
| [033](adr/033-consent-and-sharing-authority.md) | Consent / sharing authority | Accepted |
| [034](adr/034-training-plan-ownership-and-coach-collaboration.md) | TrainingPlan ownership and coach collaboration | Accepted |
| [035](adr/035-audit-model.md) | Audit model | Accepted |

Defer further ADRs: billing (V4), wearables (V5), AI coach (V6), marketplace (V7), parent/guardian, Season aggregate (unless later required).

---

## 16. Testing strategy (QA)

### 16.1 Principles

Server is the boundary; **404** (not 403) for foreign/inaccessible tenant resources; consent additive (no sensitive auto-grant); invitation accept idempotent for same account with exactly-one membership; no hidden writes; Slice Done ≠ happy-path demo.

### 16.2 Required suites (grow per slice)

- Authorization matrix using locked roles only (`ATHLETE`…`ORG_OWNER`) — no Staff RO cells
- Org/team IDOR isolation + multi-org/multi-team fixtures
- Invitation lifecycle (7-day expiry, hash-only token, immutable role, idempotent re-accept, wrong-account non-oracle)
- Consent grant/revoke/re-grant + immediate fail-closed for coach/org product views
- Membership removal / leave / stale access
- Team readiness honesty (`minCohortSize = 5`, no composite score, no hidden generate)
- Assignment vs recommendation both visible; athlete decline/unable audited (Slice E+)
- Athlete transparency subset vs raw audit denial
- Web Vitest coach/athlete flows; mobile Jest invite/consent/membership only
- HTTP multi-actor chains

### 16.3 CI / Verify (Slice A recommendation)

**Decision:** For Slice A, extend the existing Verify **`core`** shard with `--tests 'com.devinolabs.uap.organization.*'` and `--tests 'com.devinolabs.uap.consent.*'` in the **same change that lands real tests** for those packages.

| Rule | Detail |
| --- | --- |
| Fail-closed | Do not pre-declare empty `--tests` patterns; Gradle “no tests found” must fail the job; never `\|\| true` |
| Orphan ban | Any new `organization` / `consent` test package outside shard filters = Slice A Done failure |
| ModularityTests | Remain on `core`; new `@ApplicationModule` packages must satisfy allowedDependencies |
| Later split | If `core` approaches the 20-minute budget after B/C volume, introduce dedicated shard `org-consent` and update Sonar JaCoCo merge artifact count |

**Do not** edit `verify.yml` in this decision-lock task — apply the wiring when Slice A runtime is authorized.

### 16.4 Quality Gate Steward

Independent review after each substantial slice and before `develop` → `main`. Compare Overall trends to V2 baseline; campground when applicable.

---

## 17. Implementation slices

Prefer small **vertical** slices. Refined order puts **consent before wellness reads** and **coach planning writes before team aggregates that depend on stable collaboration** — team readiness still must not run before consent.

| Slice | Product outcome | Backend | Web | Mobile | DB | Tests / security / DoD |
| --- | --- | --- | --- | --- | --- | --- |
| **A — Foundation** | Org & Team exist | Modulith `organization` (+ `consent` skeleton); CRUD org/team; ports sketched | Hidden/feature-flagged routes OK | Athlete app regression only | `organizations`, `teams` | Cross-tenant IDOR; **wire Verify `core` patterns with first tests**; QG New Code; **DoD:** no wellness APIs; ADRs already Accepted |
| **B — Membership & invitations** | People can join with roles | Membership + invitation lifecycle (PO invitation rules); `membership` port | Athlete invite accept/decline; admin invite create | Invite accept/decline | memberships, invitations | Lifecycle + concurrency + token non-oracle; **DoD:** exactly one membership; idempotent re-accept |
| **C — Consent & sharing** | Athlete controls sharing | grants/scopes/revoke; `grants` port | Consent manager | Consent toggles | `consent_grants` | No sensitive auto-grant; revoke-then-read; **DoD:** no readiness without scope |
| **D — Coach roster & athlete views** | Coach sees authorized roster/detail | Roster + consent-aware projections; peer roster-safe identity | Coach shell: roster + athlete detail | Team context only | indexes as needed | IDOR + field matrix; **DoD:** no non-consented wellness; no peer email/wellness |
| **E — Coach training workflows** | Coach assigns/collaborates | Training use cases + ports; assignment vs recommendation; decline/unable | Coach planning UX (web) | Athlete sees assignments | optional templates | AuthZ + audit; **DoD:** State Engine not a write target |
| **F — Team readiness** | Honest team readiness | Aggregate stored consented readiness; `minCohortSize=5` | Team readiness UI (web) | — | none or justified tiny cache | Insufficient/suppress; **DoD:** no composite team score |
| **G — UX completion** | Cohesive coach + athlete org UX | API polish; athlete transparency subset | Full coach IA | Invite/consent polish | — | Cache isolation; **DoD:** athlete Home not admin-ized; no coach mobile console |
| **H — Hardening / release gate** | V3 releasable | Audit completeness; stale authZ battery | Hardening | Hardening | — | Full matrix freeze; Sonar vs baseline; **DoD:** steward + QA sign-off |

**Do not** start F before C. **Do not** write coach data into State Engine POSTs.

---

## 18. Definition of done (V3)

V3 is complete only when:

1. Athletes can join/leave teams under invitation + membership policy.
2. Coaches/admins can manage roster and roles within authorization matrix.
3. Consent scopes gate athlete-level wellness/readiness/training collaboration.
4. Team readiness is honest, consent-aware, non-misleading, uses `minCohortSize = 5`, and has **no** composite team score.
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

## 20. Product Owner decisions (approved) and Pre-Slice-A contract

### 20.1 Approved Product Owner decisions

| Topic | Decision | Slice implications |
| --- | --- | --- |
| Account / persona | One Account; multi-persona; Athlete + Coach/Admin simultaneous OK | ADR-030; authZ by membership |
| Coach-only accounts | Allowed; no Athlete row required; `athleteId` only when applicable | Membership schema; invite athlete vs coach paths |
| Organization topology | Flat Organization → Teams; no nesting; multi-org memberships OK | Slice A schema; no nested APIs |
| Multiple team memberships | Athletes may have multiple ACTIVE teams (incl. cross-org) | Unique keys per team; multi-team fixtures in QA |
| Peer roster visibility | Roster-safe identity only; no email/contact/wellness/readiness/recovery/training from peer membership alone | Roster DTO field allow-list |
| TrainingPlan ownership | Athlete-owned; coach collaborator/assigner; no coach-owned plan tree | ADR-034; Slice E |
| Consent defaults | No sensitive scopes auto-grant; membership ⇒ roster-safe identity only | Slice C before D/F |
| Leave/revoke history | Immediate end of coach/org sensitive product access; internal audit retained; no sensitive history in org views | AuthZ + projection tests |
| Invitations | Email/account-bound; CSPRNG token; hash-at-rest; single-use; 7-day expiry; immutable role; idempotent same-account re-accept; fail-closed non-oracle | Slice B |
| Minors | Deferred; schools OK without compliance claims; legal review future dependency | No guardian flows in V3 |
| Coach training authority | Explicit assignments OK; never mutate State Engine/readiness; assignment ≠ recommendation; athlete decline/unable; auditable | ADR-029/034; Slice E |
| Team readiness | No composite team score; bands/counts; `minCohortSize = 5`; GET read-only stored only | Slice F |
| Coach mobile | Web-only coach console; mobile athlete-first | Slice G scope |
| Athlete-visible audit | Transparency subset only; not raw security audit | ADR-035; Slice G/H |
| Staff/read-only | Deferred | Matrix without Staff RO |
| Season | No Season in Slice A; Team `ACTIVE`→`ARCHIVED` only | Slice A schema |

### 20.2 Pre-Slice-A contract (normative)

| Contract item | Locked value |
| --- | --- |
| Role enum | `ATHLETE`, `COACH`, `HEAD_COACH`, `TEAM_ADMIN`, `ORG_ADMIN`, `ORG_OWNER` |
| Organization lifecycle | `ACTIVE` → `ARCHIVED` |
| Team lifecycle | `ACTIVE` → `ARCHIVED` |
| Membership lifecycle | `ACTIVE` → `REMOVED` \| `LEFT` |
| Invitation lifecycle | `PENDING` → `ACCEPTED` \| `DECLINED` \| `REVOKED` \| `EXPIRED` |
| ConsentGrant lifecycle | `ACTIVE` → `REVOKED` (re-grant = new id) |
| Invitation idempotency | Same authorized account replaying successful accept → idempotent success; exactly one membership |
| Sensitive consent defaults | None auto-granted on join |
| Denial semantics | Unauthenticated **401**; inaccessible authenticated **404**; owned conflicts **409**/validation; invite abuse non-oracle |
| Multi-org / multi-team | Allowed |
| Coach-only membership | Allowed without Athlete row |

### 20.3 Slice A readiness review

Independent confirmation (docs-only; Lead coordinated with Backend, QA, Security/Quality Gate Steward, DevOps):

| Role | Verdict |
| --- | --- |
| Lead / Architect | **Ready** — ADRs Accepted; topology/roles/lifecycles locked; Slice A scope remains org/team foundation without wellness |
| Backend | **Ready** — Modulith split + Flyway org/team tables implementable; ports sketched; coach-only membership schema clear |
| QA | **Ready** — Matrix/IDOR/invite contracts concrete; Staff RO removed; 404 Deny cells; invitation idempotency testable |
| Security / Quality Gate Steward | **Ready** — Threat mitigations map to locked decisions; campground deferred until identity/security paths are touched; New Code gates unchanged |
| DevOps / CI-CD | **Ready** — Prefer extend Verify **`core`** with `organization.*` + `consent.*` when tests land; dedicated `org-consent` shard later if timeout risk; fail-closed; **no workflow edit in this lock task** |

**Remaining true blockers before Slice A runtime:** none architectural. Slice A still requires **explicit user authorization** to implement code/migrations/Verify wiring.

**Not blockers:** V4–V7 deferrals; minors legal review; Season; Staff RO; coach mobile.

---

## 21. Risks

| Risk | Mitigation |
| --- | --- |
| Org god-module | Split `organization` vs `consent`; ports into training |
| Consent bolted on late | Slice C before D/F |
| Second readiness engine | Aggregates read stored assessments only |
| JWT role stale privileges | Membership/consent re-check every sensitive call |
| Athlete Home polluted | Sibling coach IA |
| Verify skips new tests | Wire `core` patterns with first tests; orphan = Done fail |
| Legal overclaim | Schools without compliance claims; minors deferred |
| Scope creep into V4–V7 | Explicit boundaries §19 |
| Touching CSRF/cookie debt poorly | Campground + steward review |

---

## 22. External Integration note

V3 does not require provider work. If org roster later syncs from SIS/HR systems (post-V3), keep provider DTOs behind integration boundaries — same rule as V5 wearables.

---

## 23. First authorized implementation step (Slice A)

**Not authorized by this decision-lock commit.** When explicitly authorized:

1. Create Modulith `organization` (+ `consent` skeleton) per ADR-031.
2. Flyway for `organizations` / `teams` (`ACTIVE`/`ARCHIVED`).
3. Wire Verify `core` `--tests` for new packages **with** first real tests.
4. Cross-tenant IDOR tests green; no wellness/consent product APIs yet.
5. Steward New Code review for the slice.

Stop until that authorization is explicit.

---

## 24. Git / safety for decision-lock work

- Branch: `develop`
- Docs/ADR only
- No runtime code, migrations, API/UI implementation, PR, merge to `main`, deploy, tag, publish, visibility, or Sonar admin changes

Suggested commit:

`docs: lock V3 product decisions and architecture`
