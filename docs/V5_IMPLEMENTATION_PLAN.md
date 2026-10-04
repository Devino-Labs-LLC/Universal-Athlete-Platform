# Athlete Readiness V5 — Connected Athlete Implementation Plan

**Theme:** Connected Athlete (wearables / health-platform integrations)  
**Product question:** How does Athlete Readiness incorporate trustworthy connected athlete signals without allowing third-party devices or providers to become the owner of the athlete’s readiness truth?  
**Document type:** Product Owner + Architecture planning lock (docs)  
**Planning tip:** `e955f05da58d230046c265b1b9a42ff5875a1004` (`main` = `develop`, 0/0)  
**Production schema baseline:** Flyway **V37**  
**Prior version:** V4 commercialization — G–I **PRODUCTION DEPLOYED / DORMANT VERIFIED**; commercial billing **OFF**; V4 **NOT** commercially active  
**V5 status:** **V5A IMPLEMENTATION COMPLETE / CI VERIFIED on develop**; Health Connect Android **DEVICE CERTIFIED — DEVELOP** (2026-10-03; CSRF revalidated §36.12; final lifecycle closeout §36.13 on tip `b3b4620` / Verify [37174694721](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/37174694721)); **V5 MVP device gate SATISFIED** (D1 — ≥1 real certified connector); HealthKit **IMPLEMENTED / CI VERIFIED**, **DEVICE CERTIFICATION PENDING** (**NOT DEVICE CERTIFIED**); production **NOT VERIFIED / NOT PROMOTED**; V5B deferred  

**Highest accepted ADR before this plan:** ADR-045 → V5 ADRs **046–051 Accepted** (2026-09-30 PO lock)  
**PO decisions:** D1–D14 **LOCKED** 2026-09-30  
**Implementation:** Authorized on `develop` after PO-lock Verify (this program); **no** `main`/production promotion

---

## 0. Agents consulted

| Role | Contribution |
| --- | --- |
| Lead / Architect | Modulith placement, lifecycle, slices, ADR set, consolidated PO decisions |
| Backend | Persistence sketch, ownership, inbox separation from billing |
| Athlete Intelligence / Data | Evidence model, V5A vs V5B, precedence, State Engine boundary |
| External Integration | Provider matrix, HealthKit / Health Connect / OAuth research |
| Mobile | HealthKit / Health Connect UX, offline queue, IA separation from V4 billing |
| Web | Connected Apps IA, OAuth-capable surfaces, Railway package constraint |
| Security / Code Quality | Threat model T23–T36, consent layers, token rules |
| QA / Test Automation | Full test matrix and slice done bars |
| DevOps / CI-CD | Dormant deploy vs activation gates, observability, Verify shard note |
| Documentation / Release | This plan + Proposed ADRs 046–051 |

---

## 1. Vision / objective

V5 makes Athlete Readiness a **Connected Athlete** product: athletes can link approved health/fitness sources, sync normalized evidence with provenance, and (when Product Owner unlocks) allow State Engine to consume mapped knowledge — while:

- State Engine / readiness / recommendations remain owned by `training` (ADR-029);
- providers never write readiness scores, coach authorization, membership, or recommendation truth;
- consent remains additive (connect ≠ coach share);
- athletes without wearables remain first-class;
- V4 commercial flags stay orthogonal and default **off**.

---

## 2. Current V4 baseline (authoritative)

| Item | Status |
| --- | --- |
| `main` / `develop` | `e955f05da58d230046c265b1b9a42ff5875a1004` (0 ahead / 0 behind) |
| Schema | **V37** |
| A–F | **PRODUCTION VERIFIED** |
| G–I runtime | **PRODUCTION DEPLOYED / DORMANT VERIFIED** |
| Individual Stripe | **SANDBOX CERTIFIED** |
| Apple / Google **billing** | **IMPLEMENTED / CI VERIFIED / SANDBOX BLOCKED — CREDENTIALS** |
| Commercial billing / entitlement / capacity | **OFF** |
| Wearables / HealthKit / Health Connect | **Not implemented** (V3 §19 deferred to V5) |

**Carryover blocker (must not contaminate V5):** V4 Apple/Google **billing** sandbox certification remains credential-blocked. HealthKit / Health Connect are **separate** OS surfaces from App Store / Play Billing. Do not reopen G–I architecture or fake V4 certification inside V5.

---

## 3. V5 scope (candidate — pending PO lock)

**In scope for planning:**

1. Provider-neutral **integrations** Modulith module (connections, sync, evidence ports).
2. Athlete-owned connection lifecycle + explicit sync (no hidden sync on GET).
3. Normalized connected evidence with provenance + idempotent ingest.
4. Consent layers A–D (ADR-050 Proposed): connect / process / coach share / org visibility.
5. Mobile OS hubs: **Apple HealthKit**, **Android Health Connect** (foundation providers).
6. Optional later **one** server OAuth wearable connector (candidate WHOOP; Garmin currently access-constrained).
7. Athlete Connected Apps UX (Web + Mobile); coach sees derived consented product data only.
8. Optional State Engine consumption (**V5B**) only after Product Owner lock — default plan separates store-only (**V5A**).
9. Dormant deploy / per-provider activation / commercial availability separation.

---

## 4. Explicit non-scope

| Out | Why |
| --- | --- |
| V6 generative / predictive AI coach | V3 §19; purple/AI tokens reserved |
| V7 marketplace / social / brand ecosystem | V3 §19 |
| Music (Spotify / Apple Music / Audiomack) | Catalog ≠ Connected Athlete |
| SIS / HR roster sync | Different problem |
| Nutrition platform depth | Not implied by V5 foundation |
| Weight-room TV / unrelated org expansion | Not Connected Athlete |
| Medical diagnosis / clinical / HIPAA claims | Constitution |
| Second readiness engine | ADR-029 |
| Coach connecting a provider for an athlete | Athlete-owned |
| Shipping “all wearables” | Foundation ≠ connectors |
| Replacing Railway Web B1 hardlink strategy | Proven V4 deploy baseline |
| Turning on V4 commercial flags | Separate launch gate |

---

## 5. Product principles

1. **Providers supply evidence; Athlete Readiness owns truth.**
2. **Map at the boundary** — no HealthKit / Health Connect / WHOOP / Garmin types in State Engine.
3. **GET remains no-hidden-write / no-hidden-sync.**
4. **Connect ≠ process ≠ coach share ≠ org share.**
5. **Fail honestly** — never fake provider success.
6. **Minimize health data** — normalized evidence + provenance; no raw warehouse by default.
7. **No wearable → no readiness penalty.**
8. **Manual athlete agency preserved** unless PO locks otherwise.
9. **V4 billing ≠ V5 health** — separate modules, IA, flags, and certifications.
10. **Dormant code ≠ activated provider ≠ commercially available.**

---

## 6. Connected-data ownership

