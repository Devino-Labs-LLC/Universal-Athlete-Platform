# Athlete Readiness V4 — Implementation Plan

**Theme:** Commercialization / Billing / Entitlements  
**Product question:** Can Athlete Readiness become a commercially operable SaaS product where individuals and organizations can pay through an appropriate billing channel, receive provider-neutral entitlements, manage subscriptions safely, and retain access across Web/iOS/Android without billing logic contaminating the product domain?

**Document type:** Product Owner decision lock (docs)  
**Planning commit:** `117b37ef95c142daa323da021cc8172565b56803`  
**Production baseline (`main`):** `5b714f787b690acda8240402d26fe955577b2fc8`  
**Production schema:** Flyway **V34**  
**Prior version:** Athlete Readiness V3 — **COMPLETE — PRODUCTION VERIFIED**  
**§22 lock status:** **COMPLETE** (ADR-036–045 Accepted)

**This document does not authorize V4 Slice A or any runtime implementation.** Slice A still requires explicit implementation authorization.

---

## 0. Agents consulted

Lead / Architect coordinated planning and consulted:

| Role | Contribution |
| --- | --- |
| Lead / Architect | Bounded context, entitlement seam, slice order, ADR set, PO decision lock |
| Backend | Modulith placement, ports, webhook/idempotency, fail-fast config |
| Web | Pricing/billing IA under coach/org shell; athlete Profile subscription surfaces |
| Mobile | Individual IAP recognition only if PO includes individual monetization; no coach billing console |
| External Integration | Stripe Billing + Checkout + Portal; Apple/Google server validation seams; no Connect |
| Security / Code Quality | Commercial threat model; Sonar maintainability debt strategy; campground |
| QA / Test Automation | Entitlement matrix, webhook replay, seat races, duplicate-subscription tests |
| DevOps / CI-CD | Sandbox-first secrets; Verify inclusion later; restore full `develop` Sonar scope |
| Documentation / Release | This plan + ADRs (Accepted at §22 lock) |
| Athlete Intelligence / Data | Billing must not contaminate State Engine, readiness math, Team Readiness semantics, or consent |

Stripe guidance consulted for ordinary SaaS subscriptions (Checkout `mode=subscription`, Customer Portal, signature-verified idempotent webhooks). **Stripe Connect is out of scope** unless a multi-party payout requirement appears later (Athlete Readiness is not a marketplace paying third-party sellers).

---

## 1. Repository findings (current reality)