| Concern | Owner |
| --- | --- |
| Connection, tokens, sync runs, adapters | New Modulith module **`integrations`** (ADR-046 Proposed) |
| Provider-neutral evidence store / ports | `integrations` publishes; Athlete Intelligence defines contracts |
| State Engine / readiness / recommendations | `training` only (ADR-029) |
| Coach/org sensitive views | `consent` + existing authZ (ADR-033) |
| Commercial entitlement (if ever gated) | `entitlements` / billing — **orthogonal**, default free until PO locks |

---

## 7. Provider-neutral architecture

```text
OS hubs / OAuth wearables
        │ adapters (DTOs, tokens, webhooks, device uploads)
        ▼
integrations (Connection + SyncRun + inbox + mapping)
        │ provider-neutral evidence ports
        ▼
training State Engine (explicit generation only)
        ▼
READINESS_Vn / recommendations (unchanged ownership)
```

**Lead recommendation:** module name `integrations` (matches V3 §19). Reject folding into `training` / `athlete` / `billing`.

Allowed dependency sketch (proposed):

- `integrations` → `identity :: auth`, `athlete :: context`, `audit :: writer`, optionally `entitlements`
- `training` → `integrations :: evidence` (read/consume mapped facts only)
- never: `training` → provider SDKs

---

## 8. Signal / evidence model (Athlete Intelligence)

### 8.1 Families (V5A store-capable)

| Family | Canonical examples | V5A store | V5B State Engine input |
| --- | --- | --- | --- |
| Sleep | duration, stages summary if available | Yes | PO lock required |
| Activity / steps | daily steps, active energy | Yes | PO lock |
| Heart | resting HR, workout HR summaries | Yes | PO lock |
| HRV | overnight / morning summary metrics | Yes | PO lock |
| Workouts | start/end, sport type, duration, strain proxies | Yes (dedup critical) | PO lock |
| Body metrics | weight (if approved) | Optional / minimize | Defer unless PO insists |
| Recovery-adjacent proprietary | WHOOP strain/recovery etc. | Only via connector if approved | PO lock |

Do **not** auto-accept every provider field. Prefer summaries over high-frequency streams in V5.

### 8.2 Manual vs connected

| Rule | Lead recommendation |
| --- | --- |
| Athlete check-in sleep/ratings | Remain athlete-owned; do not silent-overwrite |
| Connected sleep/HRV | Parallel evidence with provenance |
| Conflict display | Show both / prefer athlete-entered for subjective dims |
| Device workout vs in-app occurrence | Dedup by time window + provenance; do not double-count load |

### 8.3 Missing / stale

- Missing wearable: athlete remains first-class; **no readiness penalty**.
- Stale: expose freshness class (`fresh` / `stale` / `unavailable`) — exact thresholds are a **PO decision**.
- Reject impossible values / future timestamps / negative durations (quarantine + audit; do not crash readiness).

### 8.4 V5A vs V5B

| Track | Meaning |
| --- | --- |
| **V5A** | Acquire, normalize, store, display provenance — **no** `READINESS_V1` formula change |
| **V5B** | Selected signals may enter State Engine / new calculator version — **requires PO lock**, fixtures, explainability, missing-data behavior, version id |

**Lead + Athlete Intelligence recommendation:** ship V5-MVP as **V5A + optional light display**; keep V5B as a separate unlock.

---

## 9. Provenance model

Each evidence row must support explaining “where did this come from?”:

- `providerKey`
- `externalRecordId` (or content hash fallback)
- `sourceDeviceOrApp` (when known)
- `observedAt` (UTC + timezone metadata when available)
- `providerUpdatedAt` (nullable)
- `ingestedAt`
- `syncRunId`
- `provenanceClass` (e.g. `OS_HUB`, `OAUTH_PROVIDER`, `CLIENT_DEVICE`)
- `quality` / confidence (optional)

Provenance is **durable product data**, not logs-only.

---

## 10. Consent / privacy

See ADR-050 Proposed.

| Layer | Description |
| --- | --- |
| A | Authorize provider / OS health connection |
| B | Authorize Athlete Readiness processing of imports |
| C | Coach visibility via existing ConsentGrant scopes |
| D | Org visibility via membership + consent |

**STOP:** connection must never auto-expand coach/org visibility.

Privacy posture: minimize; encrypt tokens at rest; no HIPAA/medical-device claims; athlete self-history vs disconnect retention is PO decision #13.

---

## 11. Connection lifecycle

See ADR-048 Proposed.

**Connection:** `DISCONNECTED` | `PENDING` | `CONNECTED` | `NEEDS_REAUTH` | `ERROR`  
**Sync run:** `REQUESTED` | `RUNNING` | `SUCCEEDED` | `PARTIAL` | `FAILED`

Disconnect: stop future sync; revoke/invalidate tokens; retention of normalized history = PO lock (Lead: retain self-history, drop credentials).

---

## 12. Synchronization model

See ADR-049 Proposed.

| Mode | Use |
| --- | --- |
| Mobile upload | HealthKit / Health Connect → authenticated upload API |
| Explicit sync | Athlete CTA / `POST …/sync` |
| Webhook / push | OAuth providers when approved |
| Scheduled job | After MVP; PO decision #6 recommends user-explicit only for MVP |

**Initial backfill window:** Product Owner decision (candidates: 7 / 30 / 90 days). Health Connect history beyond ~30 days may require extra OS permission (`READ_HEALTH_DATA_HISTORY`).

Pagination, cursors/checkpoints, rate-limit backoff, corrections/deletes: owned by integrations sync design; must be idempotent.

---

## 13. Idempotency / concurrency

See ADR-051 Proposed.

- Unique evidence key: `(providerKey, athleteId, externalRecordId)` (+ sync stream).
- Integrations inbox separate from `billing_provider_events`.
- Concurrent uploads / webhook replay / offline queue replay: same row, no double State Engine side effects.
- Required concurrency tests before connector production deploy.

---

## 14. Time / units

- Store instants in **UTC**; retain provider timezone / local day where sleep spans midnight.
- Do not infer “today” solely from UTC date for athlete-local surfaces (reuse existing athlete timezone conventions where present).
- Canonical units at domain boundary (minutes, seconds, kg, bpm, ms, kcal, meters). Adapters convert; provider units do not leak.

---

## 15. State Engine integration boundary

```text
Connected Evidence → validate/normalize → (optional) Knowledge resolution
        → explicit State Engine generation → READINESS_Vn
```

Forbidden: `provider webhook → direct readiness mutation`.

---

## 16. Provider candidates & priority

### Official research snapshot (planning; not credentials)

| Provider | Auth | Mobile vs server | Notes | V5 stance |
| --- | --- | --- | --- | --- |
| **Apple HealthKit** | OS per-type auth | **Mobile read** + upload; background delivery entitlement | Mature; device testing required for background | **Foundation #1** (Lead: if iOS is primary launch OS) |
| **Android Health Connect** | OS permissions + Play declarations | **Mobile read** + Changes tokens; background read permission optional; history >30d special permission | Aggregate for steps to avoid double-count | **Foundation #2** (honest parity; may trail iOS) |
| **WHOOP** | Server OAuth (typical) | Backend tokens + APIs | Proprietary recovery/strain may fill hub gaps | **Optional C2+ candidate** |
| **Garmin Health API** | OAuth2 PKCE; partner program | Server push/webhooks | New developer program access historically constrained / paused | **Defer** until access confirmed |
| **Fitbit / Google Fit legacy** | OAuth; restricted scopes / CASA risk | Server | Migration & compliance burden | **Defer** |
| Strava / Oura / Polar / nutrition | Various | — | Not required for foundation | **Out of V5-MVP** unless PO expands |

**External Integration + Lead recommendation:**  
Foundation = OS hubs (HealthKit then Health Connect). At most one OAuth connector later if hubs leave a product gap (WHOOP preferred over Garmin while Garmin access is constrained). Do **not** ship all brands in V5.

Dedup: hubs already aggregate many device brands; a later direct Garmin connector must not double-count the same workout already imported via HealthKit/Health Connect (lineage / external id / time-window merge — ADR follow-up when multi-provider unlocked).

---

## 17. Web / iOS / Android / coach responsibilities

### Web

- Connected Apps status, disconnect, errors, last sync.
- Browser OAuth for **server** providers only.
- **Cannot** access HealthKit / Health Connect.
- Educate “connect Apple Health / Health Connect in the mobile app”.
- Shared packages must remain Railway-safe (`file:` + `--install-links` + B1 `cp -al` hardlinks). Never reintroduce `workspace:*` for web deploy.

### iOS

- HealthKit permission UX, anchored/observer queries, optional background delivery.
- Normalize → durable offline upload queue → server.
- Dedicated **Profile → Connected Apps** (not Premium billing).

### Android

- Health Connect permissions, read/aggregate, Changes API, optional background read.
- Honest capability differences vs iOS (no fake parity).
- Same Connected Apps IA.

### Coach / org

- No raw wearable console.
- See existing readiness/recovery projections only under ConsentGrant.
- Never connect/revoke another athlete’s provider; never read tokens.

---

## 18. Security threat model (summary)

Continues V4 numbering → **T23–T36** (Security steward). Highest severity themes:

| IDs | Themes |
| --- | --- |
| T23–T25 | Forged upload, foreign athlete bind, replay duplication |
| T26–T27 | OAuth/PKCE failure, token theft |
| T28–T30 | Over-broad scopes, webhook forgery, account mismatch |
| T31–T33 | Malicious timestamps, duplicate amplification, consent bypass |
| T34–T36 | Coach IDOR, revoke-still-syncing, payload/log leakage |

**Architecture STOP conditions:** connection auto-expands coach visibility; tokens in clients/logs; GET triggers sync; provider types in State Engine; billing inbox reused for health.

Full table lives with Security planning evidence; implementers must map mitigations into ADRs 046–051 and slice tests.

---

## 19. Data retention / deletion