| Finding | Evidence |
| --- | --- |
| **No billing runtime** | No Stripe / Apple IAP / Google Play / RevenueCat / entitlement modules, DTOs, or config |
| V3 deferred V4 | `docs/V3_IMPLEMENTATION_PLAN.md` §19 — org may later link billing account id; **do not implement** in V3 |
| Modulith modules | `identity`, `athlete`, `organization`, `consent`, `training`, `audit` |
| Org roles | `ATHLETE`, `COACH`, `HEAD_COACH`, `TEAM_ADMIN`, `ORG_ADMIN`, `ORG_OWNER` |
| Org manage capability today | Invitation/admin ops: ORG_ADMIN or ORG_OWNER; **V4 billing authority locked: ORG_OWNER only** (§22.1 #5) |
| Account / Athlete | One Account; ≤1 Athlete; coach-only accounts OK (ADR-030) |
| Audit | Durable append-only `security_audit_events` (V34 / ADR-035); adapters in org/consent/training |
| Web IA | Athlete `/app/*`; coach sibling `/coach/*`; no billing routes |
| Mobile IA | Athlete-first tabs; no coach console |
| Config pattern | Fail-fast required secrets; no silent prod/sandbox fallbacks |
| Highest accepted ADR | **ADR-045** (V4 commercialization set 036–045 Accepted) |

Internal names remain **UAP**. Commercial name remains **Athlete Readiness**. No repository-wide rename.

---

## 2. Locked architectural principle (unless evidence later contradicts)

**Billing provider can vary. Entitlement cannot.**

```text
Stripe / Apple / Google
        ↓
Provider subscription state (adapter)
        ↓
billing bounded context
        ↓
Entitlements (provider-neutral)
        ↓
Product capability checks
```

Product code must **not** scatter:

- `isStripeCustomer`
- `hasAppleSubscription`
- `googlePlayActive`

Product features ask:

> Does this account or organization currently possess entitlement **X**?

Provider adapters answer **how** commercial state is synchronized.

---

## 3. Two customer types (explicit separation)

### 3.1 Organization subscription

| Item | Locked |
| --- | --- |
| Customer | School, club, team organization, other Organization account |
| Channel | **Stripe / Web** only |
| Not forced through | Apple App Store or Google Play billing |
| Pricing shape | Fixed **active-athlete bands** (architecture supports up to 25 / 75 / 250); dollar amounts locked before Slice B sandbox catalog — **not invented here** |
| Cadence | Monthly + annual Price variants of the same tier/entitlement |
| Trial | 14-day org trial; payment method required up front |
| Billing authority | **ORG_OWNER only** |

### 3.2 Individual athlete subscription

| Item | Locked |
| --- | --- |
| In V4 | **Yes** — Slice **G** (later); org slices A–F must not depend on Apple/Google implementation |
| Customer | Individual Athlete Readiness Account |
| Providers | `STRIPE` (Web), `APPLE_APP_STORE`, `GOOGLE_PLAY` |
| Cross-client rule | One valid individual entitlement → same Premium capability on **Web + iOS + Android** after sign-in |
| Tier | Internal **PREMIUM**; commercial targets $9.99/mo and $99.99/yr (catalog creation not authorized by this lock) |
| Trial | **No** individual trial in initial V4 |
| Separation | **Billing owner** ≠ **entitlement** |

---

## 4. Recommended billing bounded context

**Name:** `billing` (prefer over `commercial` — domain is payment + entitlement derivation, not a broader CRM).

| Owns | Does not own |
| --- | --- |
| Commercial customer mapping (Account/Org ↔ provider customer refs) | Organization identity, Teams, membership authority graph |
| Subscription state (canonical internal lifecycle) | Consent scopes |
| Provider references (customer/subscription/price IDs) | State Engine / readiness |
| Plan/tier identity (internal catalog keys → provider Price IDs) | TrainingPlan ownership |
| Entitlement derivation / effective capabilities | UI design system |
| Billing lifecycle (checkout, renew, cancel, grace, switch) | Arbitrary athlete wellness storage |

**References only:** Organization ID and Account ID as UUIDs/value objects. **No shared JPA entities** across modules. Follow existing Modulith style (`organization :: membership`, `consent :: grants`, `audit :: writer`).

Proposed published interfaces (names illustrative):

- `billing :: entitlements` — `EntitlementPort` / `hasCapability(subject, capability)`
- `billing :: writer` or reuse `audit :: writer` for commercial security events
- Internal `BillingProviderPort` — Stripe / Apple / Google adapters

Organization must **not** become a billing god-module.

---

## 5. Provider abstraction

Minimal shared concepts across Stripe / Apple / Google:

| Concept | Purpose |
| --- | --- |
| `BillingProvider` | `STRIPE` \| `APPLE_APP_STORE` \| `GOOGLE_PLAY` |
| External customer / account reference | Provider-side payer identity |
| External subscription reference | Provider subscription/purchase id |
| External product/price reference | Catalog mapping |
| Subscription status (provider-native) | Mapped into internal lifecycle |
| Paid-through / current period end | Access until date |
| Cancellation state | Cancel-at-period-end vs ended |
| Provider event identity | Idempotency |

Do **not** build a universal payment framework. Do **not** store card/bank credentials. Prefer on-demand Stripe invoice fetch over duplicating financial documents.

---

## 6. Stripe architecture (Web / Organization — and Stripe individual if approved)

### 6.1 Components

| Component | Role |
| --- | --- |
| Stripe Billing | Recurring subscription engine |
| Checkout Sessions (`mode=subscription`) | Hosted acquisition |
| Customer Portal | Self-service payment method, cancel, plan change, invoices |
| Webhooks | Authoritative commercial state sync |

**Do not:**

- Build renewals with PaymentIntents
- Trust client-supplied dollar amounts or Price IDs without server allow-list
- Treat success URL redirect as fulfillment source of truth

**Server chooses** the canonical Stripe Price from configuration.

### 6.2 Candidate webhook events (confirm against current Stripe API at implementation)

| Event | Typical action |
| --- | --- |
| `checkout.session.completed` | Link Account/Org ↔ Customer; start sync |
| `checkout.session.async_payment_succeeded` / `_failed` | Async methods |
| `customer.subscription.created` / `updated` / `deleted` | Sync status, cancel flags, period end |
| `invoice.paid` | Extend paid-through; confirm ACTIVE |
| `invoice.payment_failed` | PAST_DUE / grace messaging |
| `customer.subscription.trial_will_end` | Only if trials exist |

Mandatory: **signature verification**, **idempotent** processing by `event.id`, **refetch** subscription from Stripe when resolving races, reject stale out-of-order updates using provider timestamps/version where available.

### 6.3 Catalog (plan only — do not create in Stripe now)

Prefer **one Product per commercial tier**, separate **Prices** for monthly/annual/currency.

Example structure only (names/prices **not locked**):

```text
Athlete Readiness Starter
  monthly Price / annual Price
Athlete Readiness Team
  monthly / annual
Athlete Readiness Organization
  monthly / annual
```

Sandbox/test first. Live Products/Prices only after V4 gates. Separate sandbox vs live IDs and webhook secrets. **Fail fast** if environment mismatch.

---

## 7. Apple / Google individual billing (locked for Slice G)

Individual monetization is **in V4** (Slice **G**). Organization slices A–F must not depend on Apple/Google adapters.

| Platform | Approach |
| --- | --- |
| iOS | Apple auto-renewable subscription; **server-side** validation/sync (App Store Server API / notifications) |
| Android | Google Play recurring subscription; **server-side** validation/sync (Play Developer API / RTDN) |

Mobile clients must not be the sole trust root for entitlement. Map provider notifications into the same internal lifecycle + entitlements (Premium).

---

## 8. Cross-platform entitlement UX (locked)

| Origin | Web | iOS | Android |
| --- | --- | --- | --- |
| Stripe | ✅ | ✅ | ✅ |
| Apple | ✅ | ✅ | ✅ |
| Google | ✅ | ✅ | ✅ |

Billing continues with the **originating** provider until cancel/expire/explicit switch. Do **not** copy payment credentials across providers.

---

## 9. Duplicate subscription prevention & provider switch (locked)

### 9.1 Prevention

If Account already has **ACTIVE** or **grace-valid** individual entitlement from any provider:

- Show paid/Premium state  
- Identify billing provider  
- **Do not** present normal Stripe/Apple/Google duplicate checkout  
- Direct management to the correct provider channel  

### 9.2 Explicit switch

1. Cancel/stop renewals on current provider  
2. Retain entitlement until paid-through  
3. Begin replacement provider **after** the existing paid period  
4. Never silent migrate; never automatic double subscription  

---

## 10. Tax architecture (channel-specific — locked)

| Channel | Locked stance |
| --- | --- |
| Stripe / Web | Stripe Tax is the intended Web tax-calculation integration. Advertised pricing is **tax-exclusive** where applicable (`price + applicable taxes`). Enable for **live** charging only after Devino Labs tax registrations + product tax classification are reviewed/configured. |
| Apple | App Store tax category; Apple owns storefront tax handling per its rules — **do not** run Apple renewals through Stripe Tax |
| Google | Play product/tax classification; provider jurisdiction rules — **do not** double-tax via Stripe |

**Hard rule:** `automatic_tax.enabled = true` is **not** proof Devino Labs is registered everywhere. Registration ≠ calculation.

Do **not** add `athlete.salesTax` or similar domain booleans.

Planning documents **who is merchant / billing processor / tax handler by channel** without unsupported legal claims. Legal/tax counsel remains a launch dependency for live tax collection.

---

## 11. Entitlement model (locked principles)

Provider-neutral capabilities:

| Subject | Locked direction |
| --- | --- |
| Always free (when authorized) | Auth/account; invite accept/decline; leave Team; consent grant/revoke/re-grant; transparency/activity; athlete-owned retained history per existing contracts |
| Organization gated (candidates) | Active Team management; coach collaboration; consent-aware coach product views; Team Readiness — final matrix in Slice A/C |
| Individual | **PREMIUM** = richer/advanced individual capability; must not arbitrarily remove basic athlete functionality — final matrix in Slice A/C |

Product checks use `EntitlementPort`; never provider identity.

---

## 12. Authorization vs entitlement (HARD)

| Concern | Answers | Owner |
| --- | --- | --- |
| Authorization (V3) | **Who** may act (membership, role, consent, IDOR) | organization / consent / training |
| Entitlement (V4) | **Whether** the commercial subject purchased the capability | billing |

Both may be required. Neither replaces the other.

Examples:

- Valid COACH + consent ≠ org still entitled to a paid coach-suite capability  
- Paid Organization ≠ every Account is a coach  

---

## 13. Individual vs organization entitlement precedence

**Locked principle:** effective capability = **safe union** of currently valid entitlements, unless a capability has explicit exclusive semantics.

| Event | Effect |
| --- | --- |
| Athlete loses org membership | Lose **organization-provided** commercial access; **keep** personal Premium |
| Personal subscription cancels/expires | Lose personal capabilities; **keep** valid org-granted access |
| Both active | Union |

---

## 14. Active-athlete band model (locked — ADR-041)

**Not** usage-metered/per-athlete monthly billing.

| Rule | Locked |
| --- | --- |
| Shape | Fixed bands (architecture supports **up to 25 / 75 / 250**) |
| Dollar prices | Commercial catalog lock **before** Slice B sandbox Product/Price creation — do not invent prices in code or ADRs |
| Billable unit | One **ACTIVE** Athlete identity **once per Organization** (multi-Team does not multiply) |
| Not billable | Pending invitation, LEFT, REMOVED |
| Cross-org | Independent count per Organization |
| Downgrade | Cannot take effect while count exceeds target band; ORG_OWNER remediates; never auto-delete athletes |

Billing disputes must resolve from deterministic domain data.

---

## 15. Canonical internal subscription lifecycle (locked — ADR-040)

Do not mirror every provider status into product code.

| Internal state | Meaning |
| --- | --- |
| `PENDING` | Checkout/purchase started; not yet entitled |
| `TRIALING` | Organization 14-day trial (payment method on file); entitled |
| `ACTIVE` | Paid entitled |
| `PAST_DUE` | Payment failed; within or entering grace accounting |
| `GRACE_PERIOD` | Explicit **7 calendar day** failed-payment grace; entitled capabilities remain; billing-owner messaging |
| `CANCEL_AT_PERIOD_END` | Cancel scheduled; entitled until paid-through / period end |
| `EXPIRED` | Period ended without renewal / after grace without recovery |
| `CANCELLED` | Terminal cancel path after period end (keep distinct from EXPIRED only if ops need it; prefer minimal set) |

**Primary happy paths:**

```text
PENDING → TRIALING → ACTIVE → …
PENDING → ACTIVE → …          (no trial / individual)
ACTIVE | TRIALING → CANCEL_AT_PERIOD_END → EXPIRED
ACTIVE → PAST_DUE / GRACE_PERIOD → ACTIVE | EXPIRED
```

Provider-status mapping tables live in adapters (ADR-044).

---

## 16. Grace / cancellation / downgrade (locked)

| Topic | Locked behavior |
| --- | --- |
| Failed payment | **7 calendar days** grace; normal entitled capability remains; billing-owner messaging; then restrict **commercial-gated** capabilities only |
| Never on billing failure | Delete memberships, consent, athlete data, readiness, training history, or audit |
| Cancellation | **Cancel at period end**; entitlements through paid-through; reactivation before expiry supported |
| Downgrade over limit | **Cannot become effective** while active billable athletes exceed target band; ORG_OWNER must remediate first; never auto-delete/remove athletes |
| Data retention | Billing end ≠ delete product data |

---

## 17. Trials / cadence / promotions / enterprise (locked)

| Topic | Locked |
| --- | --- |
| Org trial | **14 days**; payment method required up front |
| Individual trial | **None** in initial V4 |
| Cadence | **Monthly + annual** (Price variants; same entitlement) |
| Coupons/promotions | **Deferred** beyond initial V4 |
| Enterprise/custom invoicing | **Deferred**; initial org path is self-service Stripe Billing |

---

## 18. Webhook / provider event model

Persist minimum:

- provider event id (unique)  
- provider + type  
- receivedAt / processedAt  
- linked subscription/customer refs  
- processing outcome  

Optional: bounded evidence blob only if operations require it — **default: do not store arbitrary raw payloads**.

Requirements: signature verify, idempotency, replay-safe, out-of-order resistant, reconciliation job later if needed.

---

## 19. Commercial audit

Reuse ADR-035 durable audit via `audit :: writer`.

| Candidate event types | Notes |
| --- | --- |
| `BILLING_CHECKOUT_INITIATED` | Actor + org/account + plan key — no Price secrets |
| `BILLING_SUBSCRIPTION_ACTIVATED` | |
| `BILLING_PLAN_CHANGED` | |
| `BILLING_CANCEL_REQUESTED` | |
| `BILLING_SUBSCRIPTION_ENDED` | |
| `BILLING_PROVIDER_SWITCHED` | |
| `BILLING_ENTITLEMENT_CHANGED` | When material |

Never audit: card data, full sensitive webhook bodies, secrets, tokens.

Separate: security audit ≠ analytics ≠ provider event store.

---

## 20. V4 commercial threat model (T12+)

Continue V3 T1–T11. Add commercial threats:

| ID | Threat | Mitigation direction |
| --- | --- | --- |
| T12 | Forged Checkout Price ID / client amount | Server allow-listed Price IDs only |
| T13 | Forged / replayed webhook | Signature + event-id idempotency |
| T14 | Out-of-order webhook | Refetch + timestamp/version guards |
| T15 | Cross-org billing IDOR | AuthZ on org billing like V3 membership |
| T16 | Unauthorized cancel/upgrade | Billing authority matrix (PO) |
| T17 | Stale entitlement after cancel | Webhook + paid-through enforcement |
| T18 | Seat-limit race | Transactional seat accounting |
| T19 | Duplicate Stripe/Apple/Google sub | Pre-checkout entitlement check |
| T20 | Fake App Store / Play tokens | Server validation APIs |
| T21 | Provider↔Account mismatch | Bind purchases to authenticated Account |
| T22 | Coupon/tax client manipulation | Server-side Stripe Tax / portal rules |

Deny by default.

---

## 21. Environments, secrets, UI/IA

### 21.1 Config (fail-fast)

| Secret / config | Location |
| --- | --- |
| Stripe restricted secret key | Server only |
| Stripe webhook secret | Server only |
| Product/Price IDs (sandbox vs live) | Server config |
| Portal configuration | Server |
| Apple / Google validation credentials | Server only |
| Publishable key | Client OK where required |

No silent live↔sandbox fallback.

### 21.2 UI plan (no implementation)

| Surface | Placement |
| --- | --- |
| Public pricing | Marketing / logged-out Web |
| Organization Billing | Coach/org admin area under `/coach` or `/org/billing` — **not** athlete Home |
| Individual subscription | Athlete Profile / dedicated billing page Web; Mobile premium state if in scope |
| Manage billing | Stripe Portal redirect; Apple/Google manage links by provider |

Athlete Home must not become a billing dashboard. Mobile coach billing console remains out (coach console is Web).

---

## 22. Product Owner decision lock (normative)

**Status:** COMPLETE  
**Recorded from Product Owner authorization for V4 commercialization.**

### 22.1 Approved decisions

| # | Topic | Decision |
| --- | --- | --- |
| 1 | Scope | **Organizations + Individuals.** Org = Stripe/Web. Individual = Stripe Web and/or Apple and/or Google → same provider-neutral entitlement across Web/iOS/Android. Individual work is **Slice G**; slices A–F must not depend on Apple/Google adapters. |
| 2 | Org pricing | **Fixed active-athlete bands** (not usage-metered/per-athlete monthly). Architecture supports bands such as **up to 25 / 75 / 250**. Exact org **dollar** prices are a commercial catalog lock **before** Slice B sandbox Product/Price creation — **do not invent prices** in code or ADRs. |
| 3 | Billable athlete | One **ACTIVE** Athlete identity counts **once per Organization** regardless of Team count. Pending invitation / LEFT / REMOVED = **not** billable. Same athlete in separate Orgs counts independently. |
| 4 | Free vs entitled | Billing **never** gates privacy/account-control rights. Always available when otherwise authorized: authentication/account access; invitation accept/decline; leave Team; consent grant/revoke/re-grant; athlete transparency/activity; access to athlete-owned retained data/history per existing contracts. Org commercial entitlement gates paid org/coach capabilities (candidates: active Team management, coach collaboration, consent-aware coach views, Team Readiness). V3 authZ + consent remain independent. Individual **PREMIUM** must not arbitrarily remove basic athlete functionality; final capability matrix defined in **Slice A/C** before enforcement. |
| 5 | Billing authority | **ORG_OWNER only** may initiate/manage/cancel/change Organization billing. ORG_ADMIN does **not** gain financial authority from operational authority. |
| 6 | Cadence | **Monthly + annual** as Price variants of the same commercial tier/entitlement. |
| 7 | Trial | Org: **14-day** trial; **payment method required up front**. Individual: **no** Stripe/Apple/Google trial in initial V4. |
| 8 | Failed-payment grace | **7 calendar days.** During grace: entitled capability remains + billing-owner messaging. After grace: restrict commercial-gated capabilities only. **Never** delete memberships, consent, athlete data, readiness, training history, or audit because billing failed. |
| 9 | Cancellation | **Cancel at period end.** Entitlements through paid-through/current period end. Reactivation before expiry supported. Cancellation does not delete product data. |
| 10 | Downgrade over limit | Downgrade **cannot become effective** while active usage exceeds target band. ORG_OWNER must remediate first. Never auto-delete/remove athletes or memberships. |
| 11 | Individual tier | Internal **PREMIUM**. Launch **targets** (not live-catalog authorization): **$9.99/month**, **$99.99/year**. Apple/Google use storefront/localized tiers resolving to the same Premium entitlement. |
| 12 | Provider switching | One individual provider owns billing at a time. Block normal duplicate checkout while another ACTIVE or grace-valid subscription exists. Switch: cancel/stop renewal → retain through paid-through → begin replacement after existing paid period. No silent migration; no automatic double subscription. |
| 13 | Tax display | Stripe/Web advertised pricing is **tax-exclusive** where applicable (`price + applicable taxes`). Apple/Google follow storefront-required presentation. Do not force one display convention across providers. |
| 14 | Stripe Tax | Intended Web tax-calculation integration. Enable for **live** charging only after Devino Labs tax registrations + product tax classification are reviewed/configured. `automatic_tax.enabled` ≠ registration. Never double-apply Stripe Tax to Apple/Google. No unsupported legal/tax claims. |
| 15 | Promotions | Coupons/promotion codes **deferred** beyond initial V4. |
| 16 | Enterprise | Enterprise/custom contract invoicing **deferred**. Initial org commercialization = self-service Stripe Billing. |

### 22.2 Pre-Slice-A normative contract

| Contract item | Locked value |
| --- | --- |
| Bounded context | `billing` module; UUID refs only to Account/Organization; no shared JPA with org/consent/training |
| Entitlement principle | Provider-neutral capabilities; product asks `has(capability)` |
| AuthZ × entitlement | Authorization = WHO; entitlement = WHETHER purchased; both may be required; neither replaces the other |
| Org channel | Stripe Billing + Checkout + Customer Portal |
| Individual channels | Stripe Web, Apple App Store, Google Play (Slice G) |
| Org pricing shape | Fixed active-athlete bands (25 / 75 / 250 supported); dollar amounts deferred to pre–Slice B catalog lock |
| Billable athlete | ACTIVE once per Organization; invite/LEFT/REMOVED not billable |
| Org billing role | ORG_OWNER only |
| Cadence | Monthly + annual Price variants |
| Org trial | 14 days; payment method up front |
| Individual trial | None (initial V4) |
| Grace | 7 calendar days; then restrict commercial-gated only |
| Cancel | At period end; paid-through access; reactivation OK |
| Downgrade | Block effective downgrade until usage ≤ band |
| Individual product | PREMIUM entitlement |
| Free surfaces | Account/privacy/consent/invite/leave/transparency/retained athlete history (when authorized) — never paywalled |
| Lifecycle states | `PENDING`, `TRIALING`, `ACTIVE`, `PAST_DUE`, `GRACE_PERIOD`, `CANCEL_AT_PERIOD_END`, `EXPIRED` (+ `CANCELLED` only if needed) |
| Tax | Channel-specific; Stripe/Web tax-exclusive display; Stripe Tax after registration review |
| Promotions / enterprise | Deferred |
| Connect | Out of scope |
| V1–V3 invariants | Membership, consent, IDOR, Team Readiness privacy, State Engine ownership, no-hidden-write **untouched** |
| Maintainability | Campground on touched paths only; no mass smell cleanup in lock/Slice A docs |

### 22.3 Slice A readiness

| Role | Verdict |
| --- | --- |
| Lead / Architect | **Ready** — §22 locked; ADR-036–045 Accepted; A–I order final |
| Backend / External Integration | **Ready** to implement after **explicit Slice A authorization** |
| Web / Mobile / QA / Security / AI / DevOps / Docs | Planning lock sufficient; no runtime in this task |

**Remaining blocker before Slice A runtime:** explicit user authorization to implement code/migrations — **not** an open PO decision.

---

## 23. Final V4 slices (A–I)

| Slice | Theme |
| --- | --- |
| **A** | Commercial foundation — `billing` module, ports, entitlement model, catalog keys (no live Stripe mutation unless separately authorized) |
| **B** | Stripe Organization subscription — sandbox catalog after price lock, Checkout, customer mapping, lifecycle sync |
| **C** | Entitlements — server-side commercial capability enforcement; finalize free vs gated matrix |
| **D** | Bands & usage — active-athlete band enforcement |
| **E** | Billing management — Customer Portal, upgrade/downgrade/cancel/reactivate |
| **F** | Webhooks / dunning / reconciliation — events, 7-day grace, recovery |
| **G** | Individual monetization — Stripe Web + Apple + Google → Premium |
| **H** | Commercial UX completion — pricing/billing cohesion |
| **I** | Hardening / RC — T12–T22, entitlement matrix, tax/config, production certification |

**Do not begin Slice A from this decision-lock task.**

---

## 24. ADRs (036–045) — Accepted

| ADR | Title | Status |
| --- | --- | --- |
| [036](adr/036-billing-bounded-context.md) | Billing bounded-context ownership | **Accepted** |
| [037](adr/037-provider-neutral-entitlements.md) | Provider-neutral entitlement model | **Accepted** |
| [038](adr/038-organization-vs-individual-subscriptions.md) | Organization vs individual subscription ownership | **Accepted** |
| [039](adr/039-billing-provider-channel-strategy.md) | Billing-provider / channel strategy | **Accepted** |
| [040](adr/040-subscription-lifecycle.md) | Subscription lifecycle | **Accepted** |
| [041](adr/041-seat-and-usage-semantics.md) | Seat / usage semantics (active-athlete bands) | **Accepted** |
| [042](adr/042-authorization-vs-entitlement.md) | Authorization vs entitlement | **Accepted** |
| [043](adr/043-tax-channel-boundaries.md) | Tax responsibility boundaries | **Accepted** |
| [044](adr/044-provider-webhook-idempotency.md) | Provider webhook / idempotency model | **Accepted** |
| [045](adr/045-duplicate-subscription-and-provider-switch.md) | Duplicate-subscription / provider-switch policy | **Accepted** |

None left Proposed after this lock.

---

## 25. Maintainability debt strategy (V4 engineering objective)

SonarCloud **`main`** (2026-09-10 steward review):

| Measure | Value |
| --- | --- |
| Open CODE_SMELL | **~1,414** |
| Maintainability | **A** (debt ratio ~0.3%) |
| Top 5 rules ≈ **72%** | `java:S5778`, `typescript:S1874`, `typescript:S6759`, `java:S107`, `java:S1128` |
| New Code vs `2.0.0` | QG PASSED; Maintainability A |

**Caveat:** current `develop` analysis may be incomplete (~939 ncloc) — DevOps should restore full-scope analysis; use **`main`** for Overall debt.

### V4 rule

- Every V4 slice applies **campground** on touched code  
- No NOSONAR, mass-format, broad exclusions, fake coverage  
- Optional Lead-approved bounded campaigns (unused imports / readonly props / CRITICAL complexity in one folder)  
- Non-gating aspiration: material reduction toward **&lt; ~1,000** smells by V4 release **if** safe — not a mandate to rewrite V1–V3  
- Overall Security **D** / Reliability **C** remain higher-priority residuals than smell count when those paths are touched  

---

## 26. Athlete Intelligence boundary

Billing may gate **product surface availability** (examples: coach suite exposure, history depth) via `EntitlementPort` only.

Must **never** become billing-dependent:

- State Engine ownership / generate semantics  
- Readiness calculation  
- Team Readiness aggregation, `minCohortSize=5`, complementary suppression  
- Consent / membership authorization  
- No-hidden-write GET contracts  

Training/state/consent must **never** import Stripe/Apple/Google types.

---

## 27. Explicit non-goals (this planning task)

- No runtime V4 code  
- No Flyway migrations  
- No Stripe Product/Price/Customer creation  
- No Apple/Google console mutation  
- No Slice A start  
- No V5+ (wearables, AI coach, marketplace)  
- No Stripe Connect  

---

## 28. Delivery notes (decision lock)

| Item | Value |
| --- | --- |
| Branch | `develop` |
| Allowed | Docs + ADR status updates only |
| Commit intent | `docs: lock V4 commercialization decisions` |
| After push | Verify may run; **no** main merge/tag/deploy/Stripe/Apple/Google mutation |

---

## 29. Decision-lock readiness

| Role | Verdict |
| --- | --- |
| Lead / Architect | **COMPLETE** — §22 locked; ADR-036–045 Accepted; A–I final |
| Documentation / Release | Plan + ADRs updated |
| All other planning roles | Unchanged from planning review; no runtime |

**V4 Product Owner decision lock: COMPLETE**

Do not begin Slice A until explicitly authorized.