| Item | Proposed default (pending PO) |
| --- | --- |
| Access/refresh tokens | Encrypted at rest; deleted/invalidated on disconnect |
| Normalized evidence | Retain for athlete self-history (Lead #13B) unless PO chooses delete-on-disconnect |
| Raw provider payloads | Minimize / TTL; not a warehouse |
| Audit | Retain disconnect/connect events without secrets |
| Export | Future; do not invent unsupported compliance claims in V5 |

---

## 20. Observability

Metrics (no sensitive payloads): connections by provider, sync success/fail/partial, reauth required, records ingested/rejected, provider latency, rate-limit counts.

Logs: never tokens, never full health payloads.

---

## 21. Failure / recovery

Provider outage must not take Athlete Readiness down. Timeouts, backoff, rate limits, partial sync honesty. Manual check-in / training / readiness paths continue without connected data.

---

## 22. Entitlement / commercial implications

**Unresolved Product Owner decision (#11).**

| Option | Consequence |
| --- | --- |
| **A — Free in V5** (Lead recommendation until V4 commercial live) | Avoid coupling Connected Athlete to dormant billing |
| **B — `INDIVIDUAL_PREMIUM` gated** | Requires commercial activation story + UX for unpaid athletes |
| **C — Org-paid add-on** | Complex; defer unless PO has a clear org SKU |

Billing remains separate from authorization and consent.

---

## 23. Proposed V5 slices

Aligned Lead (F/C/S/U) with QA letter map.

### Phase F — Foundation (required)

| Slice | Purpose | Backend | Web | Mobile | Integrations | Data | Security | Tests | External prereq | Done bar |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **F0** | Docs + PO lock | — | — | — | — | — | ADR Proposed→Accepted | Docs Verify | None | This plan Accepted + PO decisions locked |
| **F1** | `integrations` module + Connection/SyncRun schema + flags off | Modulith + Flyway | — | — | Skeleton ports | Connection tables | IDOR ownership | Domain/HTTP/modularity | None | Module boots dormant; GET creates nothing |
| **F2** | Evidence contract + inbox pattern | Evidence tables + ports | — | — | Mapping ports | Provenance | T25/T31 | Persistence/idempotency | None | Upsert idempotent; training can depend on port |
| **F3** | Athlete UX shell | Status APIs | Connected Apps empty/disabled CTAs | Same | — | — | No secrets in UI | Web/Mobile UX tests | None | IA live; connect gated until C1 |

### Phase C — Connectors (ordered; not all required)

| Slice | Purpose | Notes |
| --- | --- | --- |
| **C1** | First connector | PO picks HealthKit **or** Health Connect first (Lead: HealthKit if iOS-primary). Sandbox/device cert path. Flag default off. |
| **C2** | Second OS hub | The other of HealthKit / Health Connect |
| **C3…** | Optional OAuth wearable | Only if PO unlocks; WHOOP candidate; Garmin deferred pending access |

### Phase S — State Engine consumption (optional relative to store-only MVP)

| Slice | Purpose |
| --- | --- |
| **S1** | Eligible observation types as knowledge |
| **S2** | Explicit generation consumes stored evidence (V5B if formula changes) |
| **S3** | Regression ADR-029/034 ownership |

### Phase U — UX hardening

| Slice | Purpose |
| --- | --- |
| **U1** | Connect / reauth / revoke / last sync / errors |
| **U2** | Athlete transparency of imports |
| **U3** | Coach surfaces only if PO unlocks (still no raw streams) |

### Phase R — RC / promotion

| Slice | Purpose |
| --- | --- |
| **R1** | Full matrix freeze, Verify+Sonar, steward+QA sign-off, dormant production promotion strategy |

**Suggested completion bars (PO #1):**

- **V5-Foundation:** F0–F3  
- **V5-MVP (Lead recommendation):** Foundation + one certified connector + U1 + evidence display; **V5A** (no formula change) unless PO unlocks V5B  
- **V5-Expanded:** additional connectors as explicit increments  

---

## 24. Per-slice completion gates (execution model)

For every future runtime slice:

1. Focused tests green  
2. Regression battery for identity/consent/readiness/training/org/billing  
3. Specialist reviews (QA + Security steward minimum)  
4. Commit → push `develop` → GitHub Verify → Sonar  
5. Sonar New Code: Reliability/Security/Maintainability **A**; coverage **≥80%**; duplication **≤3%**; hotspots **100%**; Quality Gate **PASS**  
6. Evidence in this plan’s ledger (append later)  
7. Next slice only after done bar  

Windows long serial Testcontainers may be **INCONCLUSIVE**; GitHub Verify remains authoritative.

---

## 25. Test matrix (QA summary)

Layers: Unit, Domain, Adapter, HTTP, Persistence, Concurrency, Idempotency, Replay/OOO, Offline queue, Mobile, Web, Security, Consent, IDOR, Provider sandbox, Regression.

Must cover: GET no-hidden-sync; revoke stops sync; foreign athlete 404; forged upload rejection; HealthKit≠billing; Health Connect permissions revoked; multi-source workout dedup when multi-provider unlocked; readiness unchanged when wearable absent (V5A).

---

## 26. Provider certification plan

| Gate | Meaning |
| --- | --- |
| Implementation complete | Code + tests on `develop` |
| Sandbox / device certified | Real OS permissions or OAuth sandbox; classify BLOCKED if credentials/devices missing (honest, like V4 Apple/Google billing) |
| Production deployed dormant | Flags off; health UP |
| Provider activated | Per-provider enable flag + secrets |
| Commercially available | Product launch gate (possibly entitlement) |

Never collapse these statuses.

---

## 27. Deployment strategy

- Preserve Railway Web **B1**:

```text
cd apps/web && npm install --install-links && cd /app && mkdir -p node_modules && cp -al apps/web/node_modules/. node_modules/
```

- Local/CI remain **pnpm**.
- Per-provider `UAP_INTEGRATIONS_<PROVIDER>_ENABLED` (name TBD) default **false**; fail-fast when enabled without required secrets.
- Prefer future Verify shard for `integrations` if core grows heavy (DevOps note; no workflow edit in this planning task).
- Develop-only planning docs in this task; **no main merge required** for planning.

---

## 28. Release / rollback

- Prefer flag-off rollback over schema down-migrations.
- Disconnect path must remain available if a connector misbehaves.
- Do not delete production DB to “undo” a connector.

---

## 29. V4 carryover blockers

| Blocker | V5 handling |
| --- | --- |
| Apple/Google **billing** sandbox credentials | Separate stream; do not block HealthKit/Health Connect planning |
| Commercial billing OFF | Keep OFF; do not enable for V5 tests |
| Entitlement/capacity OFF | Keep OFF |

---

## 30. V6 / V7 deferrals

| Version | Scope | V5 stance |
| --- | --- | --- |
| V6 | Generative / predictive AI coach | Do not implement; no purple AI chrome on Connected Apps |
| V7 | Marketplace / social / ecosystem | Do not implement |

---

## 31. ADRs (Accepted)

| ADR | Title | Status |
| --- | --- | --- |
| [046](adr/046-integrations-bounded-context.md) | Integrations bounded context | **Accepted** |
| [047](adr/047-connected-evidence-and-provenance.md) | Connected evidence & provenance | **Accepted** |
| [048](adr/048-connection-and-sync-lifecycles.md) | Connection & sync lifecycles | **Accepted** |
| [049](adr/049-explicit-connected-sync.md) | Explicit sync / no hidden GET sync | **Accepted** |
| [050](adr/050-connected-consent-layers.md) | Connected consent layers | **Accepted** |
| [051](adr/051-health-integration-idempotency.md) | Health ingest idempotency | **Accepted** |

Accepted 2026-09-30 after Product Owner locked D1–D14.

---

## 32. V5 PRODUCT OWNER DECISIONS (LOCKED 2026-09-30)

Options and rationale remain below for audit. **Locked values** are authoritative in §33.

### D1 — What “V5 complete” means — **LOCKED B**

Foundation + at least one real connected-health connector certified + minimal Connected Apps UX. HealthKit first; Health Connect remains an approved V5 target after HealthKit. OAuth wearables not required for V5 completion.

### D2 — Modulith home — **LOCKED A**

New `integrations` module. `training` retains State Engine / readiness / recommendations.

### D3 — First connector — **LOCKED A1**

HealthKit first, then Health Connect. No fake parity. No simultaneous first-connector delivery if it harms quality.

### D4 — Backfill window — **LOCKED B**

Default **30 days**, centrally configurable. Respect OS history/permission limits.

### D5 — Ingest vs readiness generation — **LOCKED B**

V5A store/map/display only. Sync must not mutate readiness.

### D6 — Manual vs connected precedence — **LOCKED B**

Manual check-in remains authoritative for its workflow; connected data is parallel evidence.

### D7 — Multi-provider concurrency — **LOCKED A (MVP)**

One active health provider/hub per athlete in V5 MVP; schema must allow later multi-provider without destructive redesign.

### D8 — Coach visibility — **LOCKED A**

No new coach connected-health metadata in V5 MVP. Existing consented readiness/recovery projections only.

### D9 — Retention after disconnect — **LOCKED B**

Drop credentials; stop sync; retain normalized athlete self-history. Disconnect ≠ privacy deletion request.

### D10 — Entitlement — **LOCKED A**

Connected Athlete is **free** in V5. Keep entitlement seam; do not wire commercial gating.

### D11 — Scheduled/background server sync — **LOCKED B**

User-explicit sync only in MVP. No server scheduled polling. Mobile durable queue for athlete-initiated sync is allowed when idempotent.

### D12 — Stale thresholds — **LOCKED B**

Qualitative fresh/stale/unavailable from provenance timestamps; provisional product logic only; not clinical.

### D13 — Readiness formula — **LOCKED A**

V5A only. No `READINESS_V2`. Missing wearable never reduces readiness. V5B deferred.

### D14 — OAuth wearable — **LOCKED A**

OS hubs only (HealthKit + Health Connect). No WHOOP/Garmin/Fitbit/Oura/Strava/Polar in this V5 program.

---

## 33. Decision log

| ID | Decision | Locked value | Date | Notes |
| --- | --- | --- | --- | --- |
| D1 | V5 complete bar | **B** | 2026-09-30 | Foundation + ≥1 certified connector + Connected Apps UX; HealthKit first; Health Connect approved next; OAuth not required |
| D2 | Modulith home | **A** | 2026-09-30 | New `integrations` module |
| D3 | First connector | **A1** | 2026-09-30 | HealthKit → Health Connect; honest platform differences |
| D4 | Backfill window | **B** | 2026-09-30 | 30 days default; centrally configurable |
| D5 | Ingest vs generate | **B** | 2026-09-30 | Store-only; no auto readiness |
| D6 | Manual precedence | **B** | 2026-09-30 | Parallel evidence; no silent overwrite |
| D7 | Multi-provider MVP | **A** | 2026-09-30 | One active hub; schema multi-ready |
| D8 | Coach visibility | **A** | 2026-09-30 | No new connected coach metadata |
| D9 | Disconnect retention | **B** | 2026-09-30 | Drop credentials; retain normalized history; deletion separate |
| D10 | Entitlement | **A** | 2026-09-30 | Free in V5; seam only |
| D11 | Scheduled sync | **B** | 2026-09-30 | User-explicit only; no server cron |
| D12 | Stale thresholds | **B** | 2026-09-30 | Qualitative / provisional |
| D13 | Readiness formula | **A** | 2026-09-30 | V5A only; V5B deferred |
| D14 | OAuth wearable | **A** | 2026-09-30 | OS hubs only |

---

## 34. Explicit non-claims / program boundary

- Product Owner D1–D14 are **locked** (2026-09-30). ADRs 046–051 are **Accepted**.
- This document **authorizes V5 runtime implementation on `develop`** after the PO-lock Verify is green.
- Still **not** authorized by this program: merge to `main`, Railway production deploy, provider activation in production, V4 commercial billing on, App Store / Play publish, scheduled sync.
- V4 remains **not** commercially active.
- Apple/Google **billing** remain **not** sandbox-certified (orthogonal to HealthKit / Health Connect).
- V5B readiness formula change remains **deferred**.
- Health Connect Android **DEVICE CERTIFIED — DEVELOP** (§36.11–§36.13) does **not** imply HealthKit certification, production verification, or commercial activation.

---

## 35. Execution ledger

| Slice | Status | Runtime SHA | Migrations | Verify | Sonar | Device/provider proof | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| F0 PO lock + ADR accept | **COMPLETE** | `8d3d6ee` | — | [36805522460](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/36805522460) SUCCESS | PASS | N/A | D1–D14 locked; ADR 046–051 Accepted |
| F1 Foundation module | **COMPLETE** | `e6d85f1` | **V38** | [36809793740](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/36809793740) SUCCESS | PASS | N/A | Flyway tip fix after `0c71b96` |
| F2 Evidence + inbox | **COMPLETE** | `d59d07b` | **V39** | [36813219849](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/36813219849) SUCCESS | PASS | N/A | Artifact cleanup on tip |
| F3 UX shell | **COMPLETE** | `2bf16d4` (includes Sonar remediations) | — | Sonar FAIL on `dee885b`; green after remediations on `2bf16d4` | PASS on `2bf16d4` | N/A | Profile → Connected Apps; shared `@uap/connected-apps-contracts` |
| C1 HealthKit | **COMPLETE (CI)** | `2bf16d4` | — | [36819796718](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/36819796718) SUCCESS | PASS | **IMPLEMENTED / CI VERIFIED**; **DEVICE CERTIFICATION PENDING** (**NOT DEVICE CERTIFIED**) | Adapter + queue + evidence upload; CI only; physical iOS + Apple path still required for device cert |
| C2 Health Connect | **COMPLETE (CI)** + **DEVICE CERTIFIED — DEVELOP** | Runtime cert §36.11; CSRF §36.12; final lifecycle §36.13 on tip `b3b4620` | — | [37174694721](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/37174694721) SUCCESS | PASS | **DEVICE CERTIFIED — DEVELOP** | See §36.11–§36.13; READ-only HC; synthetic Toolbox writer only |
| U1/U2 UX harden | **COMPLETE** | `01f8c4e` | — | [36821461021](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/36821461021) SUCCESS | PASS | N/A | Provisional freshness labels (D12) |
| R1 RC | **COMPLETE on develop** (RC-ready develop; **not** production) | `01f8c4e` + HC device cert close-out | V38–V39 | SUCCESS | PASS | HC **DEVICE CERTIFIED — DEVELOP**; **V5 MVP device gate SATISFIED**; HK device cert pending | V5A implementation + CI complete; D1 satisfied by HC Android; no `main`/prod |
| Phase 0 RC hygiene | **COMPLETE** | `375768d` | — | [36867244813](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/36867244813) SUCCESS | PASS | N/A | Removed stale U1/U2+R1 NOT STARTED ledger dupes; coach-of-athlete integrations IDOR HTTP test |
| Device cert gate | **MVP SATISFIED — HC DONE / HK OPEN** | EAS Android Dev Client + Railway develop; tip `b3b4620` | V38–V39 | [37174694721](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/37174694721) SUCCESS | PASS | HC **DEVICE CERTIFIED — DEVELOP**; HK **NOT DEVICE CERTIFIED** | D1 (≥1 certified connector) **SATISFIED** by HC Android; HK remains provider-completeness follow-up |

---

## 36. Real-device certification evidence (develop only)

**Gate date:** 2026-10-01  
**Program:** develop-only; no `main` promotion; no production activation; V4 billing untouched.

### 36.1 Phase 0 hygiene

| Item | Result |
| --- | --- |
| Stale ledger dupes (`U1/U2` / `R1` NOT STARTED) | **Removed** (COMPLETE rows retained) |
| Coach-of-athlete negative auth | **Added** `IntegrationCoachPrincipalAuthorizationHttpIntegrationTests` — coach on same team cannot get/mutate athlete connection/evidence (`CONNECTION_NOT_FOUND`); athlete owner still `CONNECTED` |
| Focused local test | **PASS** (`gradlew test --tests …IntegrationCoachPrincipalAuthorizationHttpIntegrationTests`) |

### 36.2 Develop backend certification environment

| Check | Result |
| --- | --- |
| Railway CLI | Present; **`railway whoami` → Unauthorized** |
| Develop provider flags enable/restore | Later live cert (§36.11–§36.13) used develop flags **on**; restore-to-`false` remains an **operator follow-up** (docs agents do not mutate Railway) |
| Health / readiness / Flyway V38–V39 on develop host | **Not verified live** — blocked on CLI auth |
| Production | **Untouched** |

### 36.3 Disposable athlete accounts

Not created — device journeys could not start. Strategy remains: separate non-production athlete accounts per platform (D7 one-active-hub).

### 36.4 Platform access preflight

| Check | Result |
| --- | --- |
| `eas-cli` | `24.8.0` via `npx` (not on global PATH as `eas`) |
| `eas whoami` | **Not logged in** (interactive `npx eas-cli login` required — PO local) |
| Apple Developer / device registration | **Not exercised** |
| `adb` | `34.0.4`; `adb devices` **empty** |
| Android emulator/device | **None attached** |
| Mobile config | **PASS** — `com.devinolabs.uap`; `expo-dev-client`; `react-native-health`; HealthKit no clinical; HC READ_* only |

### 36.5 Apple HealthKit

| Cell | Status |
| --- | --- |
| Build type / ID | **BLOCKED** — EAS auth required for development build |
| Device / OS | **BLOCKED** — no iPhone available to this agent |
| Availability / permissions / deny / allow | **Not exercised** |
| Signal families (sleep, RHR, HRV, steps, active energy, workout) | **CI VERIFIED ONLY** (adapter/normalize unit tests); device cells **NO TEST RECORD AVAILABLE** |
| 30-day import / upload / idempotency / offline / disconnect / reconnect | **Not exercised** |
| **Certification** | **BLOCKED — EAS LOGIN + PHYSICAL iOS DEVICE** |

PO unblock (interactive, local only): from `apps/mobile` run `npx eas-cli@24.8.0 login`, then `npx eas-cli@24.8.0 build --platform ios --profile development`, register/install on a physical iPhone Dev Client, re-run §12–§16 journey against develop backend.

### 36.6 Android Health Connect

**Historical status (2026-10-01 attempt).** Superseded for Health Connect by **§36.11 DEVICE CERTIFIED — DEVELOP** (2026-10-03; CSRF §36.12; final lifecycle §36.13). Rows below remain the blocked-attempt record.

| Cell | Status (2026-10-01) |
| --- | --- |
| Build / device | **BLOCKED — DEVICE/PLATFORM ACCESS** (`adb devices` empty) |
| Health Connect availability | **Not exercised** |
| Permissions / deny / grant / revoke | **Not exercised** |
| Signal families | **CI VERIFIED ONLY**; device cells **NO TEST RECORD AVAILABLE** |
| Incremental / upload / idempotency / offline / disconnect / reconnect | **Not exercised** |
| **Certification** | **BLOCKED — ANDROID DEVICE/EMULATOR** (later closed in §36.11) |

Material docs note (not a runtime redesign): HC privacy-policy rationale intent currently maps to MainActivity (Play compliance gap for store later); Toolbox is a disposable cert writer only (not in CI path).

### 36.7 Cross-cutting (not device-exercised)

| Item | Result |
| --- | --- |
| One-provider enforcement | CI covered previously; not re-proven on device |
| Coach/org non-expansion | **HTTP coach-of-athlete IDOR test PASS** (Phase 0) |
| State Engine non-mutation | CI contracts unchanged; **no live device ingest** to re-prove |
| T23–T36 device paths | Applicable device rows **N/A this attempt** (access blocked); OAuth threats remain N/A for OS-hub V5 |
| Develop provider flags restored OFF | **Historical N/A for 2026-10-01 attempt.** After §36.11–§36.13 live cert, develop flags were **enabled** for ingest — **operator should restore `false`** per §36.9 #3 / §36.11 operator follow-up (not performed by docs agents) |

### 36.8 Classification (initial attempt)

**Historical classification (2026-10-01).** See **§36.11** for current Health Connect / D1 status.

- HealthKit: **BLOCKED — EAS LOGIN + PHYSICAL iOS DEVICE**
- Health Connect: **BLOCKED — ANDROID DEVICE/EMULATOR**
- D1 minimum (≥1 real certified connector): **not satisfied** (at this snapshot)
- V5A CI implementation on develop: **unchanged COMPLETE**
- `main` / production: **unchanged**

### 36.9 Resume attempt (2026-10-01 later)

| Check | Result |
| --- | --- |
| `eas whoami` | Still **Not logged in** → **EAS LOGIN STILL REQUIRED** (iOS stream STOP) |
| `eas.json` development profile | **Reused** (`developmentClient: true`, `distribution: internal`) — no profile change |
| Android AVD | **Pixel_7_API_34** started; `adb` **emulator-5554 device**; Android **14 / API 34**; `google_apis_playstore` |
| Health Connect package | `com.google.android.healthconnect.controller` **present** |
| Generated manifest READ_* | Sleep, RHR, HRV, Steps, ActiveCalories, Exercise — **read-only** (prebuild evidence) |
| Local Dev Client build | Prebuild OK; Gradle 9 hit `jcenter()` in `@react-native-cookies/cookies` → **pnpm patch** `jcenter`→`mavenCentral` |
| AssembleDebug after patch | **FAILED** — CMake/ninja `mkdir(CMakeFiles/…)` under deep Windows pnpm paths (`react-native-screens` / `react-native-worklets`) |
| Installable APK / HC UX journey | **Not achieved** |
| Backend flags | **Not toggled** — Railway Unauthorized; **BLOCKED — BACKEND CERT ENV** for live upload cells |
| HealthKit certification | **BLOCKED — EAS AUTH** |
| Health Connect certification | **BLOCKED — DEVICE** (emulator OK; installable build not produced on this host) |

**PO unblock (exact):**
1. `npx eas-cli@24.8.0 login` (interactive, local) — unblocks iOS **and** optional EAS Android cloud Dev Client (avoids local CMake).
2. Prefer EAS Android `build --platform android --profile development` while local Windows CMake remains broken; **or** fix local long-path/CMake env (outside this program without PO approval for toolchain surgery).
3. Railway **develop** only: set `UAP_INTEGRATIONS_ENABLED=true` and provider flags for E2E, then restore `false`.
4. Separate disposable develop athletes per platform (D7).

### 36.10 Classification (after resume)

**Historical classification (2026-10-01 resume).** Superseded by **§36.11** for Health Connect and D1.

- HealthKit: **BLOCKED — EAS AUTH**
- Health Connect: **BLOCKED — DEVICE** (AVD+HC available; Dev Client APK not installable here)
- Backend live cells: **BLOCKED — BACKEND CERT ENV**
- D1 minimum: **not satisfied** (at this snapshot)
- Runtime defect fix landed for Gradle 9: cookies `jcenter` patch (does not by itself certify either hub)
- `main` / production: **unchanged**

### 36.11 Health Connect Android — DEVICE CERTIFIED — DEVELOP (2026-10-03)

**Overall result:** **DEVICE CERTIFIED — DEVELOP**  
**Scope:** Android Health Connect connector on Railway **develop** only. Not production. Not HealthKit. Not “all OS hubs certified.” Not commercial activation.

| Item | Result |
| --- | --- |
| Environment | Railway **develop**; Android **14 / API 34**; **Pixel_7_API_34** emulator; EAS Android Development Client |
| Athlete app HC permissions | **READ-only** (Sleep, Resting heart rate, HRV, Steps, Active calories, Exercise) — no WRITE permissions added |
| Synthetic native data writer | Google Health Connect Toolbox `androidx.health.connect.client.devtool` **2.3.5** (disposable; outside Athlete Readiness) |
| Certified path | HC native records → native Android adapter → normalization → `CLIENT_DEVICE` evidence → Railway develop ingest → persistence → replay/idempotency → incremental sync → permission revoke → disconnect → reconnect |
| Provider | `HEALTH_CONNECT` |
| Evidence uniqueness | `(provider, athleteId, externalRecordId)` |
| READINESS_V1 | **Unchanged** (Home remained Not assessed; connected evidence did not autonomously generate readiness) |
| Manual check-in | Remained authoritative |

**Mapping observed on device:**

| Signal | Native HC type | UAP family / type | Provenance |
| --- | --- | --- | --- |
| Sleep | `SleepSession` | `SLEEP` / `DURATION` | `CLIENT_DEVICE` |
| Resting HR | `RestingHeartRate` | `HEART` / `RHR` | `CLIENT_DEVICE` |
| HRV | `HeartRateVariabilityRmssd` | `HRV` / `RMSSD` | `CLIENT_DEVICE` |
| Steps | `Steps` | `ACTIVITY` / `STEPS` | `CLIENT_DEVICE` |
| Active calories | `ActiveCaloriesBurned` | `ACTIVITY` / `ACTIVE_ENERGY` | `CLIENT_DEVICE` |
| Exercise | `ExerciseSession` | `WORKOUT` / mapped workout type | `CLIENT_DEVICE` |

**First non-zero sync (connect-time explicit sync):**

| Step | Result |
| --- | --- |
| disconnect | HTTP **200** |
| beginConnect | HTTP **201** |
| confirm | HTTP **200** |
| evidence-batches | HTTP **202** |
| sync | HTTP **200** |
| Evidence count | **0 → 8** |
| UI | lifecycle **CONNECTED**; data freshness **Recent** |
| Families observed | `SLEEP`, `HEART`, `HRV`, `ACTIVITY` (STEPS + ACTIVE_ENERGY), `WORKOUT` |
| Provenance / IDs | All `CLIENT_DEVICE`; `externalRecordId` populated from HC metadata IDs |

**Replay / idempotency:** repeated connect-time sync; evidence-batches accepted; evidence count remained **8**; no duplicate persisted rows.

**Incremental (Set B synthetic):** Steps `9001`, RHR `61`, ExerciseSession `walking` → batch accepted, sync successful, evidence **8 → 11**, prior IDs retained, `lastSuccessfulSyncAt` advanced.

**Permission revoke:** Android Health Connect “Remove all permissions” → reconnect showed permission sheet → cancel → lifecycle **PENDING**; no new evidence; historical **11** retained.

**Disconnect:** lifecycle **DISCONNECTED**; Connected Apps **Not connected**; no sync while disconnected; historical evidence retained (**11**).

**Disconnected ingest protection:** no fabricated stale mobile request was sent in this cert. Automated contract `disconnectedRejectsIngest` → HTTP **409** / `INTEGRATION_INGEST_NOT_ACCEPTED`.

**Reconnect:** permissions re-granted via HC Manage Permissions UI → reconnect succeeded → **CONNECTED**; evidence upload accepted; persisted count remained **11** (no duplication).

#### D1 acceptance (precise)

**D1 is satisfied** by this certification because D1 requires: foundation + **at least one** real certified connected-health connector + minimal Connected Apps UX.

- Satisfying connector: **Android Health Connect** (**DEVICE CERTIFIED — DEVELOP**).
- D1 does **not** mean every provider is certified.
- **HealthKit** remains **NOT DEVICE CERTIFIED**.
- D3 preferred HealthKit-first sequencing; device/access reality made Health Connect the first **device-certified** hub. That does not revoke D1 once one real connector is certified.

#### Canonical status wording (use these labels)

| Surface | Status |
| --- | --- |
| V5A implementation | **COMPLETE / CI VERIFIED** |
| Health Connect Android | **DEVICE CERTIFIED — DEVELOP** |
| V5 MVP device gate (D1) | **SATISFIED** (HC Android is the certified connector) |
| HealthKit | **IMPLEMENTED / CI VERIFIED**; **DEVICE CERTIFICATION PENDING** (**NOT DEVICE CERTIFIED**) |
| Production | **NOT VERIFIED / NOT PROMOTED** |
| V5 “fully complete” / all OS hubs / commercial activation | **Do not claim** |

Do **not** collapse: **IMPLEMENTATION COMPLETE** ≠ **CI VERIFIED** ≠ **DEVICE CERTIFIED — DEVELOP** ≠ **PRODUCTION VERIFIED**.

#### Non-blocking UX follow-up (do not implement in cert close-out)

Connected Apps mobile UI does not yet expose the existing explicit sync mutation while CONNECTED; current device certification used connect/reconnect-triggered explicit sync. Track visible Sync action as a non-blocking UX follow-up. (`useRequestConnectionSyncMutation` exists but is unwired to a visible control.)

#### Operator follow-up — develop feature flags (do not change Railway from this docs task)

Canonical post-E2E guidance (§36.9 PO unblock #3): on Railway **develop** only, enable `UAP_INTEGRATIONS_ENABLED` and the Health Connect provider flag for certification, then **restore both to `false`** when the cert window ends.

- Certification required develop flags **on** for live ingest.
- After HC **DEVICE CERTIFIED — DEVELOP**, restoring develop flags to **false** is the documented default-off posture (fail-closed when not intentionally certifying).
- **Operator action required** if flags are still `true` on develop — this documentation update does **not** mutate Railway.
- Production flags remain **untouched**. Billing / entitlement / capacity flags remain **untouched**.

#### Explicit non-claims for this close-out

- V5 is **not** fully complete.
- All OS hubs are **not** certified.
- HealthKit is **not** device certified.
- Production is **not** verified and **not** promoted.
- Connected evidence does **not** affect `READINESS_V1`.
- Commercial activation did **not** occur.
- No Athlete Readiness Health Connect WRITE permissions were added.
- Runtime connector code was **not** changed for the original §36.11 close-out (CSRF mobile fixes landed separately and were revalidated in §36.12).

### 36.12 Health Connect CSRF revalidation + matrix confirmation (2026-10-03, tip)

**Purpose:** Re-prove protected writes after native CSRF cookie/header fixes (`3a1878d`, `c839ab1`) on develop tip (documentation close-out recorded against tip including `5cc9ece`). No production action. No HealthKit exercise. No new Athlete Readiness WRITE permissions.

| Item | Result |
| --- | --- |
| Environment | Railway **develop**; Android **14 / API 34**; **Pixel_7_API_34**; EAS Development Client; Metro JS delivery (no new EAS build required for CSRF/token JS) |
| CSRF presence diagnostics | Session access cookie **present**; anti-forgery cookie **present**; `antiForgeryHeader` = **attached** (values not logged) |
| beginConnect | `POST /api/v1/integrations/connections` → **201** |
| Protected writes | confirm **200**; evidence-batches **202**; sync **200**; disconnect **200** |
| Permission deny | Cancel on OS sheet → lifecycle **PENDING**; no crash; no CONNECTED; no evidence upload |
| Permission grant | → CONNECTED; freshness **Recent**; connect-time explicit sync |
| 30-day backfill | OS sheet states past-30-day read; connect-time sync executed |
| Signal families | Sleep, RHR, HRV RMSSD, Steps, Active calories, Exercise — **PASS** (Exercise: READ path + previously retained synthetic data; do **not** overstate a newly inserted Exercise sample in this exact revalidation session) |
| Idempotent / repeat | disconnect→reconnect sync succeeded without duplicate-failure behavior |
| Incremental | synthetic HC inserts (Toolbox); `lastSuccessfulSyncAt` advanced (~3:16 PM → ~3:23/3:24 PM local) |
| Manual check-in | remained outstanding / “Next”; HC did not auto-complete check-in |
| READINESS_V1 | **UNCHANGED** — Home remained **Not assessed** |
| Permission revoke | revoke + re-prompt Cancel → **PENDING** |
| Disconnect / reconnect | disconnect **200** → Not connected; reconnect → CONNECTED + sync |
| Disconnected ingest | contract `disconnectedRejectsIngest` → **409** / `INTEGRATION_INGEST_NOT_ACCEPTED` (no fabricated stale client request solely for this revalidation) |
| Defect / commit from revalidation pass | **None** — report-only; CSRF fixes already on develop |

**V5 MVP device gate:** remains **SATISFIED** (D1). HealthKit device certification remains **pending** and is **not** required to keep the MVP device gate satisfied.

**Locked decisions preserved:** D1–D14; V5A store/map/display only; no readiness formula change; explicit user sync only; 30-day default backfill; one active hub; disconnect stops sync / retains normalized history; no coach raw console; Connected Athlete FREE in V5; OS hubs only.

### 36.13 Health Connect Android — final lifecycle closeout (2026-10-04, tip `b3b4620`)

**Overall result:** Health Connect Android remains **DEVICE CERTIFIED — DEVELOP**.  
**V5 MVP device gate (D1):** remains **SATISFIED**.  
**Purpose:** Record the post-CSRF end-to-end grant → sync → revoke → disconnect → reconnect certification on tip `b3b46204462ed1f6f462825c4640b76227b57289` with GitHub Verify [37174694721](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/37174694721) **SUCCESS**. No runtime code change. No new EAS build. No production action. No HealthKit exercise.

| Item | Result |
| --- | --- |
| Environment | Railway **develop** healthy; Android **14 / API 34**; **Pixel_7_API_34**; EAS Development Client; native `react-native-health-connect` |
| Stable connection id | `1a3bb468-ff49-45b8-aba7-2da317e0b2f0` across retry / disconnect / reconnect |
| Retry-from-PENDING | **PASS** — PENDING hides Connect; retry = Disconnect → Connect; identity stable |
| Permission sheet | **PASS** — observed after revoke/reconnect and Manage Permissions grant path |
| Permission scope | **PASS** — READ-only Sleep, Resting HR, HRV, Steps, Active calories, Exercise; no WRITE; OS sheet described past **30 days** |
| CONNECTED lifecycle | **PASS** — `beginConnect` **201** → `confirm` **200** → UI Connected |
| Initial sync | **PASS** — `evidence-batches` **202**; `sync` **200**; freshness **Recent**; `lastSuccessfulSyncAt` populated |
| Backfill | **PASS** — `DEFAULT_BACKFILL_DAYS = 30`; explicit connect-time sync only; no server scheduler |
| SyncRun | **PASS** — `POST /sync` **200**; OS hub upload-only; evidence-batch is active ingest; `lastSuccessfulSyncAt` advances |
| Idempotency / replay | **PASS** — disconnect/reconnect accepted; same connection id; no duplicate failure |
| Incremental sync | **PASS** — synthetic Health Connect Toolbox inserts; reconnect/sync advanced last successful sync |
| Deduplication | **PASS** — stable external record identity; replayed batches accepted without duplicate failure |
| READINESS_V1 | **UNCHANGED** — Not assessed before/after; no readiness improvement or formula change claimed |
| Manual check-in | **PASS** — remained outstanding / authoritative; HC did not auto-complete |
| Permission revoke | **PASS** — Remove all → sheet → Cancel → **PENDING**; no confirm/evidence after cancel |
| Disconnect | **PASS** — HTTP **200** → Not connected; freshness Unavailable |
| Stale upload rejection | **PASS** — contract `disconnectedRejectsIngest` → HTTP **409** / `INTEGRATION_INGEST_NOT_ACCEPTED` |
| Reconnect | **PASS** — Allow-all → CONNECTED; confirm **200**; evidence-batches **202**; sync **200**; same connection id |
| Runtime defects | **None** blocking |
| Code / EAS | No runtime code changed during this cert; no new EAS build required |

**Signal matrix (device):**

| Family | Classification | Nuance |
| --- | --- | --- |
| Steps | **DEVICE VERIFIED** | Toolbox synthetic + sync |
| Resting heart rate | **DEVICE VERIFIED** | Toolbox synthetic + sync |
| HRV RMSSD | **DEVICE VERIFIED** | Toolbox synthetic + sync |
| Active calories | **DEVICE VERIFIED** | Toolbox synthetic + sync |
| Sleep | **DEVICE VERIFIED** | Prior retained / non-empty connect-time evidence; a **new** Sleep insertion was **not** independently logcat-confirmed in this exact session |
| Exercise | **DEVICE VERIFIED** | READ path + retained data verified; a **new** ExerciseSession insertion was **not** completed in this exact session |

Do **not** overstate Sleep/Exercise as newly inserted samples in this exact session.

**Non-blocking UX observations (not blockers):**

- No visible manual Sync control while CONNECTED (connect/reconnect-triggered explicit sync remains the certified path).
- PENDING has no Connect button; current retry is Disconnect → Connect.

**Canonical status (unchanged labels):**

| Surface | Status |
| --- | --- |
| Health Connect Android | **DEVICE CERTIFIED — DEVELOP** |
| V5 MVP device gate (D1) | **SATISFIED** |
| HealthKit | **PENDING / NOT DEVICE CERTIFIED** (**IMPLEMENTED / CI VERIFIED**; physical iOS + EAS path still required) |
| Production / both OS hubs / commercial activation | **Do not claim** |

**Architecture preserved:** V5A store/map/display only; READINESS_V1 unchanged; manual check-in authoritative; explicit sync only; 30-day default backfill; one active provider/hub; normalized evidence retained after disconnect; credentials/authorization removed on disconnect; stale upload after disconnect rejected.

**Operator follow-up:** if develop integration flags are still `true` after this cert window, restore `UAP_INTEGRATIONS_ENABLED` and Health Connect provider flag to **`false`** (docs agents do not mutate Railway). Production remains untouched.
