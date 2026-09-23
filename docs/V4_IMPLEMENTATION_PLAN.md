# Athlete Readiness V4 — Implementation Plan

**Theme:** Commercialization / Billing / Entitlements  
**Product question:** Can Athlete Readiness become a commercially operable SaaS product where individuals and organizations can pay through an appropriate billing channel, receive provider-neutral entitlements, manage subscriptions safely, and retain access across Web/iOS/Android without billing logic contaminating the product domain?

**Document type:** Product Owner decision lock (docs)  
**Planning commit:** `117b37ef95c142daa323da021cc8172565b56803`  
**Production baseline (`main`):** Slice D **PRODUCTION VERIFIED** at promotion SHA `1563b684b81e698aaaeaa2abb835f5f141f6201c` (see §39). Prior Slice C SHA `0349424d1a05b543370ed9b75d25b58644d53a03` / `03fbdb1a827539bf66557750bf009ebb89e2f7e7`. Prior Slice B SHA `212f3f44bfe4c8709b636a7839d83c0a978edaa3`.  
**`develop`:** tracks `main` after Slice D promotion (docs-evidence tip follows in §39).  
**Production schema:** Flyway **V36** (inferred — see §33 / §39) 
**Prior version:** Athlete Readiness V3 — **COMPLETE — PRODUCTION VERIFIED**  
**§22 lock status:** **COMPLETE** (ADR-036–045 Accepted)  
**Slice A status:** **PRODUCTION VERIFIED** — commercial foundation only (see §30).
**Pre-Slice-B Organization catalog lock:** **COMPLETE** (see §31).
**Slice B status:** **PRODUCTION VERIFIED** (see §32 sandbox cert + §33 production).  
**Pre-Slice-C entitlement matrix:** **PRODUCT OWNER-APPROVED** (see §34). **Slice C:** **PRODUCTION VERIFIED** (see §36; develop certification in §35). Production **entitlement enforcement remains off**. **Pre-Slice-D capacity lock:** **PRODUCT OWNER LOCKED** (see **§37**; Option B). **Slice D:** **PRODUCTION VERIFIED** (see **§39**; develop certification in **§38**). Production **capacity enforcement remains off**. **Pre-Slice-E billing management:** contract in **§40** (runtime **not** authorized). Slice E is **not** started. V4 is **not** complete.

**This document's §22 lock does not by itself authorize runtime work.** Slice A was separately authorized and is evidenced in §30. Slice B was later explicitly authorized and its local implementation contract is recorded in §32. Live catalog and live charging remain unauthorized.

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

## 1. Planning baseline (pre-V4 repository findings)

This section is the **planning-era snapshot** taken before Slice A–B runtime. It is **not** a description of current production.

Current production: `billing` module exists (V35–V36), Organization Stripe sandbox is certified, Slice B is **PRODUCTION VERIFIED** with Stripe **disabled** in production, and `EntitlementPort` exists but is **not** used at V3 product edges. See §§30–34.

| Finding (at planning) | Evidence at that time |
| --- | --- |
| **No billing runtime (then)** | No Stripe / Apple IAP / Google Play / RevenueCat / entitlement modules, DTOs, or config |
| V3 deferred V4 | `docs/V3_IMPLEMENTATION_PLAN.md` §19 — org may later link billing account id; **do not implement** in V3 |
| Modulith modules (then) | `identity`, `athlete`, `organization`, `consent`, `training`, `audit` |
| Org roles | `ATHLETE`, `COACH`, `HEAD_COACH`, `TEAM_ADMIN`, `ORG_ADMIN`, `ORG_OWNER` |
| Org manage capability | Invitation/admin ops: ORG_ADMIN or ORG_OWNER; **V4 billing authority locked: ORG_OWNER only** (§22.1 #5) |
| Account / Athlete | One Account; ≤1 Athlete; coach-only accounts OK (ADR-030) |
| Audit | Durable append-only `security_audit_events` (V34 / ADR-035); adapters in org/consent/training |
| Web IA (then) | Athlete `/app/*`; coach sibling `/coach/*`; no billing routes (billing Web now exists under `/coach/.../billing` — §32/§33) |
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
| Pricing shape | Fixed **active-athlete bands** (up to 25 / 75 / 250). Dollar amounts **locked** in §31 (Starter / Team / Organization) |
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

### 6.3 Catalog (locked commercially — do not create in Stripe until Slice B)

Prefer **one Product per commercial tier**, separate **Prices** for monthly/annual/currency.

**Product Owner-approved Organization catalog** (tax-exclusive Web/Stripe; see §31):

```text
Athlete Readiness Starter
  monthly: $49
  annual:  $490
Athlete Readiness Team
  monthly: $99
  annual:  $990
Athlete Readiness Organization
  monthly: $149
  annual:  $1,490
```

Internal catalog keys remain `ORG_BAND_25` / `ORG_BAND_75` / `ORG_BAND_250`. Marketing names must never replace stable identifiers.

This docs lock does **not** authorize Stripe Product/Price creation. Slice B creates the **sandbox** catalog after explicit Slice B authorization. Live Products/Prices only after V4 gates. Separate sandbox vs live IDs and webhook secrets. **Fail fast** if environment mismatch.

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
| Shape | Fixed bands (**up to 25 / 75 / 250**) |
| Dollar prices | **Locked** in §31 (Starter $49/$490; Team $99/$990; Organization $149/$1,490) — tax-exclusive; do not invent alternate prices in code |
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
| 2 | Org pricing | **Fixed active-athlete bands** (not usage-metered/per-athlete monthly): **up to 25 / 75 / 250**. Exact org **dollar** prices are Product Owner-locked in **§31** (Starter / Team / Organization). Do not invent alternate prices in code. Stripe Product/Price creation remains Slice B (not authorized by §22 alone). |
| 3 | Billable athlete | One **ACTIVE** Athlete identity counts **once per Organization** regardless of Team count. Pending invitation / LEFT / REMOVED = **not** billable. Same athlete in separate Orgs counts independently. |
| 4 | Free vs entitled | Billing **never** gates privacy/account-control rights. Always available when otherwise authorized: authentication/account access; invitation accept/decline; leave Team; consent grant/revoke/re-grant; athlete transparency/activity; access to athlete-owned retained data/history per existing contracts. Org commercial entitlement gates paid org/coach capabilities. V3 authZ + consent remain independent. Individual **PREMIUM** must not arbitrarily remove basic athlete functionality. **Final Organization free-vs-gated matrix: §34** (Product Owner lock; Slice C enforcement not authorized by §34 alone). |
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
| Org pricing shape | Fixed active-athlete bands (25 / 75 / 250); dollar amounts **locked** in §31 |
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
| **B** | Stripe Organization subscription — sandbox catalog from §31 lock, Checkout, customer mapping, lifecycle sync |
| **C** | Entitlements — server-side commercial capability enforcement using the §34 matrix. Code deploy ≠ activation (`UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED`, default `false`). Production `true` requires a later commercial-launch gate |
| **D** | Bands & usage — active-athlete band enforcement. Semantics locked in **§37**. Runtime **not** authorized by this lock. Dedicated flag `UAP_BILLING_ORGANIZATION_CAPACITY_ENFORCEMENT_ENABLED` (default **false**; independent of Stripe and Slice C entitlement flags) |
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

SonarCloud **`main`** (post–Slice A production tip `acc5bb3c`, 2026-09-11):

| Measure | Value |
| --- | --- |
| Open CODE_SMELL | **1,424** |
| Prior planning reference | **~1,414** |
| Delta | **+10** — exactly **10** New Code smells from Slice A billing files (not a Sonar rule/scope admin change) |
| Maintainability | **A** |
| New Code vs `2.0.0` | QG PASSED (Slice A); Maintainability A |

**Slice A smell follow-up (not a Slice B blocker):**

- Repeated validation-message findings (`java:S1192`) may be cleaned when those billing files are touched  
- Tiny test-style findings may be cleaned opportunistically  
- Do **not** mass-refactor intentional `java:S107` aggregate/JPA constructors  
- Do **not** launch repository-wide `java:S5778` cleanup  
- No NOSONAR / suppression / exclusions  

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

---

## 30. Slice A — Commercial foundation (implementation evidence)

**Status:** Implemented on `develop` (runtime foundation only).  
**Does not authorize Slice B.** No Stripe/Apple/Google integration. **No V3 product-edge paywall** (enforcement deferred to Slice C).

### 30.1 Module architecture

| Item | Value |
| --- | --- |
| Modulith module | `com.devinolabs.uap.billing` |
| Published API | `billing :: entitlements` → `EntitlementPort` |
| Layers | `api/`, `application/`, `domain/`, `infrastructure/persistence/` |
| Allowed dependencies | `{}` (UUID refs only; no shared JPA with identity/org/consent/training) |
| Public HTTP | **None** |

### 30.2 Schema

| Item | Value |
| --- | --- |
| Flyway | **V35** `create_billing_subscriptions` |
| Table | `billing_subscriptions` |
| Stores | subject type/id, provider enum, plan key, lifecycle state, optional opaque provider refs, trial/period/grace timestamps, version |
| Does not store | payment credentials, card/bank data, dollar amounts, Stripe Price IDs, Apple/Google product IDs |

### 30.3 Catalog keys

`ORG_BAND_25`, `ORG_BAND_75`, `ORG_BAND_250`, `INDIVIDUAL_PREMIUM`

Organization bands max active athletes: **25 / 75 / 250** (definition only; usage counting = Slice D).

### 30.4 Capability vocabulary

Organization: `ORG_TEAM_MANAGEMENT`, `ORG_COACH_COLLABORATION`, `ORG_COACH_ATHLETE_VIEW`, `ORG_TEAM_READINESS`  
Individual: `INDIVIDUAL_PREMIUM`

### 30.5 Lifecycle & entitlement

States: `PENDING`, `TRIALING`, `ACTIVE`, `PAST_DUE`, `GRACE_PERIOD`, `CANCEL_AT_PERIOD_END`, `EXPIRED`  
(No separate `CANCELLED` in Slice A.)

Centralized on `Subscription.isCommerciallyEntitledAt(Instant)` / `effectiveCapabilitiesAt`:

| State | Entitled? |
| --- | --- |
| PENDING / PAST_DUE / EXPIRED | No |
| TRIALING | Yes until `trialEndsAt` (org 14-day only) |
| ACTIVE | Yes |
| GRACE_PERIOD | Yes until explicit `graceEndsAt` (7 calendar days) |
| CANCEL_AT_PERIOD_END | Yes until `currentPeriodEndsAt` |

Provider identity does not change capability semantics.

### 30.6 Explicit non-goals (Slice A)

- No checkout / portal / webhooks / IAP  
- No seat enforcement  
- No existing V3 surface paywalled  
- No mandatory Stripe env vars  
- No provider SDKs or network clients  

### 30.7 Sonar develop-scope investigation

`sonar-project.properties` already sets:

`sonar.sources=backend/uap-server/src/main/java,apps/web/src,apps/mobile/src`

The planning-era ~**939 ncloc** develop reading is **not** explained by missing source paths. Treat as incomplete/collapsed short-lived branch analysis presentation vs `main` Overall (~113k ncloc at V2). **No repo-side sonar.sources fix required** for Slice A. Use **`main`** for Overall debt until DevOps restores full-scope develop analysis confidence. Quality Gate thresholds unchanged.

Slice A New Code (post-dedupe tip `ebfdb8ea`): Reliability/Security/Maintainability **A**, coverage **~80.1%**, duplication **0.0%**, hotspots **100%**, Quality Gate **PASSED**. First tip failed on New Code duplication **3.8%** (copied Modulith-local JPA base); fixed by inlining into `BillingSubscriptionJpaEntity`.

### 30.8 Reviews

| Role | Verdict |
| --- | --- |
| QA / Test Automation | PASS (billing suite + Flyway V35 + Verify core shard includes `billing.*`) |
| Security / Quality Gate Steward | PASS |
| Athlete Intelligence / Data | PASS (State Engine / readiness / Team Readiness / consent / membership / no-hidden-write untouched) |

**develop tip after Slice A:** `acc5bb3c47f4e617f6b5226d3ef17804e4831ace` (also `main`; production verified)  
**Verify (QG green, develop Slice A tip):** https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/34558418596  

Do not begin Slice B until explicitly authorized.

## 31. Pre-Slice-B Organization Catalog Lock

**Status:** **COMPLETE** — Product Owner-approved initial V4 Organization pricing.  
**Does not authorize Slice B.** Does not create Stripe Products/Prices. Does not change runtime code.

### 31.1 Locked commercial names and catalog keys

| Web / Stripe-facing name | Internal catalog key | Active-athlete capacity |
| --- | --- | --- |
| **Starter** | `ORG_BAND_25` | Up to **25** |
| **Team** | `ORG_BAND_75` | Up to **75** |
| **Organization** | `ORG_BAND_250` | Up to **250** |

Marketing names must **never** replace stable internal identifiers.

### 31.2 Locked Organization prices (tax-exclusive)

| Tier | Monthly | Annual | Effective annual monthly | Approx. annual discount |
| --- | --- | --- | --- | --- |
| Starter | **$49** | **$490** | $40.83 | ~16.7% (10× monthly / two months free) |
| Team | **$99** | **$990** | $82.50 | ~16.7% (10× monthly / two months free) |
| Organization | **$149** | **$1,490** | $124.17 | ~16.7% (10× monthly / two months free) |

Annual pricing is intentionally **10 × monthly**. No alternate annual discount percentages, coupons, promo codes, or negotiated enterprise pricing in initial V4 (those remain deferred per §22.1 #15–16).

### 31.3 Future Slice B sandbox catalog shape (not created yet)

```text
Athlete Readiness Starter
  monthly: $49
  annual:  $490

Athlete Readiness Team
  monthly: $99
  annual:  $990

Athlete Readiness Organization
  monthly: $149
  annual:  $1,490
```

Slice B may create these **sandbox** objects only after explicit Slice B authorization.

### 31.4 Band, trial, tax, and authority (unchanged)

| Rule | Locked value |
| --- | --- |
| Billable unit | One **ACTIVE** Athlete identity once per Organization |
| Multi-Team | Does **not** multiply the count |
| Not billable | Pending invitation; LEFT; REMOVED |
| Cross-Organization | Independent count per Organization |
| Capacity model | **25 / 75 / 250** unchanged |
| Trial | 14-day Organization trial; payment method required up front |
| Cadence | Monthly + annual |
| Grace | 7 calendar days on failed payment |
| Cancel | At period end |
| Billing authority | **ORG_OWNER** only |
| Tax display | Advertised Web price **+ applicable taxes** (prices above are **not** tax-inclusive) |
| Stripe Tax live | Only after Devino Labs tax registration / product classification review; `automatic_tax.enabled = true` ≠ registration |
| Apple/Google tax | Separate; not part of this Organization price lock |

### 31.5 Individual Premium (unchanged)

| Item | Value |
| --- | --- |
| Targets | **$9.99/month**, **$99.99/year** |
| This lock | Does **not** alter Individual pricing |

### 31.6 Sonar follow-up note

Slice A CODE_SMELL **~1,414 → 1,424** (+10) is attributed to exactly **10** new Slice A findings. **Not a Slice B blocker.** Campground guidance in §25 applies; no runtime cleanup in this docs lock.

**V4 Organization price lock: COMPLETE**

Do not begin Slice B until explicitly authorized.

---

## 32. Slice B — Stripe Organization subscription (implementation evidence)

**Status:** **COMPLETE on `develop`.** Sandbox catalog, hosted Checkout, and signed webhook certification recorded in §32.5.
**Does not authorize Slice C.** No V3 product-edge paywall. **Stripe Tax collection remains OFF.** Live Stripe was not used.

### 32.1 Runtime contract

| Item | Implemented behavior |
| --- | --- |
| SDK | `com.stripe:stripe-java:33.4.2` via `StripeClient` (no deprecated global API key) |
| Enablement | Disabled by default (`uap.billing.stripe.enabled=false`). When enabled: fail-fast test-mode `rk_test_`/`sk_test_` key, `whsec_` webhook secret, six distinct Price IDs, HTTPS or local HTTP success/cancel URLs |
| Live safety | `prod`/`production` activation rejected; live keys and non-sandbox Stripe objects rejected |
| Catalog | Server-owned allowlist maps `ORG_BAND_25`/`75`/`250` × `MONTHLY`/`ANNUAL` to configured sandbox Price IDs; clients never submit Price IDs or amounts |
| Checkout | Hosted Checkout Session, `mode=subscription`, quantity `1`, 14-day trial, payment method required up front, dynamic payment methods, **no automatic tax** |
| Authority | Active `ORG_OWNER` on an active Organization only; foreign accounts and `ORG_ADMIN` receive non-oracle not-found |
| Retry safety | Client supplies a stable request UUID; Stripe idempotency keys; unique Organization→Stripe Customer mapping |
| Fulfillment | Checkout persists `PENDING` (not entitled). Success redirect is **not** authoritative. Verified Stripe webhooks (and optional owner sync) refetch Stripe and apply a provider-neutral snapshot |
| Durable webhook idempotency | `billing_provider_events` unique `(provider, provider_event_id)` claimed in `REQUIRES_NEW`. `PROCESSED`/`IGNORED` replays after restart do not re-apply. `RECEIVED` remains retryable. Concurrent applies retry optimistic-lock conflicts and then complete the receipt |
| Stale state | Authoritative Stripe Subscription refetch; `provider_state_as_of` ignores equal/older snapshots; unknown provider statuses fail closed |
| Invoice events | UAP IDs from Invoice `parent.subscription_details.metadata` (not Invoice.metadata) |
| Event object | Prefer typed `getObject()`; if empty, `deserializeUnsafe()`. Failure is 502 so Stripe retries — not HTTP 200 ignore |
| Webhook API version | Pin Dashboard endpoint to stripe-java train **`2026-08-26.dahlia`** |
| Audit | `BILLING_CHECKOUT_INITIATED`, `BILLING_SUBSCRIPTION_SYNCHRONIZED`, `BILLING_SUBSCRIPTION_ACTIVATED` — plan/cadence/state only |

### 32.2 HTTP API

```text
POST /api/v1/billing/organizations/{organizationId}/checkout-sessions
{
  "requestId": "UUID",
  "planKey": "ORG_BAND_25 | ORG_BAND_75 | ORG_BAND_250",
  "cadence": "MONTHLY | ANNUAL"
}

POST /api/v1/billing/organizations/{organizationId}/subscriptions/{subscriptionId}/sync
{
  "checkoutSessionId": "cs_..."
}

GET /api/v1/billing/organizations/{organizationId}

POST /api/v1/billing/webhooks/stripe
  Stripe-Signature: t=...,v1=...
  raw JSON body (no CSRF; signature is the trust root)
```

Checkout/sync/GET: authenticated `ORG_OWNER`, CSRF on mutations. Webhook: unauthenticated, CSRF ignored, signature required.

Webhook events handled (current Stripe names): `checkout.session.completed`, `customer.subscription.created`, `customer.subscription.updated`, `customer.subscription.deleted`, `invoice.paid`, `invoice.payment_failed`. Live-mode events are ignored.

### 32.3 Schema (Flyway V36)

- nullable `billing_cadence`, `provider_state_as_of` on `billing_subscriptions`
- `billing_organization_customers` — one Stripe customer per Organization
- `billing_provider_events` — provider, provider_event_id, event_type, received_at, processed_at, processing_status; unique `(provider, provider_event_id)`; **no raw payload**

No Apple/Google processing tables.

### 32.4 Explicit non-goals preserved

- **No Slice C** entitlement enforcement or V1–V3 endpoint paywall
- No Customer Portal, upgrade/downgrade/cancel/reactivate management (Slice E)
- No dunning/grace automation/reconciliation beyond initial webhook sync (Slice F)
- No individual Stripe / Apple / Google monetization (Slice G)
- No Stripe Tax enablement, promotions, enterprise invoicing, or Connect
- No live Stripe mutation, `main` merge, release, tag, or deployment

### 32.5 Sandbox catalog and smoke (Athlete Readiness sandbox)

**Account:** Athlete Readiness sandbox `acct_1UHZjZD418eILvNQ` (Stripe CLI `whoami` `mode=test`; Session/Customer/Subscription `livemode=false`). Not the generic DEVINO LABS LLC live/test accounts.

| Product | Product ID | Monthly Price | Annual Price |
| --- | --- | --- | --- |
| Athlete Readiness Starter (`ORG_BAND_25`) | `prod_VIAHNlhHCs6m2K` | `price_1UHa1BD418eILvNQY5et2YSU` **4900 USD / month** | `price_1UHa1DD418eILvNQvsvcQWzK` **49000 USD / year** |
| Athlete Readiness Team (`ORG_BAND_75`) | `prod_VIALhxOIzkPIGT` | `price_1UHa1FD418eILvNQSHnLU9uW` **9900 USD / month** | `price_1UHa1HD418eILvNQNpJaOC8k` **99000 USD / year** |
| Athlete Readiness Organization (`ORG_BAND_250`) | `prod_VIALuN39AJt5PC` | `price_1UHa1ID418eILvNQ6ZHSZQ1F` **14900 USD / month** | `price_1UHa1LD418eILvNQlILJOp3p` **149000 USD / year** |

Exactly **3** Products and **6** recurring licensed Prices. Tax behavior **exclusive**. `automatic_tax.enabled=false` on Checkout Sessions and Subscriptions. No tax registrations created.

| Check | Result |
| --- | --- |
| Hosted Checkout (prior sandbox cert) | Session `cs_test_a1G5ku309lD2fFapaKkRrwsHtwrq5YjZquTN9Vgc1z2S3AzKZTgVPqNgeh`: `status=complete`, `payment_status=paid`, `mode=subscription`, `payment_method_collection=always`, **$0 due today**, card collected, Subscription `sub_1UHaOKD418eILvNQ2evsoO9Z` **trialing**, Customer `cus_VIAgQcdqprYAbs`, 14-day trial, Starter monthly $49. Browser redirect to an unrelated success URL is **not** fulfillment. |
| Server Checkout this cert | `ORG_OWNER` `POST /checkout-sessions` → HTTP 201, internal **PENDING**, Stripe Customer `cus_VIBXbn5nIM56Lq`, Session `cs_test_a1SwjkcaOnfnPYOKeiEDMbMDg7xJsOaC4UQXCQZqNUvRlivYyeduyYE2JF` (`open`, `unpaid`, `mode=subscription`, `payment_method_collection=always`, `amount_total=0`, `automatic_tax.enabled=false`, livemode=false). Same `requestId` replay returned the **same** Session/Customer. Second `requestId` while PENDING → `BILLING_CHECKOUT_IN_PROGRESS`. |
| Fulfillment webhook | Stripe CLI listen (`2026-08-26.dahlia`) to `http://127.0.0.1:8080/api/v1/billing/webhooks/stripe`. Real Subscription `sub_1UHbGQD418eILvNQ7qLePd1m` **trialing**, trial_end **2026-10-04 03:18:02Z** (~14 days), default PM present, Price `price_1UHa1BD418eILvNQY5et2YSU`. Internal `4ba8f1c1-9088-4fe7-b32a-409bb83938f4` **PENDING → TRIALING**. |
| Event IDs | `invoice.paid` `evt_1UHbGRD418eILvNQ1IoGiLN7` **PROCESSED**; `customer.subscription.created` `evt_1UHbGRD418eILvNQTpdslLWm` **PROCESSED** (unique `STRIPE` + event id). |
| Signature | Missing/invalid `Stripe-Signature` → HTTP **400** empty body. CLI-forwarded signed events → HTTP **200** after the lock retry. |
| Duplicate replay | Resend both event IDs: HTTP 200, still one subscription, one customer, one `BILLING_SUBSCRIPTION_ACTIVATED`, `provider_state_as_of` unchanged. |
| Restart replay | Backend stopped and started; resend `invoice.paid` → HTTP 200, still TRIALING / version 1 / one customer / one activation audit. |
| Stale / out-of-order | Domain: equal/older `provider_state_as_of` ignored (`SubscriptionDomainTests`). Live: replay of the same Stripe snapshot did not regress TRIALING. |
| Customer reuse | One `billing_organization_customers` row `cus_VIBXbn5nIM56Lq`; Stripe lists **one** Subscription for that Customer. |
| Duplicate Checkout guard | After TRIALING, new Checkout → HTTP **409** `BILLING_SUBSCRIPTION_EXISTS` (no second Stripe Subscription). |
| Live Stripe | **Untouched** (`stripe listen` without `--live`; objects `livemode=false`). |
| Slice C | **Not started.** `EntitlementPort` unused outside billing tests. |

Secrets, webhook signing secrets, API keys, and card numbers are not recorded here. Repository default remains `UAP_BILLING_STRIPE_ENABLED=false`.

Slice B sandbox certification is **COMPLETE**. Production promotion with Stripe remaining **disabled** is recorded in **§33**.

---

## 33. Slice B — PRODUCTION VERIFIED

**Status:** **V4 Slice B — PRODUCTION VERIFIED**  
**Does not authorize Slice C.** No entitlement enforcement. No live Stripe Products, Prices, Customers, Checkout Sessions, Subscriptions, webhook destinations, or API keys.

### 33.1 Fast-forward promotion

| Item | Value |
| --- | --- |
| Pre-promotion `develop` / `origin/develop` | `4031328152a031fbbb2815afa0373a01cbc16534` |
| Pre-promotion `main` / `origin/main` | `acc5bb3c47f4e617f6b5226d3ef17804e4831ace` |
| Topology | `develop` **6** commits ahead of `main`, **0** behind; merge-base = `main` |
| Method | Solo-maintainer `git merge --ff-only develop` on `main` (no PR, rebase, squash, or force push) |
| Code promotion SHA | `4031328152a031fbbb2815afa0373a01cbc16534` |
| Authoritative develop Verify (pre-promotion) | [35487328304](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/35487328304) **SUCCESS** |

### 33.2 Main Verify / Sonar

| Item | Value |
| --- | --- |
| Main Verify | [35487870740](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/35487870740) **SUCCESS** |
| Jobs | Backend core, Backend training-app, Backend training-http, Backend aggregate, Web, Mobile, Sonar Quality Gate — all **success** |
| Quality Gate | **PASSED** (`alert_status=OK`) — https://sonarcloud.io/dashboard?id=Devino-Labs-LLC_Universal-Athlete-Platform&branch=main |
| New Code reliability | **A** |
| New Code security | **A** |
| New Code maintainability | **A** |
| New Code coverage | **82.8%** |
| New Code duplication | **0.4%** |
| New Code hotspot review | **100%** |

Gate thresholds were not weakened.

### 33.3 Railway production

| Item | Evidence |
| --- | --- |
| Auto-deploy | GitHub environment `Universal Athlete Platform / production` deployment **6548800901** for SHA `4031328…` — **success** (2026-09-20T03:57:56Z). No manual recovery deploy. |
| `UAP_Server` | `https://uapserver-production.up.railway.app` remains **UP** after auto-deploy |
| `UAP_Client_Web` | `https://uapclientweb-production.up.railway.app` HTTP 200; bundle `index-4xIvjPtN.js` contains `OrganizationBilling`; no `sk_test_`, `rk_test_`, `whsec_`, or sandbox Price IDs in the bundle |
| `/actuator/health` | HTTP 200 `{"status":"UP"}` |
| `/actuator/health/liveness` | HTTP 200 `{"status":"UP"}` |
| `/actuator/health/readiness` | HTTP 200 `{"status":"UP"}` |

Railway dashboard variable listing was **not** available (no Railway CLI / token in this environment). Stripe-disabled conclusions below do **not** claim a direct Railway variable dump.

### 33.4 V36

`V36__create_billing_stripe_org_foundation.sql` — **inferred**, not a direct Flyway history or MySQL query.

Production Hibernate `ddl-auto=validate` plus billing JPA entities that are **not** `@ConditionalOnProperty` would fail startup if V36 tables were missing. The server stayed healthy after the `4031328` auto-deploy. Direct `flyway_schema_history` evidence was not available.

### 33.5 Production Stripe remained disabled; live Stripe untouched

| Check | Result |
| --- | --- |
| Repository default | `uap.billing.stripe.enabled: ${UAP_BILLING_STRIPE_ENABLED:false}` |
| GitHub Actions secrets | Only `SONAR_TOKEN`; no Stripe keys |
| This promotion | No sandbox `sk_test_` / `rk_test_` / `whsec_` / Price IDs / localhost Checkout URLs were written to Railway production |
| Boot | Healthy with **no** Stripe credential requirement. `StripeBillingConfiguration` is `@ConditionalOnProperty(... enabled=true)` and **refuses** the `prod`/`production` profile even if enabled |
| Webhook | `POST /api/v1/billing/webhooks/stripe` with a dummy `Stripe-Signature` → HTTP **401** `/error` (controller not registered). Enabled Stripe would return **400** for an invalid signature |
| Checkout HTTP | Unauthenticated billing mutations do not reach Stripe; `OrganizationBillingController` is not registered while disabled |
| Live Stripe | **Untouched** (no live Products/Prices/Customers/Checkout/Subscriptions/webhooks/keys created) |

### 33.6 V1–V3 regression / billing boundary / Slice C

| Check | Result |
| --- | --- |
| Unauthenticated protected APIs | `/api/v1/identity/me`, athletes, organizations, training overview, readiness, teams → **401** |
| CSRF intact | `POST /api/v1/identity/logout` and `POST /api/v1/organizations` without CSRF → **403** `CSRF_INVALID` |
| Login CSRF exemption intact | `POST /api/v1/identity/login` still authenticates (invalid credentials **401**, not CSRF) |
| Webhook CSRF skip | Did not weaken unrelated mutations |
| No V1–V3 paywall | Surfaces remain auth/consent bounded, not entitlement-gated |
| Slice C | **Not started.** `EntitlementPort` / `EntitlementQueryService` unused outside the billing module and billing tests |

V4 is **not** complete. Slice C **runtime** is authorized only after §34.13 is on `develop`. Production **activation** (`UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED=true`) is a later commercial-launch gate.

---

## 34. Pre-Slice-C Organization entitlement matrix (Product Owner lock)

**Status:** **PRODUCT OWNER-APPROVED** matrix + **rollout policy locked**. Docs only in this subsection’s originating commit.  
**Slice C runtime implementation** is authorized **after** this rollout lock is committed on `develop`.  
**Does not authorize** production enforcement activation, live Stripe, Flyway beyond existing V36, Slice D, or commercial launch.

**Refs:** `main` = `212f3f44bfe4c8709b636a7839d83c0a978edaa3` (Slice B **PRODUCTION VERIFIED**, Stripe disabled in production). Matrix lock `c64ba79eae5d4199ac8a1c7845ea4debfb79dcc5`; clarification `cedf1049a1fdf2c023114cc3de609963380f2da1`.

### 34.1 Principles

| Principle | Lock |
| --- | --- |
| Authorization | WHO may act — existing V3 membership, role, consent, IDOR (ADR-032, ADR-033, `docs/security/V3_AUTHORIZATION_MATRIX.md`) |
| Entitlement | WHETHER the **Organization** purchased a capability — `EntitlementPort.hasCapability(ORGANIZATION, organizationId, capability)` (ADR-037, ADR-042) |
| Both may be required | Paid status never replaces membership, role, consent, IDOR, or Team Readiness privacy |
| Subject | Organization capabilities check **`BillingSubjectType.ORGANIZATION` + the authorized `organizationId`**. Never the coach Account subscription. Never `INDIVIDUAL_PREMIUM` on Organization edges |
| Catalog | All org bands grant the same four org capabilities (`CommercialCatalog`). Slice D bands are **not** this lock |
| Provider | Product edges must not branch on Stripe/Apple/Google, plan names, or `STRIPE_ACTIVE` |
| Server is authoritative | React route guards, button visibility, and client plan state are not security |
| Athlete Intelligence | Billing may gate **product-surface access** only. No billing in State Engine, readiness/recovery/recommendation calculators, Team Readiness math, consent determination, or membership ports |

### 34.2 Evaluation order (authZ before entitlement)

1. **Unauthenticated** → existing **401** `UNAUTHENTICATED`.
2. **CSRF** on unsafe methods → existing **403** `CSRF_INVALID` (filter; not commercial).
3. **V3 authZ** (membership, role, resource graph, consent-for-existence where V3 already 404s) → existing **404** + existing surface `code`. No existence oracle.
4. **Only then** entitlement, using `organizationId` from the **already-authorized** graph (Team parent org or the org that just passed membership). Never evaluate entitlement first. Never check a client-supplied org id before that graph check.
5. Authenticated **and otherwise authorized** but missing capability → **HTTP 402** `COMMERCIAL_ENTITLEMENT_REQUIRED` (see §34.5). Never 404 for commercial denial.

Insufficient V3 role on a **paid** org remains the existing **404**, never 402 (example: `TEAM_ADMIN` cannot create coach assignments).

**Consent vs entitlement (do not collapse unpaid + no-consent):**

Global order remains AuthN → CSRF → V3 authZ / **consent-for-existence** → entitlement. Paid/unpaid must not become an existence oracle (unauthorized + paid and unauthorized + unpaid return the **same** 401/404).

Split the two consent kinds:

| Surface | Missing consent while membership/role/resource authZ already passed | Unpaid Organization |
| --- | --- | --- |
| Coach athlete overview | Section-level consent is **projection shaping**, not existence denial. Entitled + missing section → **200** `notShared`. | Membership/role/resource passed + unpaid + missing section consent → **402** `COMMERCIAL_ENTITLEMENT_REQUIRED` (do not skip to `notShared`) |
| Coach training assignment | `TRAINING_COLLABORATION` is part of the existing V3 **authorization/existence** gate. Missing it → existing **404** **before** entitlement, **even if** the Organization is unpaid. | Same 404 whether paid or unpaid — never 402 |

### 34.3 Temporal semantics (do not reimplement at edges)

Use `EntitlementPort` only. `Subscription.isCommerciallyEntitledAt` already encodes:

| State | Entitled? |
| --- | --- |
| `PENDING` | No |
| `TRIALING` | Yes until **exclusive** `trialEndsAt` (`isBefore`) |
| `ACTIVE` | Yes |
| `PAST_DUE` | No (unless Slice F has entered `GRACE_PERIOD`) |
| `GRACE_PERIOD` | Yes until exclusive `graceEndsAt` |
| `CANCEL_AT_PERIOD_END` | Yes until exclusive `currentPeriodEndsAt` |
| `EXPIRED` | No |
| No subscription row | No (same 402 as EXPIRED; keep separate test fixtures) |

At exactly the end instant → not entitled → 402.

### 34.4 Capability mapping

| Capability | Paid behavior (Slice C) | Must not include |
| --- | --- | --- |
| `ORG_TEAM_MANAGEMENT` | Creating, renaming, and archiving **Teams**; **creating** org/team invitations | Accept/decline invite; leave; consent; roster-safe identity; list/revoke outstanding invites; remove members; org create/rename/archive; billing |
| `ORG_COACH_COLLABORATION` | Coach `TrainingAssignment` list/get/create/update | Athlete-owned `TrainingPlan` graph; athlete decline/unable/list-mine |
| `ORG_COACH_ATHLETE_VIEW` | Consent-aware coach athlete **overview** HTTP | Roster-safe identity; athlete self-history |
| `ORG_TEAM_READINESS` | `GET /api/v1/teams/{teamId}/readiness` surface | Aggregation math, k-anonymity, stored-read, no mega-score |
| `INDIVIDUAL_PREMIUM` | **Not enforced in Slice C** (Slice G) | Do not paywall basic athlete functionality |

### 34.5 Commercial-denial contract (recommendation; not implemented)

| Item | Lock |
| --- | --- |
| HTTP | **402** (`Payment Required`) |
| Machine `code` | `COMMERCIAL_ENTITLEMENT_REQUIRED` |
| Body | Existing problem JSON: `code`, `message`, `timestamp`, `path`, `details` |
| Why not 404 | Would disguise commerce as IDOR and leak/hide inconsistently |
| Why not 403 | Already `CSRF_INVALID` / `ACCESS_DENIED` / account-state; ADR-032 forbids 403 as a tenant oracle |
| Message | Must not include Stripe ids, Price IDs, plan keys, or foreign-org hints |
| Side effects | Gated mutations that 402 must write **nothing** |

Clients must key on **`code`**, not status category alone (web `errorMapper` has no 402 branch today).

**402 applies only when `UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED=true`** (see §34.13). With the default `false`, gated surfaces keep pre-Slice-C V3 responses.

### 34.6 Durable free-vs-gated matrix

Legend: **Free** = never 402. **Gated** = after V3 authZ, require the named capability.

AuthZ column is the **existing** V3 rule (unchanged). Consent column is V3 (unchanged).

#### Identity / account (always Free)

| Product action | Existing authZ | Consent | Capability | Free/Gated | Server enforcement boundary |
| --- | --- | --- | --- | --- | --- |
| Register / verify-email / login | Public (+ CSRF ignore on those POSTs) | None | — | **Free** | `IdentityController` — do not call `EntitlementPort` |
| Refresh / logout / logout-all / GET me | Session / cookie as today | None | — | **Free** | Same |
| Athlete onboarding / profile graph | Caller athlete | None | — | **Free** | Athlete use cases |

#### A. Organization / Team administration

| Product action | Existing authZ | Consent | Capability | Free/Gated | Server enforcement boundary |
| --- | --- | --- | --- | --- | --- |
| `POST /api/v1/organizations` | Authenticated; creator → `ORG_OWNER` | None | — | **Free** | `CreateOrganizationUseCase` — needed to reach billing |
| `GET /api/v1/organizations` | Active org **or** team membership in that org | None | — | **Free** | `ListOrganizationsForAccountUseCase` |
| `GET /api/v1/organizations/{organizationId}` | Same; else `ORGANIZATION_NOT_FOUND` | None | — | **Free** | `GetOrganizationUseCase` |
| `PATCH /api/v1/organizations/{organizationId}` | ACTIVE `ORG_OWNER` | None | — | **Free** | `UpdateOrganizationUseCase` — owner identity, not paid workspace |
| `POST /api/v1/organizations/{organizationId}/archive` | ACTIVE `ORG_OWNER` | None | — | **Free** | `ArchiveOrganizationUseCase` — emergency wind-down |
| `POST /api/v1/organizations/{organizationId}/teams` | ACTIVE `ORG_OWNER` + org ACTIVE | None | `ORG_TEAM_MANAGEMENT` | **Gated** | `CreateTeamUseCase` after `requireManageAccess` |
| `GET /api/v1/organizations/{organizationId}/teams` | Active **org** member | None | — | **Free** | `ListTeamsForOrganizationUseCase` — needed to leave/navigate |
| `GET /api/v1/teams/{teamId}` | `TeamAccessGuard.requireVisibleTeam` | None | — | **Free** | `GetTeamUseCase` |
| `PATCH /api/v1/teams/{teamId}` | ACTIVE `ORG_OWNER` of parent org | None | `ORG_TEAM_MANAGEMENT` | **Gated** | `UpdateTeamUseCase` after owner check |
| `POST /api/v1/teams/{teamId}/archive` | ACTIVE `ORG_OWNER` | None | `ORG_TEAM_MANAGEMENT` | **Gated** | `ArchiveTeamUseCase` after owner check |

No dedicated org-settings, hard-delete, or ownership-transfer APIs exist — do not invent them.

#### B. Invitations and memberships

| Product action | Existing authZ | Consent | Capability | Free/Gated | Server enforcement boundary |
| --- | --- | --- | --- | --- | --- |
| `POST …/organizations/{organizationId}/invitations` | `ORG_ADMIN` or `ORG_OWNER` (`requireOrgAdminOrOwner`) | None | `ORG_TEAM_MANAGEMENT` | **Gated** | `CreateOrganizationInvitationUseCase` after invite authZ. Invited role remains `ORG_ADMIN` (cannot invite `ORG_OWNER`) |
| `GET …/organizations/{organizationId}/invitations` | `ORG_ADMIN` or `ORG_OWNER` (`requireOrgAdminOrOwner`) | None | — | **Free** | `ListOrganizationInvitationsUseCase` — required to operate revoke |
| `POST …/invitations/{invitationId}/revoke` (org) | `ORG_ADMIN` or `ORG_OWNER` (`requireOrgAdminOrOwner`) | None | — | **Free** | `RevokeInvitationUseCase` — containment after lapse |
| `GET …/organizations/{organizationId}/memberships` | Active org member | None | — | **Free** | `ListOrganizationMembershipsUseCase` |
| `DELETE …/memberships/{membershipId}` (org) | Admin/owner + `InvitationAuthority` | None | — | **Free** | `RemoveOrganizationMemberUseCase` — eject compromised staff; Slice D remediation |
| `POST …/memberships/me/leave` (org) | Own active membership; last owner blocked | None | — | **Free** | `LeaveOrganizationUseCase` |
| `POST /api/v1/teams/{teamId}/invitations` | `requireInviteCapability` | None | `ORG_TEAM_MANAGEMENT` | **Gated** | `CreateTeamInvitationUseCase` after invite authZ |
| `GET /api/v1/teams/{teamId}/invitations` | `requireListInvitationsCapability` | None | — | **Free** | `ListTeamInvitationsUseCase` |
| `POST /api/v1/teams/{teamId}/invitations/{id}/revoke` | Invite capability | None | — | **Free** | `RevokeInvitationUseCase` |
| `GET /api/v1/teams/{teamId}/memberships` | `requireVisibleTeam` | None | — | **Free** | `ListTeamMembershipsUseCase` — **not** consent-filtered |
| `DELETE /api/v1/teams/{teamId}/memberships/{membershipId}` | `requireManageRoster` | None | — | **Free** | `RemoveTeamMemberUseCase` |
| `POST /api/v1/teams/{teamId}/memberships/me/leave` | Own active team membership | None | — | **Free** | `LeaveTeamUseCase` |
| `GET /api/v1/me/invitations` | Invited email | None | — | **Free** | `ListMyInvitationsUseCase` |
| `POST /api/v1/me/invitations/{id}/accept\|decline` | Invited email (+ verified on accept) | None | — | **Free** | `AcceptInvitationUseCase` / `DeclineInvitationUseCase` |
| `POST /api/v1/invitations/{rawToken}/accept\|decline` | Same | None | — | **Free** | Token variants of the same use cases |
| `GET /api/v1/athletes/me/teams` | Active ATHLETE memberships | None | — | **Free** | `ListMyAthleteTeamsUseCase` |

No membership **role-change** API exists.

#### C. Coach assignment / collaboration

| Product action | Existing authZ | Consent | Capability | Free/Gated | Server enforcement boundary |
| --- | --- | --- | --- | --- | --- |
| Coach `GET/POST/PATCH …/teams/{teamId}/athletes/{athleteId}/training/assignments` | `CoachTrainingAuthorization.requireCollaborationWrite` (`COACH`/`HEAD_COACH`) | **`TRAINING_COLLABORATION`** else 404 | `ORG_COACH_COLLABORATION` | **Gated** | `AssignCoachTrainingUseCase` / `UpdateCoachTrainingAssignmentUseCase` after V3 404s |
| Athlete `GET /api/v1/athletes/me/training/assignments` | Caller athlete | Owner | — | **Free** | `AthleteTrainingAssignmentUseCase.listMine` |
| Athlete decline / unable | Assignment owned by caller | Owner | — | **Free** | `decline` / `markUnable` |

This is **not** TrainingPlan sharing. Athlete-owned plans stay free (F/I).

#### D. Coach roster

| Product action | Existing authZ | Consent | Capability | Free/Gated | Server enforcement boundary |
| --- | --- | --- | --- | --- | --- |
| `GET /api/v1/teams/{teamId}/roster` | `requireVisibleTeam` | **None** — roster-safe identity | — | **Free** | `GetTeamRosterUseCase` — needed to leave/revoke with context |

#### E. Consent-aware athlete views

| Product action | Existing authZ | Consent | Capability | Free/Gated | Server enforcement boundary |
| --- | --- | --- | --- | --- | --- |
| `GET …/teams/{teamId}/athletes/{athleteId}/overview` | `canViewTeam` + athlete on team | Per-section scopes; missing → `notShared` (HTTP 200) | `ORG_COACH_ATHLETE_VIEW` | **Gated** | `GetCoachAthleteOverviewUseCase` after membership 404; **then** entitlement; **then** existing stored projection. Never generate State/readiness on GET |
| `POST/GET /api/v1/athletes/me/consents` | Caller athlete + active ATHLETE membership on grant | Athlete-owned | — | **Free** | `CreateConsentGrantUseCase` / `ListMyConsentGrantsUseCase` |
| `POST …/consents/{consentId}/revoke` | Grant owned by caller | Owner | — | **Free** | `RevokeConsentGrantUseCase` |

Paid Organization **never** grants scopes. Unpaid **never** revokes them.

#### F. Training-plan assignment

**Coach TrainingPlan assignment HTTP does not exist.** Athlete-owned `/api/v1/training/plans/**` and schedule mutations remain **Free**. Do not silently treat them as `ORG_COACH_COLLABORATION`.

#### G. Team Readiness

| Product action | Existing authZ | Consent | Capability | Free/Gated | Server enforcement boundary |
| --- | --- | --- | --- | --- | --- |
| `GET /api/v1/teams/{teamId}/readiness` | `canViewTeamReadinessAggregate` (team COACH/HEAD_COACH/TEAM_ADMIN or org ADMIN/OWNER); else 404 | Athletes included only with readiness scopes; `minCohortSize = 5`; complementary suppression; GET stored-read only; no mega-score | `ORG_TEAM_READINESS` | **Gated** | `GetTeamReadinessUseCase` after role 404; entitlement; then **identical** V3 aggregate. Do not import billing into `TeamReadinessSuppressionPolicy` or membership predicates |

Entitled + below-minimum cohort remains existing `INSUFFICIENT_DATA` / `BELOW_MINIMUM`, not 402.

#### H–I. Athlete transparency and owned history

| Product action | Existing authZ | Consent | Capability | Free/Gated | Server enforcement boundary |
| --- | --- | --- | --- | --- | --- |
| `GET /api/v1/athletes/me/transparency` | Caller athlete | Owner | — | **Free** | `GetAthleteTransparencyUseCase` |
| Athlete-owned state / readiness / recovery / recommendations / load / PRs / calendar / client facades / generate+regenerate | Caller athlete | Owner | — | **Free** | Existing training use cases — including **explicit generate** POSTs |

No coach HTTP to another athlete’s history. Coach projection is **E** only.

#### J. Billing (recovery; always Free commercially)

| Product action | Existing authZ | Consent | Capability | Free/Gated | Server enforcement boundary |
| --- | --- | --- | --- | --- | --- |
| `POST/GET /api/v1/billing/organizations/{organizationId}…` | ACTIVE `ORG_OWNER` (`canManageOrganization`); else 404 | None | — | **Free** | `OrganizationCheckoutService` — do not nest entitlement on billing itself. Stripe flag still required for beans |
| `POST /api/v1/billing/webhooks/stripe` | Signature; `permitAll` | None | — | **Free** | `OrganizationWebhookService` |
| Web `/coach/organizations/:organizationId/billing` + success/cancel | Client is not authZ | — | — | **Free** (UX) | Server checkout remains owner-only |

`ORG_ADMIN` must **not** gain billing because a feature is gated. Billing 404 for non-owners stays 404, never 402.

### 34.7 AuthZ × entitlement tests (every gated family)

Gated families: team create/update/archive; org/team **invite create**; coach assignment list/get/create/update; coach overview; Team Readiness.

For each family, Slice C tests with **`UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED=true`** must include at least:

| # | Fixture | Expected |
| --- | --- | --- |
| 1a | Unauthenticated, no entitlement | Existing 401 (GET) or 403 `CSRF_INVALID` (unsafe without CSRF) — **never 402** |
| 1b | Authenticated inaccessible, no entitlement | Existing 404 + existing `code` — **never 402** |
| 2a/2b | Same actors, org **ACTIVE** | **Identical** 401/403/404 as 1a/1b — paid is not an oracle |
| 2c | Insufficient V3 role, org ACTIVE | Existing 404 — **never 402** |
| 3 | V3-authorized, no subscription | **402** `COMMERCIAL_ENTITLEMENT_REQUIRED` |
| 4 | V3-authorized, `ACTIVE` | Existing success |
| 5 | `EXPIRED` | 402 |
| 6 | `TRIALING` before / at `trialEndsAt` | success / 402 |
| 7 | `CANCEL_AT_PERIOD_END` before / at `currentPeriodEndsAt` | success / 402 |
| 8a | Overview: entitled + no section consent | **200** `notShared` (not 402) |
| 8b | Overview: entitled + valid consent | Existing shared sections |
| 8c | Assignment: entitled + no `TRAINING_COLLABORATION` | Existing **404** (consent-for-existence **before** 402) |
| 8d | Assignment: entitled + valid collaboration consent | Existing success |
| 8e | Overview: membership/role/resource passed, Organization **unpaid**, section consent missing | **402** `COMMERCIAL_ENTITLEMENT_REQUIRED` (section consent is projection shaping, not an existence denial — do not skip to `notShared`) |
| 8f | Assignment: `TRAINING_COLLABORATION` missing, Organization **unpaid** | Existing **404** **before** entitlement (same as paid). Paid/unpaid must not become an oracle |
| 9 | `PAST_DUE` | 402 |
| 10 | `GRACE_PERIOD` before / at `graceEndsAt` | success / 402 |
| 11 | `PENDING` | 402 |
| 12 | Org unpaid; coach `ACCOUNT` + `INDIVIDUAL_PREMIUM` | **402** |
| 13 | Other org ACTIVE; this org unpaid | 402 on **this** org; foreign still 404 |

Reuse `EntitlementPort` with a fixed `Clock`. Do not re-code lifecycle in product tests. Do not delete existing V3 success tests — add ACTIVE org entitlement fixtures when Slice C lands.

Gated mutations that 402 must be side-effect free. Team Readiness GET remains stored-read on 200 and 402.

### 34.8 Free-surface regression (must never 402)

Prove each of these with the Organization in `EXPIRED` / `PAST_DUE` / `PENDING` (or no row):

- Identity register/login/verify/refresh/logout/me
- Invite accept/decline (`/me` and token) and `GET /api/v1/me/invitations`
- Invitation **list + revoke** (org and team)
- Leave Team and leave Organization
- Remove org/team member (existing role rules)
- Consent grant / revoke / re-grant / list
- Transparency / activity
- Athlete-owned history **and** explicit generate/regenerate
- Roster / memberships / GET team / GET org / list org teams
- Billing checkout / sync / GET current (ORG_OWNER); non-owner still 404 not 402

Billing lapse must not strand a user: they can still revoke consent, leave, inspect transparency, and the owner can still open billing.

### 34.9 Web UX contract (not implemented)

On `COMMERCIAL_ENTITLEMENT_REQUIRED`:

- Explain that the **Organization** does not currently have access to that capability. Do not say the user is unauthorized when authZ succeeded.
- **Billing CTA only for `ORG_OWNER`.** `ORG_ADMIN` and coaches do not receive financial controls.
- Avoid aggressive full-screen payment walls.
- Client hide/disable is optional UX later; **server enforcement is the gate**.
- Note: `/coach/teams/:teamId/athletes/:athleteId` is **not registered** in `AppRouter` today; Slice C still locks the **backend** overview/assignment APIs.

### 34.10 Athlete Intelligence boundary

Forbidden in `training.domain` calculators, `DailyAthleteStateGenerationService`, all athlete `Generate*` / `Regenerate*` use cases, `ConsentEffectiveAccessService`, and `OrganizationMembershipPort` (including `canViewTeam` / `canViewTeamReadinessAggregate`): `EntitlementPort`, `CommercialCapability`, Stripe/Apple/Google types.

Allowed: post-authZ surface guard in `GetTeamReadinessUseCase`, `GetCoachAthleteOverviewUseCase`, coach assignment use cases, and the gated org invitation/team-management use cases — **after** V3 404s.

Failed payment restricts gated **surfaces** only (V4 §22.1 #8). Never delete memberships, consent, athlete data, readiness, history, or audit.

### 34.11 Explicit non-goals

- No Individual Premium enforcement
- No Slice D band / seat enforcement
- No Customer Portal / dunning product work
- No live Stripe mutation from this lock
- No production enforcement **activation** in the initial Slice C promotion
- No Slice D

### 34.12 Agent reviews

| Role | Verdict |
| --- | --- |
| Lead / Architect | **PASS** — authZ × entitlement split, org subject, catalog capabilities, no Slice D/G |
| Backend | **PASS** — inventory from live controllers; enforce in named use cases after guards |
| Web | **PASS-WITH-NOTES** — server is the gate; 402 UX + owner-only CTA; coach athlete-detail route is unregistered |
| QA / Test Automation | **PASS-WITH-NOTES** — 8-cell grid expanded in §34.7; pin V3 status+`code`; 402 testable via HTTP + `$.code` |
| Security / Code Quality | **PASS-WITH-NOTES** after required reclass: invite **list/revoke** and **remove-member** stay **Free** (containment / Slice D remediation). Invite **create** remains gated. **402** accepted. AuthZ-before-entitlement required |
| Athlete Intelligence / Data | **PASS** — surface gates only; math/consent/membership commercially blind |
| Documentation / Release | **PASS** — §1 reframed as planning baseline; §34 is the durable lock |

**§34 matrix is Product Owner-approved.** Slice C **runtime** is authorized only after **§34.13** is committed. **Deployment of entitlement code ≠ activation of commercial enforcement.** Production `UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED=true` remains a later explicit commercial-launch gate.

### 34.13 Rollout: deploy code without activating enforcement

**Rule:** `DEPLOYMENT OF ENTITLEMENT CODE ≠ ACTIVATION OF COMMERCIAL ENFORCEMENT`.

Production currently has Stripe **disabled** and no live Organization subscriptions. Deploying Slice C product-edge 402s with no rollout control would commercialize every otherwise-authorized Organization that lacks an entitlement row. That must not happen merely because Slice C code reaches production before commercial launch.

#### Server-owned flag

| Item | Lock |
| --- | --- |
| Name | `UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED` |
| Owner | Server configuration only. **No client** may set, send, or override it |
| Default | **`false`** |
| `false` | Product-edge entitlement **enforcement is inactive**. Existing V1–V3 behavior unchanged. `EntitlementPort` / domain lifecycle still exist and **may be tested**. Billing HTTP remains under existing Slice B rules (`UAP_BILLING_STRIPE_ENABLED`, owner authZ). **Never 402** merely because a subscription row is absent |
| `true` | §34 server-side enforcement is **active**. After AuthN → CSRF → V3 authZ / consent-for-existence, otherwise-authorized actors without the capability receive **402** `COMMERCIAL_ENTITLEMENT_REQUIRED`. Fail closed per §34 |

Do **not** couple this flag to `UAP_BILLING_STRIPE_ENABLED`.

| Flag | Question |
| --- | --- |
| `UAP_BILLING_STRIPE_ENABLED` | May this runtime use the Stripe **provider adapter**? |
| `UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED` | Do **product edges** enforce commercial capability? |

Stripe enabled must **not** silently turn enforcement on. Enforcement enabled must **not** require Stripe at startup.

#### Tests (Slice C)

- All §34 authZ × entitlement cases run with **enforcement=`true`**. Default-off must not make those tests vacuous.
- Regression with **enforcement=`false`**: gated surfaces keep pre-Slice-C V3 status/`code`; **never 402** solely because a subscription is absent.

#### Production

| Stage | Enforcement flag | Meaning |
| --- | --- | --- |
| Initial Slice C production promotion | **`false`** (or absent → default false) | Code may deploy; production regression without premature commercialization |
| Commercial launch | **`true`** only after an **explicit** later gate | Not authorized by this lock |

Commercial-launch gate (document only; **do not authorize** here) must require at least: live billing explicitly authorized; live Stripe configuration complete; live Product/Price catalog approved; live webhook destination configured; tax/classification launch review complete; billing management/recovery sufficiently operable; entitlement matrix verified; no sandbox credentials in production.

#### Safety

This flag must **not**: alter membership, consent, readiness math, or Team Readiness privacy; create fake entitlements; grant billing authority; expose Stripe/provider identity to product modules; be client-controlled; enable itself because Stripe is enabled.

---

## 35. V4 Slice C — COMPLETE on develop (not PRODUCTION VERIFIED)

**Status:** **COMPLETE on `develop`.** Not **PRODUCTION VERIFIED**. V4 is **not** complete. Production enforcement remains **disabled / unmodified**. Slice D is **not** authorized by this section. Slice C deployment ≠ commercial enforcement activation.

### SHAs

| Item | SHA / meaning |
| --- | --- |
| Runtime implementation | `50572362d10ae220958b252c3846dc648cdecce0` — provider-neutral Organization entitlement enforcement behind default-off `UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED` |
| Final Slice C QA-hardening | `8a8c52b1a644f9162227f99d5a671a2f3099ba73` — HTTP pins for remaining §34.7 cells (especially 8e). **This is the independently reviewed develop tip for certification, not a second runtime implementation.** |

Do not treat the runtime SHA as the final certification tip. Do not treat the QA-hardening SHA as a change to product-edge behavior.

### Implementation

| Item | Evidence |
| --- | --- |
| Config flag | `UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED` → `uap.billing.entitlement-enforcement.enabled` |
| Repository default | **`false`** (`application.yaml` `:false`, `.env.example` commented false) |
| Stripe independence | Flag is a sibling of `uap.billing.stripe.enabled`. Enforcement `true` does not require Stripe or credentials. Stripe `true` does not enable enforcement. `OrganizationBillingHttpIntegrationTests` runs with enforcement `true` and still uses existing owner billing authZ only |
| Published contract | Modulith module `entitlements`: `EntitlementPort`, `CommercialEntitlementGuard`, `CommercialEntitlementRequiredException`, `CommercialCapability`, `BillingSubjectType`. Billing implements the port/guard. Organization and training depend on `entitlements` only — not `billing.application` / `billing.domain` / `billing.infrastructure` / Stripe |
| Cycle break | Billing already depends on `organization :: membership` (Slice B owner checks). Product modules therefore cannot depend on the billing module without a cycle. The provider-neutral API was extracted to sibling module `entitlements` |
| 402 contract | HTTP **402** `COMMERCIAL_ENTITLEMENT_REQUIRED`; body `{code,message,timestamp,path,details}`; generic message; no Stripe/Price/subscription/plan/org oracle leakage |
| AuthZ order | Authentication → CSRF → V3 membership/role/IDOR/consent-for-existence → `CommercialEntitlementGuard` → mutation/projection |
| Flyway | **No new migration** (V36 remains current) |
| Stripe mutation | **None** |
| Individual Premium / bands / seats | **Not enforced** |

### Gated surfaces (enforcement=`true`, after V3 authZ)

| Capability | Surfaces |
| --- | --- |
| `ORG_TEAM_MANAGEMENT` | POST org teams; PATCH team; POST team archive; POST org invitations; POST team invitations |
| `ORG_COACH_COLLABORATION` | Coach list/get/create/update TrainingAssignment |
| `ORG_COACH_ATHLETE_VIEW` | GET team athlete overview (after membership/lifecycle; before section-consent projection) |
| `ORG_TEAM_READINESS` | GET team readiness (after `canViewTeamReadinessAggregate`; before aggregate math) |

### Free surfaces (never 402 for missing entitlement)

Create/get/list/rename/archive Organization; get/list/GET Team; invitation list/revoke/accept/decline; membership lists; remove/leave org/team; roster-safe identity; athlete `/me` teams; consent grant/list/revoke/re-grant; transparency; athlete-owned history/state/readiness/recovery/recommendations/generate; billing owner recovery APIs; auth identity surfaces.

### Tests

- Flag bind: absent/false/true; Stripe enabled does not bind enforcement
- Guard unit: disabled no-op; enabled present/absent; `INDIVIDUAL_PREMIUM` rejected on org edges
- Lifecycle matrix via `EntitlementPort` + fixed `Clock` (PENDING, ACTIVE, EXPIRED, PAST_DUE, TRIALING exclusive end, CANCEL_AT_PERIOD_END exclusive end, GRACE_PERIOD exclusive end)
- HTTP enforcement=`false`: team create/update/archive, org/team invite create, coach assignment, overview, team readiness — no subscription, never 402
- HTTP enforcement=`true` + Stripe disabled: 401/CSRF 403/404 never 402; authorized unpaid 402 side-effect free; ACTIVE success; lifecycle HTTP on create-team; individual premium ≠ org capability; foreign paid org remains 404; consent-before-entitlement on assignments; **§34.7 8e** unpaid overview + missing section consent → 402 (not `notShared`); entitled+no section consent → 200 `NOT_SHARED`; unauthenticated GET overview/readiness/assignments → 401 never 402; insufficient role (`ORG_ADMIN` team create, `TEAM_ADMIN` assignment, athlete Team Readiness) + ACTIVE org → existing 404; coach assignment GET/LIST unpaid → 402; consent revoke/re-grant after lapse stays free; team readiness entitled+small cohort → existing `INSUFFICIENT_DATA`; unpaid → 402 read-only; free/control surfaces after lapse
- Web: 402 maps to `COMMERCIAL_ENTITLEMENT`, not UNAUTHORIZED/FORBIDDEN

### Independent reviews

| Role | Verdict | Notes |
| --- | --- | --- |
| QA / Test Automation | **FAIL** then **PASS** | First independent review **FAILED**: §34.7 cell **8e** was not explicitly pinned by HTTP coverage. Production behavior in `GetCoachAthleteOverviewUseCase` was already correct (membership/lifecycle → entitlement → section-consent projection). The gap was the test: the suite expired entitlement **after** granting `READINESS_CATEGORY`, so unpaid + missing section consent could still regress to 200 `notShared`. **Fix** on `8a8c52b`: HTTP coverage for unpaid overview with no section consent → **402** `COMMERCIAL_ENTITLEMENT_REQUIRED` (never `notShared`); unauthenticated GET overview/readiness/assignments → **401** `UNAUTHENTICATED` (never 402); insufficient role + ACTIVE Organization → existing V3 **404** (never 402); coach assignment GET/LIST after lapse → **402**; consent revoke/re-grant after billing lapse remains commercially free. **Final QA:** **PASS** after `8a8c52b` and Verify **35501112746**. |
| Security / Code Quality | **PASS-WITH-NOTES** | No additional product change required. AuthZ remains before entitlement. Paid state is not an existence oracle. 402 only after V3 authorization. Free control/exit surfaces remain free. Gated 402 mutations are side-effect free. Rollout flag is server-owned and defaults **false**. Residual notes were coverage nits (addressed by the QA-hardening commit) and an authorized-only 402-vs-409 archived-team ordering inconsistency — not a promotion blocker while production enforcement stays off. |
| Athlete Intelligence / Data | **PASS** | No product change required. Billing gates only the allowed product-surface use cases. State Engine, readiness/recovery/recommendation calculators, `TeamReadinessSuppressionPolicy`, Team Readiness aggregation math, `ConsentEffectiveAccessService`, and `OrganizationMembershipPort` membership predicates remain commercially blind. One `READINESS_V1` formula. GET team readiness remains stored-read. |

### GitHub Verify and Sonar

**Final authoritative certification run** (QA-hardened tip `8a8c52b`):

- GitHub Verify **[35501112746](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/35501112746)** — **SUCCESS** (Backend core / training-app / training-http / Backend aggregate / Web / Mobile / Sonar)

Historical implementation-era Verify (runtime SHA `5057236` / early docs evidence). These are **not** the final certification run:

- [35500008730](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/35500008730) — success (implementation SHA evidence)
- [35500463011](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/35500463011) — success (docs SHA evidence)

Sonar Quality Gate **PASSED** on `develop`: [dashboard](https://sonarcloud.io/dashboard?id=Devino-Labs-LLC_Universal-Athlete-Platform&branch=develop). New Code (measured after the implementation-era scan; QG still PASS on 35501112746): Reliability A (1.0), Security A (1.0), Maintainability A (1.0), Coverage **100%**, Duplication **0.1%**, Security Hotspots reviewed **100%**; 0 new bugs / vulnerabilities / code smells.

### Production / next (at develop certification)

- `UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED` repository default remains **`false`**.
- Production enforcement has **not** been activated. Production Stripe remains **disabled / unmodified**. Railway variables were **not** changed.
- Slice C code on `develop` ≠ commercial enforcement activation.
- Fast-forward to `main` and production verification are recorded in **§36**. Slice D is **not** authorized.

---

## 36. Slice C — PRODUCTION VERIFIED

**Status:** **V4 Slice C — PRODUCTION VERIFIED**  
**Does not mark V4 complete.** **Does not authorize Slice D.** **Does not activate commercial enforcement.** Live Stripe remains **untouched**.

Production rule preserved: `UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED` is **false** (repository default). Deployment of Slice C code ≠ commercial launch.

### 36.1 Pre-promotion refs and topology

| Item | Value |
| --- | --- |
| Pre-promotion `develop` / `origin/develop` | `03fbdb1a827539bf66557750bf009ebb89e2f7e7` |
| Pre-promotion `main` / `origin/main` | `212f3f44bfe4c8709b636a7839d83c0a978edaa3` |
| Topology | `develop` **9** commits ahead of `main`, **0** behind; merge-base = `main` |
| Range | `c64ba79` … `03fbdb1` (matrix lock, rollout lock, runtime `5057236`, QA-hardening `8a8c52b`, develop certification docs) |
| Method | Solo-maintainer `git merge --ff-only develop` on `main` (no PR, rebase, squash, or force push) |
| Code promotion SHA | `03fbdb1a827539bf66557750bf009ebb89e2f7e7` |
| Runtime implementation SHA | `50572362d10ae220958b252c3846dc648cdecce0` |
| QA-hardening SHA | `8a8c52b1a644f9162227f99d5a671a2f3099ba73` |
| Authoritative develop QA Verify | [35501112746](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/35501112746) **SUCCESS** |
| Develop docs-certification Verify | [35552661133](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/35552661133) **SUCCESS** |

No Flyway `V37+`. No Slice D band/seat enforcement. No Individual Premium enforcement. No Customer Portal / dunning expansion. No live Stripe files in the promotion range.

### 36.2 Runtime main Verify / Sonar

| Item | Value |
| --- | --- |
| Main Verify (promotion SHA `03fbdb1`) | [35553576380](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/35553576380) **SUCCESS** |
| Jobs | Backend core, Backend training-app, Backend training-http, Backend aggregate, Web, Mobile, Sonar Quality Gate — all **success** |
| Quality Gate | **PASSED** (`alert_status=OK`) — https://sonarcloud.io/dashboard?id=Devino-Labs-LLC_Universal-Athlete-Platform&branch=main |
| New Code reliability | **A** (1.0) |
| New Code security | **A** (1.0) |
| New Code maintainability | **A** (1.0) |
| New Code coverage | **100%** |
| New Code duplication | **0.1%** |
| New Code hotspot review | **100%** |

Gate thresholds were not weakened.

### 36.3 Railway production

| Item | Evidence |
| --- | --- |
| Auto-deploy | GitHub environment `Universal Athlete Platform / production` deployment **6560418370** for SHA `03fbdb1…` — **success** (status `2026-09-21T02:16:42Z`). No manual recovery deploy. |
| `UAP_Server` | `https://uapserver-production.up.railway.app` **UP** after auto-deploy |
| `UAP_Client_Web` | `https://uapclientweb-production.up.railway.app` HTTP **200**; bundle `index-BpWtmAn9.js` contains `COMMERCIAL_ENTITLEMENT` / `COMMERCIAL_ENTITLEMENT_REQUIRED` mapping; contains `OrganizationBilling`; no `sk_test_`, `rk_test_`, `whsec_`, `sk_live_`, or Price IDs; no `paywall` string |
| `/actuator/health` | HTTP 200 `{"groups":["liveness","readiness"],"status":"UP"}` |
| `/actuator/health/liveness` | HTTP 200 `{"status":"UP"}` |
| `/actuator/health/readiness` | HTTP 200 `{"status":"UP"}` |

Railway dashboard variable listing was **not** available (no Railway CLI / token in this environment). Enforcement-off and Stripe-off conclusions below do **not** claim a direct Railway variable dump. Repository default `UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED=false` and `UAP_BILLING_STRIPE_ENABLED=false` apply unless a production variable was set `true`. Runtime behavior is consistent with both remaining **false**: healthy boot without Stripe credentials; Stripe webhook controller not registered (dummy `Stripe-Signature` → **401** `/error`); no product-edge **402** while unauthenticated/CSRF-denied.

### 36.4 V36

No Slice C Flyway migration. Latest repository migration remains `V36__create_billing_stripe_org_foundation.sql`.

Production Hibernate `ddl-auto=validate` plus existing billing JPA entities would fail startup if V36 tables were missing. The server stayed healthy after the `03fbdb1` auto-deploy. Direct `flyway_schema_history` evidence was not available.

### 36.5 Production entitlement enforcement remained OFF

| Check | Result |
| --- | --- |
| Repository default | `uap.billing.entitlement-enforcement.enabled: ${UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED:false}` |
| `application-prod.yaml` | Does **not** override the flag |
| This promotion | Railway variables were **not** mutated. The flag was **not** set `true` for smoke testing |
| Runtime | No production **402** `COMMERCIAL_ENTITLEMENT_REQUIRED` observed on unauthenticated protected GETs or CSRF-denied mutations of gated families (create team / archive / invitations, overview, assignments, Team Readiness) |
| Guard | Loaded safely: enforcement default-off is a no-op; no Stripe credentials required for boot |
| Commercial launch | **Not declared.** No production Organization is newly paywalled by this deployment |

### 36.6 Production Stripe remained disabled; live Stripe untouched

| Check | Result |
| --- | --- |
| Repository default | `uap.billing.stripe.enabled: ${UAP_BILLING_STRIPE_ENABLED:false}` |
| This promotion | No sandbox `sk_test_` / `rk_test_` / `whsec_` / Price IDs / localhost Checkout URLs were written to Railway production |
| Webhook | `POST /api/v1/billing/webhooks/stripe` with a dummy `Stripe-Signature` → HTTP **401** `/error` (controller not registered). Enabled Stripe would return **400** for an invalid signature |
| Live Stripe | **Untouched** (no live Products/Prices/Customers/Checkout/Subscriptions/webhooks/keys/Tax) |

### 36.7 V1–V3 regression (enforcement off)

| Check | Result |
| --- | --- |
| Unauthenticated protected GETs | identity `/me`, organizations, athletes, consents, transparency, assignments, readiness, billing GET, Team Readiness, coach overview, invitations, roster, memberships → **401** `UNAUTHENTICATED` — **never 402** |
| CSRF intact | `POST` logout, create Organization, create team, archive team, org/team invitations without CSRF → **403** `CSRF_INVALID` — **never 402** |
| Login CSRF exemption intact | `POST /api/v1/identity/login` invalid credentials → **401** `INVALID_CREDENTIALS` |
| Foreign UUID resources | Unauthenticated still **401** (non-oracle: unpaid/paid not distinguishable before AuthN). Authenticated inaccessible **404** remains the V3 contract; this promotion did not invent production identities to re-probe IDOR |
| Free/control surfaces | Auth/account, invitations, leave/remove, consent, transparency, athlete-owned history/state/readiness, billing recovery remain commercially ungated in code; production smoke showed no **402** while enforcement is off |
| Athlete Intelligence / Team Readiness | No calculator/consent/membership contamination in the promotion range. GET Team Readiness remains stored-read, min cohort 5, complementary suppression, no mega-score, no hidden writes. Commercial enforcement inactive |

Authenticated mutation of live production data was **not** performed.

### 36.8 Security residual note (preserved)

Independent Security / Code Quality review: **PASS-WITH-NOTES**.

When commercial enforcement is **enabled**, an authorized unpaid owner of an **archived** Team may receive **402** rather than existing `TEAM_ARCHIVED` **409** on update/archive, because those use cases check archived status after entitlement. This is **not** an inaccessible-resource oracle.

Because production enforcement is **OFF**, this is **not** a Slice C production-promotion blocker. It is tracked for a later hardening / before-enforcement-activation gate. Runtime behavior was **not** changed in this promotion.

### 36.9 Web

Production Web HTTP **200**. Slice C machine-code mapping is present in the deployed bundle. No secret values in the bundle. No premature paywall UX string. `ORG_OWNER`-only billing authority is unchanged in server code; this promotion did not add ORG_ADMIN/coach financial controls.

### 36.10 Docs-evidence tip

The documentation commit that records this section is pushed on `develop` and fast-forwarded to `main` after its Verify succeeds. Runtime promotion Verify remains **35553576380**. The docs-tip Verify is recorded separately below when complete.

V4 is **not** complete. Slice D is **not** started. Production `UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED=true` remains a later explicit commercial-launch gate.

---

## 37. Pre-Slice-D — Organization Active-Athlete Band & Capacity Lock

**Status:** **PRODUCT OWNER LOCKED** (docs / ADR only). Option **B** is final.  
**Does not authorize Slice D runtime.** Does not authorize Slice E, Customer Portal, plan changes, Individual Premium, Flyway, Stripe mutation, Railway mutation, `main` merge, or commercial launch.

**Baseline:** `main` = `develop` = `0349424d1a05b543370ed9b75d25b58644d53a03` (Slice C **PRODUCTION VERIFIED**). Schema **V36**. Production `UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED` **false**. Production `UAP_BILLING_STRIPE_ENABLED` **false**.

### 37.1 Membership source of truth (runtime inventory)

Athlete commercial usage is **not** an `organization_memberships` count. Org-scoped rows are **ORG_OWNER** (create Organization) and **ORG_ADMIN** (org invitation accept). Athletes exist on **team_memberships**.

| Concept | Runtime |
| --- | --- |
| Organization | `organizations` (`ACTIVE` / `ARCHIVED`); PK lock target for Slice D serialization |
| Team | `teams.organization_id` FK; `ACTIVE` / `ARCHIVED`; unique `(organization_id, name)` |
| OrganizationMembership | `organization_memberships`: roles `ORG_OWNER` / `ORG_ADMIN` in practice; statuses `ACTIVE` / `REMOVED` / `LEFT`; `athlete_id` nullable and constrained to role `ATHLETE` but **no current path creates an org-scoped ATHLETE row** |
| TeamMembership | `team_memberships`: team roles `ATHLETE`, `COACH`, `HEAD_COACH`, `TEAM_ADMIN`; statuses `ACTIVE` / `REMOVED` / `LEFT`; **`athlete_id` set only when role is `ATHLETE`** via `AthleteContextPort.requireAthlete` at invitation accept |
| Active uniqueness | Generated `active_account_id` unique per team (V31); rejoin allowed with a new row after LEFT/REMOVED |
| Invitations | Org-scoped: `ORG_ADMIN` only. Team-scoped: `ATHLETE` / `COACH` / `HEAD_COACH` / `TEAM_ADMIN`. Statuses `PENDING` / `ACCEPTED` / `DECLINED` / `REVOKED` / `EXPIRED` |

**Only runtime path that creates an ACTIVE athlete TeamMembership:** `AcceptInvitationUseCase.acceptTeamInvitation` after a **PENDING** team invitation with role `ATHLETE` (token or `/me` id). There is **no** direct athlete-add API, no org-invitation-as-athlete, and no bootstrap that inserts athlete memberships.

Coach/admin team accepts also create TeamMembership but **`athlete_id` is null** — they are **not** billable athletes.

`ArchiveTeamUseCase` / `ArchiveOrganizationUseCase` persist container status only. They **do not** rewrite membership rows.

`LeaveTeamUseCase` → `LEFT`. `RemoveTeamMemberUseCase` → `REMOVED`. Invitation create does not insert membership.

### 37.2 Canonical billable predicate

An Athlete identity counts **once** for Organization **O** when that `athlete_id` has **at least one** `team_memberships` row such that:

- `status = 'ACTIVE'`
- `role = 'ATHLETE'`
- `athlete_id IS NOT NULL`
- the row’s `teams.organization_id = O`

**Usage** = `COUNT(DISTINCT athlete_id)` under that predicate.

Not: Account count, membership-row count, Team count, invitation count, org-membership count, coach/admin rows.

Same athlete in Org X and Org Y: **independent** counts (1 each). Multiple ACTIVE teams in the same Organization: **1**.

### 37.3 Archived Team / Organization (recommended lock)

ADR-041 already lists non-billable as pending invitation, LEFT, REMOVED. It is **silent** on archive.

**Lock:** ACTIVE athlete TeamMembership **continues to count** until that membership becomes **LEFT** or **REMOVED**. Archiving a Team or Organization **must not** become a hidden usage-deletion mechanism. Archive remains the existing V3 container lifecycle.

Accept onto an archived Team/Organization remains existing **404** (`InvitationNotFoundException`) — not a capacity event.

This is a **clarification of ADR-041**, not a silent change of billable unit.

### 37.4 Transitions that change the distinct count

| Id | Event | Current path | Δ usage |
| --- | --- | --- | --- |
| A | First ACTIVE athlete membership in Org | `AcceptInvitationUseCase` ATHLETE team invite | **+1** |
| B | Already ACTIVE on another Team in same Org; accept another team | same | **+0** |
| C | ACTIVE on A and B; leave A | `LeaveTeamUseCase` | **+0** |
| D | ACTIVE only on A; leave A | `LeaveTeamUseCase` | **−1** |
| E | ACTIVE on A and B; removed from B | `RemoveTeamMemberUseCase` | **+0** |
| F | Final ACTIVE membership REMOVED | `RemoveTeamMemberUseCase` | **−1** |
| G | Pending invitation | `CreateTeamInvitationUseCase` | **+0** |
| H | Decline / revoke / expire | `DeclineInvitationUseCase` / `RevokeInvitationUseCase` / expire-on-accept | **+0** |
| I | LEFT/REMOVED athlete rejoins with no other ACTIVE membership | new ATHLETE accept | **+1** |
| J | Team/Org archive | `ArchiveTeamUseCase` / `ArchiveOrganizationUseCase` | **+0** (memberships unchanged) |
| K | Coach/admin team accept | `AcceptInvitationUseCase` non-ATHLETE | **+0** |
| L | Org-admin accept | `acceptOrgInvitation` | **+0** |

Capacity is evaluated **only** on **+1** (count-increasing) transitions.

### 37.5 Invitation create vs accept

Pending invitations **do not reserve slots**. Creating an ATHLETE team invitation **must not** fail because the Organization is at band maximum (subject to existing Slice C `ORG_TEAM_MANAGEMENT` when that flag is on). Capacity may change between create and accept. **No ghost reserved seats.**

### 37.6 Acceptance at capacity

Invitation **accept remains Slice C commercially FREE**: never `ORG_TEAM_MANAGEMENT`, never HTTP **402** `COMMERCIAL_ENTITLEMENT_REQUIRED`, never billing authority for the athlete.

**Normative principle:** Organization capacity enforcement constrains an **effective commercial band**; absence of an effective commercial band is **not** itself a zero-seat band.

A capacity **409** `ORGANIZATION_ATHLETE_CAPACITY_UNAVAILABLE` requires **all** of:

1. valid V3 invitation / authentication lifecycle
2. ATHLETE role
3. distinct active-athlete count **would increase**
4. **exactly one** effective commercial Organization subscription
5. that subscription supplies a fixed band (`ORG_BAND_25` / `75` / `250`)
6. current distinct count is already **at or above** that band’s maximum

No effective band → **no capacity denial** (existing V3 accept). Multiple effective subscriptions → §37.9 (fail-closed **+1** only; not “no plan”).

When those six conditions hold:

| Item | Lock |
| --- | --- |
| HTTP | **409 Conflict** (fits existing invitation/membership conflict handler) |
| Code | `ORGANIZATION_ATHLETE_CAPACITY_UNAVAILABLE` |
| Message (invitee) | `This organization cannot add another active athlete at this time.` |
| Body | Existing `{code,message,timestamp,path,details}` with **`details` empty** |
| Surfaces | Both `POST /api/v1/invitations/{rawToken}/accept` and `POST /api/v1/me/invitations/{invitationId}/accept` |
| Forbidden in body | Stripe, Price IDs, plan keys, band names, used/limit, `remainingCapacity`, payment status, subscription ids, other Organizations, owner financial state |

**Precedence (must not invert):** unauthenticated → **401**; missing CSRF → **403** `CSRF_INVALID`; invalid/foreign/expired/revoked invitation → **404** `INVITATION_NOT_FOUND`; `EMAIL_UNVERIFIED` / `ATHLETE_PROFILE_REQUIRED` / `MEMBERSHIP_ALREADY_ACTIVE` stay those **409** codes even at cap. Idempotent re-accept of already `ACCEPTED` at cap → existing **200**. Skip capacity on org-admin and non-ATHLETE team roles. `INDIVIDUAL_PREMIUM` on the invitee Account **never** grants Organization band capacity.

Denied accept is **atomic**: invitation remains **PENDING**; no TeamMembership; `acceptIfPending` not committed; no `acceptedAt`; no `invitationAccepted` / `membershipActivated` audit; no success email. Decline/revoke remain available after 409. Athlete may retry later.

**Do not use 402** for this case. Do not throw `CommercialEntitlementRequiredException` from accept.

### 37.7 Zero-delta existing athlete

At 25/25 (or EXPIRED / no subscription): an Athlete **already counted** in that Organization accepting another Team invitation **succeeds** under existing V3 invitation rules. Distinct usage unchanged. Dedicated Slice D tests required.

### 37.8 When numeric band capacity applies

Reuse `Subscription.isCommerciallyEntitledAt` exclusive-end semantics. Do **not** re-code lifecycle at the membership edge. Do **not** inspect Stripe Price IDs.

**Organization capacity enforcement constrains an effective commercial band; absence of an effective commercial band is not itself a zero-seat band.**

| Effective commercial band (`isCommerciallyEntitledAt` = true, exactly one org subscription) | Numeric capacity on **+1** ATHLETE accept |
| --- | --- |
| `TRIALING` before `trialEndsAt` | Enforce current band max |
| `ACTIVE` | Enforce current band max |
| `GRACE_PERIOD` before `graceEndsAt` | Enforce current band max |
| `CANCEL_AT_PERIOD_END` before `currentPeriodEndsAt` | Enforce current band max |

| No effective commercial band | **+1** / zero-delta ATHLETE accept |
| --- | --- |
| No org subscription row | Existing V3 **success** — not 409, not 402 |
| `PENDING` | Existing V3 **success** |
| `PAST_DUE` | Existing V3 **success** |
| `EXPIRED` | Existing V3 **success** |
| `TRIALING` at/after `trialEndsAt` | Existing V3 **success** |
| `GRACE_PERIOD` at/after `graceEndsAt` | Existing V3 **success** |
| `CANCEL_AT_PERIOD_END` at/after `currentPeriodEndsAt` | Existing V3 **success** |

These no-band states are **not** commercially entitled. Slice C product-edge capabilities may still **402** when `UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED=true`. Invitation **accept** remains outside that gate.

Outstanding-invite scenario: entitled org issues a valid ATHLETE invitation; commercial relationship **expires before accept**; athlete accepts under V3 rules → **SUCCESS** (must be tested in Slice D).

Loss of an effective band **never** auto-removes members.

Existing counted athlete additional Team: **allow** at any boundary, with or without an effective band.

`INDIVIDUAL_PREMIUM` never supplies an Organization band. Staff/coach TeamMemberships never consume athlete capacity even if that Account also has an Athlete profile.

### 37.9 Multiple effective Organization subscriptions

`uk_billing_subscriptions_provider_sub_ref` does **not** unique-constrain `(subject_type, subject_id)`. More than one row per Organization is possible. Slice C `EntitlementQueryService` **unions** capabilities — **Slice D must not union or sum bands** and must not pick the highest band.

| Effective temporally entitled org subscriptions | Behavior |
| --- | --- |
| Exactly one | Use that band’s max for **+1** |
| Zero | **Not a zero-seat band.** Valid ATHLETE accept follows V3 (**success**) |
| Two or more | **Fail closed** for **+1** only (ambiguous/corrupt commercial state, **not** absence of a relationship). Invitee still sees generic **409** `ORGANIZATION_ATHLETE_CAPACITY_UNAVAILABLE`. Operators see an internal invariant / log (no Stripe ids, no other-org data). Do not union, sum, pick highest, or pick lowest. Zero-delta extra Team still **succeeds**. |

### 37.10 Hard band boundaries

Allowed distinct ACTIVE athletes: `ORG_BAND_25` → **0..25**; `ORG_BAND_75` → **0..75**; `ORG_BAND_250` → **0..250**.

24→25 success; 25→26 **409**. 74→75 success; 75→76 **409**. 249→250 success; 250→251 **409**. Inclusive max; no off-by-one.

### 37.11 Provider-neutral capacity architecture / Modulith

Billing already depends on `organization :: membership`. Organization must **not** depend on `billing.application` / `billing.domain` / Stripe.

**Lock:** publish on the existing leaf module `entitlements` (same cycle-break as Slice C):

Conceptual seam: `OrganizationCommercialCapacityPort.allowance(organizationId)` → provider-neutral snapshot:

- whether an **effective band exists** (exactly one entitled org subscription)
- if none: **admit** (no numeric denial)
- if exactly one: band identity + `maxActiveAthletes`
- if two or more entitled org rows: **fail closed** for **+1** (same generic 409; not exposed as “duplicate subscription”)

No Stripe types, Price IDs, Customer IDs, JPA entities, or mutable seat counters.

Billing **implements** the port using `Subscription.isCommerciallyEntitledAt` + existing `OrganizationAthleteBand` (keep band integers in billing; published view stays entitlements-neutral).

Organization **consumes** the port **only** on ATHLETE team accept after V3 invitation validity, and **only** if the athlete is not already counted.

Existing `CommercialEntitlementGuard` / Slice C capabilities are **orthogonal**. Invitation accept **never** calls the 402 guard.

Optional helper (Slice E / launch hardening, **not** Stripe Checkout in Slice D): `minimumRequiredBandFor(activeAthleteCount)` → `BAND_25` for 0..25, `BAND_75` for 26..75, `BAND_250` for 76..250; **>250** cannot be satisfied by initial self-service bands.

**Forward requirement:** an Organization whose `activeAthleteCount` exceeds a target band **must not** be allowed to make that undersized band effectively valid through initial purchase, downgrade, or plan change without remediation (example: count 40 → `ORG_BAND_25` must not become effective; count 100 → 25 and 75 insufficient; count >250 → no initial self-service band). Option B must not be used later to bypass the commercial tier model. This is **not** Slice D plan-management implementation.

### 37.12 Concurrency (T18)

Do **not** `SELECT count` then insert without serialization. No JVM locks. No Redis. No cached `seatCount`. Optimistic retry alone can commit 26 rows.

**Lock:** `SELECT … FROM organizations WHERE id = ? FOR UPDATE` (`LockModeType.PESSIMISTIC_WRITE`), matching existing invitation / athlete / billing-customer pessimistic patterns.

**Transaction:** same `@Transactional` as `AcceptInvitationUseCase`.

**Order (deadlock-safe):**

1. Existing invitation `FOR UPDATE` (token hash or id) — already implemented
2. Existing V3 invitation identity / email / PENDING / expiry / org-team graph / archived-container **404**s
3. **Then** Organization `FOR UPDATE`
4. Resolve `athleteId`; compute `alreadyCounted` + `COUNT(DISTINCT athlete_id)` under the org lock
5. If **+1** needed, consult `OrganizationCommercialCapacityPort`
6. `acceptIfPending` + insert TeamMembership + existing success audits

Two **different** new athletes at 24/25: one commit to 25, one **409**, never 26.

Same athlete two Teams at 24/25: both may succeed; distinct count **25**.

Leave/remove **do not** take the Organization write lock (must never be trapped). Isolation may cause a conservative **409** if a concurrent leave has not committed; retry is allowed. They cannot produce 26.

### 37.13 Canonical count query / indexes

```sql
SELECT COUNT(DISTINCT tm.athlete_id)
FROM team_memberships tm
INNER JOIN teams t ON t.id = tm.team_id
WHERE t.organization_id = ?
  AND tm.status = 'ACTIVE'
  AND tm.role = 'ATHLETE'
  AND tm.athlete_id IS NOT NULL
```

Drive from `idx_teams_organization_id` + `idx_team_memberships_team_id`. Scale is **≤250** active plus modest historical LEFT/REMOVED. **No V37 required** for correctness at this scale. If later `EXPLAIN` on production-like data shows a hot path, the minimal index is `(team_id, status, role, athlete_id)` — justify then; do not add a migration in this lock.

**Derived count only.** Do not persist a billing `seatCount`.

### 37.14 Over-capacity / legacy

If distinct count already exceeds the effective band (legacy, downgrade race, import, bug, **or unpaid/no-band accumulation via Option B outstanding invites**): keep memberships; leave/remove/decline/revoke remain allowed; **no +1** while an effective band exists and count ≥ max; zero-delta additional Team **allowed**; owner usage read model reports the **actual** distinct count (`overCapacity=true` when a band exists and count exceeds it). **Never auto-remove**, auto-archive Teams, or pick victims. Unpaid Organizations do **not** receive paid coach/product capabilities from Option B.

### 37.15 Authorization order (non-oracle)

Invitation accept:

1. Authentication
2. Invitation validity / email match / PENDING / expiry (invalid → existing **404**, never capacity)
3. Resolve Team + Organization from the **valid** invitation
4. Existing invitation **409**s (`EMAIL_UNVERIFIED`, `ATHLETE_PROFILE_REQUIRED`, `MEMBERSHIP_ALREADY_ACTIVE`) — these beat capacity
5. Skip capacity for non-ATHLETE roles and for already-`ACCEPTED` idempotent replay
6. `alreadyCounted` for this `athleteId` in that Organization (under Organization `FOR UPDATE`)
7. Capacity **only if** count would increase
8. Atomic membership + accept

Owner capacity GET: existing **ORG_OWNER** billing authority first, then snapshot. Non-owners: existing **404**, not usage numbers.

### 37.16 Owner usage read model / visibility

`GET /api/v1/billing/organizations/{organizationId}` is **Stripe-conditional** today (`@ConditionalOnProperty` Stripe enabled) and **ORG_OWNER-only**. Independent Web review prefers **additive fields on that GET**. That is correct **when the controller is registered**.

**Lock:** owner usage must work with **Stripe disabled** (capacity flag is independent of Stripe). Therefore Slice D must either (a) split/uncondition a read-only owner GET, or (b) keep the existing Stripe-gated GET for subscription fields **and** add a Stripe-independent owner snapshot path with the **same** 404 owner oracle. Do not create a roster/org-list usage API.

Suggested fields: `activeAthleteCount`, `bandCapacity` (nullable if no effective band), `remainingCapacity`, `atCapacity`, `overCapacity`. **No athlete identity lists. No provider secrets. Not metered billing.** Non-owner / foreign org remain **404** `ORGANIZATION_NOT_FOUND`, never 402, never usage numbers.

Do **not** grant this screen to ORG_ADMIN, TEAM_ADMIN, COACH, HEAD_COACH, or ATHLETE.

Web Slice D: optional `"23 of 25 active athletes"` for owner. No Slice E portal/upgrade UX. No paywall. Invitee 409 maps as **conflict**, not unauthorized and not 402 commercial entitlement.

### 37.17 Rollout flag (do not implement in this task)

`UAP_BILLING_ORGANIZATION_CAPACITY_ENFORCEMENT_ENABLED` — server only; default **false**; not a client flag; not on request DTOs.

| Value | Behavior |
| --- | --- |
| `false` / absent | No accept denied for band capacity. V1–V3 membership unchanged. Count/read may exist for tests |
| `true` + no effective band | Invite acceptance follows **V3** (no 409, no 402 from capacity) |
| `true` + exactly one effective band | Numeric band enforcement on **+1** |
| `true` + two or more effective org subscriptions | **+1** generic 409 (invariant); zero-delta still succeeds |

Wiring when runtime is authorized (sibling of Slice C, **not** nested under Stripe or entitlement; comment must not say 402):

```yaml
uap:
  billing:
    organization-capacity-enforcement:
      enabled: ${UAP_BILLING_ORGANIZATION_CAPACITY_ENFORCEMENT_ENABLED:false}
```

Do not add the flag to `application-prod.yaml`, CI, or web/mobile env. Java default `false`. A client header/query/body must not override it. First Slice D production promotion: **unset / false**.


| Entitlement | Capacity | Meaning |
| --- | --- | --- |
| false | false | Current production |
| true | false | Paid surfaces 402; membership capacity not enforced (controlled test only — **not** commercial launch) |
| false | true | Capacity testable; no product-edge 402 from this flag |
| true | true | Intended Organization commercial launch after all V4 gates |

Neither flag silently enables the other.

### 37.18 Free / decrease operations

Capacity **never** blocks: leave Team / leave Org (existing rules), remove member, revoke/decline invite, consent revoke, Team/Org archive, billing recovery, athlete-owned history/state/readiness.

Team archive **must not** auto-REMOVED athletes, globally revoke consent, or decrement usage via hidden membership mutation.

### 37.19 Athlete Intelligence / Stripe / audit

Capacity touches **membership activation only**. Forbidden: State Engine, readiness/recovery/recommendation math, Team Readiness (min cohort 5, complementary suppression), consent scopes, athlete history, health-data or workout-volume meters.

Stripe: **no** quantity, usage records, metered billing, catalog/live objects. One recurring Organization subscription; **fixed band ≠ Stripe seat quantity**.

Audit: success remains existing `invitationAccepted` + `membershipActivated`. Do **not** duplicate a billing audit event. Denied **+1**: no success audit. Optional later `security_audit_events` for denial may include actor + organization + invitation ids **without** plan, Stripe, or usage numbers. Not required to start Slice D.

### 37.20 Future Slice D test matrix (mandatory)

**Counting:** 0; one athlete one Team; one athlete many Teams same Org = 1; same athlete two Orgs independent; LEFT; REMOVED; pending invite = 0.

**Boundaries:** 24→25; 25 blocked; 74→75; 75 blocked; 249→250; 250 blocked. JDBC-seed roster to N−1/N/N+1; do not HTTP-register 249 athletes.

**Zero-delta:** 25/25 existing athlete second Team **success**; no-row / EXPIRED / PAST_DUE existing athlete second Team **success** (separate fixtures).

**No effective band (CAPACITY=true):** no-row / EXPIRED / PAST_DUE / PENDING + **first** distinct athlete → **SUCCESS** (never capacity 409, never 402). Keep no-row vs EXPIRED as separate fixtures.

**Outstanding invite:** org entitled when invite created; relationship expires before accept; valid ATHLETE accept → **SUCCESS**.

**Effective band:** 25/25 BAND_25 + new athlete → 409; 75/75; 250/250. 24→25 / 74→75 / 249→250 success.

**Lifecycle (exactly one entitled row):** TRIALING / ACTIVE / GRACE-before-end / CANCEL-before-end enforce numeric max. Temporal **at-end** of those states is **no effective band** → V3 success, not 409.

**Concurrency:** 24/25 two distinct accepts → one success, one 409, final **25**; 24/25 same athlete two Teams → both valid, distinct **25**; existing same-token idempotency still exactly one membership.

**Atomic 409:** invite PENDING; no membership; no `INVITATION_ACCEPTED` / `MEMBERSHIP_ACTIVATED`. Then decline still succeeds. Replay of a **successful** 24→25 token at 25/25 stays **200**.

**Over-cap (26 on band 25):** remove/leave allowed; no new distinct athlete; existing athlete extra Team allowed. Invite **create** at 25/25 and 26/25 still **201** (pending does not reserve). 25 ACTIVE + N PENDING → new distinct accept still 409.

**Non-athlete at cap:** COACH / HEAD_COACH / TEAM_ADMIN / ORG_ADMIN accept at 25/25 **success**; distinct athlete count unchanged.

**LEFT/REMOVED rejoin:** with an effective band at cap and no other ACTIVE membership → **+1** 409; still ACTIVE on another team → **+0** success. No effective band → V3 success.

**Dual HTTP surfaces:** token accept and `/me/{id}/accept` both pin 409 + PENDING leftover.

**AuthZ / oracle:** garbage/blank/revoked/expired/wrong-email/foreign UUID → **404** `INVITATION_NOT_FOUND` while another org is at cap. Unauthenticated **401**. CSRF **403** `CSRF_INVALID`. Never 409 capacity, never 402.

**Conflict precedence at cap:** unverified → `EMAIL_UNVERIFIED`; missing profile → `ATHLETE_PROFILE_REQUIRED`; already on that team → `MEMBERSHIP_ALREADY_ACTIVE`.

**409 body:** `ORGANIZATION_ATHLETE_CAPACITY_UNAVAILABLE`; empty `details`; no plan/Stripe/price/usage/remainingCapacity.

**Accept never 402:** `ENTITLEMENT=true` + `CAPACITY=true` count-increasing denial is **409**, not `COMMERCIAL_ENTITLEMENT_REQUIRED`.

**Flags:** absent/false → V3 success at over-band (never capacity 409). `true` + no effective band → V3 success. `true` + one band → numeric 409 at max. Independent of Stripe and entitlement. Client header cannot override.

**Owner GET:** ORG_OWNER snapshot; non-owner 404 without usage numbers. Not folded into invitation tests.

Reuse: `VerifiedAccountFixture`, `ConsentHttpFixtures` (do **not** use `acceptInvite` on 409 paths), `OrganizationSubscriptionFixtures` with a plan-key argument, `CountDownLatch` sibling of `InvitationAcceptConcurrencyIntegrationTests`. Dedicated Slice D HTTP class; do not append to the invitation lifecycle god-test.

### 37.21 Performance

Prefer the relational distinct count + Organization `FOR UPDATE`. Complexity O(memberships in org teams), expected tiny (≤250 active). Reject Redis counters, in-memory locks, event-sourced seat ledgers, Stripe Usage API.

### 37.22 Independent reviews (post-lock)

| Role | Verdict | Notes |
| --- | --- | --- |
| Lead / Architect | **PASS-WITH-NOTES** | No ADR-046. Option **B** is now PO-locked. |
| Backend | **PASS** | Sole +1 writer is ATHLETE team accept. Count via `team_memberships ⋈ teams`. Port must **admit** when no effective band. |
| QA / Test Automation | **PASS-WITH-NOTES** | §37.20 pins no-band SUCCESS vs at-band 409 vs outstanding-invite SUCCESS. |
| Security / Code Quality | **PASS** (on Option B) | Unpaid/no-plan is not a synthetic zero-seat gate. 409 only for actual band overflow or duplicate-effective-sub invariant. |
| DevOps / CI-CD | **PASS** | Sibling YAML default false; first production Slice D promotion leaves flag unset/false. |
| Web | **PASS-WITH-NOTES** | Owner usage line; invitee 409-by-`code`; no upgrade CTA. |
| Athlete Intelligence / Data | **PASS** | Capacity only on athlete TeamMembership activation. |
| Documentation / Release | **PASS** | Option B locked. Slice D runtime still separately unauthorized. |

### 37.23 Product Owner resolution — Option B

**Locked.** No commercially entitled Organization plan does **not** impose a band limit on invitation acceptance.

**Rationale:**

- invitation acceptance remains an athlete/control surface, not a payment gate
- billing state changing after an invitation is issued must not strand the athlete
- treating “no plan” as zero capacity would still paywall accept even if the status were 409 instead of 402
- capacity is the limit of an **effective purchased band**, not a synthetic zero-seat plan
- Slice C entitlement enforcement still controls paid-workspace **invitation create** at commercial launch
- outstanding invitations remain acceptable after lapse
- over-capacity truth is retained; memberships are never auto-pruned
- unpaid Organizations **do not** receive paid coach/product capabilities

No unresolved Product Owner decision remains for Slice D semantics.

Slice D **runtime** still requires a **separate explicit authorization**.

Production `UAP_BILLING_ORGANIZATION_CAPACITY_ENFORCEMENT_ENABLED=true` is **not** authorized here.

---

## 38. Slice D — Organization Active-Athlete Capacity (COMPLETE on develop)

**Status:** **COMPLETE on develop**. Not **PRODUCTION VERIFIED**. Does not authorize Slice E, commercial launch, production capacity `true`, production entitlement `true`, Stripe on, Railway mutation, or `main` merge.

**Baseline:** `main` = `0349424d1a05b543370ed9b75d25b58644d53a03` (unchanged). Schema remains Flyway **V36**.

### 38.1 Implementation evidence

| Item | Evidence |
| --- | --- |
| Runtime commit | `855ddf4ab32ba2474655b5df7f365fe234c2daa7` |
| Architecture | Leaf `entitlements.OrganizationCommercialCapacityPort`; billing implements; organization consumes. No `organization → billing` |
| Option B | `NoEffectiveBand` does not deny accept. No 402 on accept |
| Numeric 409 | Exactly one effective band + ATHLETE +1 at/above max → `ORGANIZATION_ATHLETE_CAPACITY_UNAVAILABLE` |
| Ambiguous effective subs | +1 generic 409; +0 succeeds |
| Flag | `UAP_BILLING_ORGANIZATION_CAPACITY_ENFORCEMENT_ENABLED` → `uap.billing.organization-capacity-enforcement.enabled` default **false**. Independent of Stripe and entitlement. Not in `application-prod.yaml` or clients |
| Canonical count | `COUNT(DISTINCT athlete_id)` ACTIVE ATHLETE `team_memberships ⋈ teams` |
| Serialization | Invitation `FOR UPDATE` then Organization `FOR UPDATE`; accept uses `READ_COMMITTED` so the post-lock count is a current read (T18) |
| Owner snapshot | Always-on `GET /api/v1/billing/organizations/{id}/capacity` (not Stripe-conditional). ORG_OWNER via `canManageOrganization`; else 404 without usage |
| Conflict contract | HTTP 409, empty `details`, no plan/Stripe/usage in body |
| Tests | Band boundaries 25/75/250; Option B no-band cells; dual accept APIs; PENDING after denial; replay; staff at cap; LEFT/REMOVED rejoin; invite create at max; concurrent distinct 24/25; concurrent same-athlete two teams; both flags 409≠402; owner GET |
| Migration | None (V36 remains latest) |
| Stripe | No Checkout/quantity/usage/catalog mutation |
| Independent reviews | Lead **PASS-WITH-NOTES**; Backend **PASS-WITH-NOTES**; QA **PASS-WITH-NOTES** after REMOVED + both-flags HTTP pins; Security **PASS-WITH-NOTES**; DevOps **PASS-WITH-NOTES**; Web **PASS-WITH-NOTES**; Athlete Intelligence **PASS**; Documentation **PASS-WITH-NOTES** |
| Local verification | Focused Slice D + Modulith + invitation concurrency green. Web typecheck/lint/test/production build green. Mobile typecheck/lint/test green. Full local `./gradlew test` hit Testcontainers connection pressure (unrelated terminal-lifecycle tests); GitHub Verify is the full-suite gate |
| Production flags | Capacity remains default-off / unauthorized. Entitlement production remains false. Stripe production remains off |

Owner snapshot for **ambiguous** effective subscriptions returns actual `activeAthleteCount` and **null** band fields (does not fabricate a numeric band). That is intentional.

Slice E undersized-plan purchase constraint remains docs-only.

V4 is **not** complete.

---

## 39. Slice D — PRODUCTION VERIFIED

**Status:** **V4 Slice D — PRODUCTION VERIFIED**  
**Does not mark V4 complete.** **Does not authorize Slice E.** **Does not activate capacity enforcement, entitlement enforcement, or Stripe.** Live Stripe remains **untouched**. Commercial launch is **not** declared.

Deployment of Slice D code ≠ activation of Organization active-athlete band enforcement. Production `UAP_BILLING_ORGANIZATION_CAPACITY_ENFORCEMENT_ENABLED` remains **false / unset** (repository default).

### 39.1 Pre-promotion refs and topology

| Item | Value |
| --- | --- |
| Pre-promotion `develop` / `origin/develop` | `1563b684b81e698aaaeaa2abb835f5f141f6201c` |
| Pre-promotion `main` / `origin/main` | `0349424d1a05b543370ed9b75d25b58644d53a03` |
| Topology | `develop` **5** commits ahead of `main`, **0** behind; merge-base = `main` |
| Range | `c1b4cd1` … `1563b68` (capacity lock, review amendments, Option B lock, runtime `855ddf4`, §38 evidence) |
| Method | Solo-maintainer `git merge --ff-only develop` on `main` (no PR, rebase, squash, or force push) |
| Code promotion SHA | `1563b684b81e698aaaeaa2abb835f5f141f6201c` |
| Runtime implementation SHA | `855ddf4ab32ba2474655b5df7f365fe234c2daa7` |
| Authoritative develop Verify | [35623777521](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/35623777521) **SUCCESS** |

Promotion range contains the provider-neutral capacity port, billing capacity service, default-off flag, `COUNT(DISTINCT athlete_id)` queries, Organization `FOR UPDATE`, invitation-accept 409 contract, Option B, owner `GET /capacity`, minimal Web mapping, concurrency tests, and §38. It does **not** contain Slice E Portal/upgrade/downgrade, Stripe quantity/metered usage, Individual Premium, Flyway `V37+`, or production flag activation.

### 39.2 Runtime main Verify / Sonar

| Item | Value |
| --- | --- |
| Main Verify (promotion SHA `1563b68`) | [35632195827](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/35632195827) **SUCCESS** |
| Jobs | Backend core, Backend training-app, Backend training-http, Backend aggregate, Web, Mobile, Sonar Quality Gate — all **success** |
| Quality Gate | **PASSED** (`alert_status=OK`) — https://sonarcloud.io/dashboard?id=Devino-Labs-LLC_Universal-Athlete-Platform&branch=main |
| New Code reliability | **A** (1.0) |
| New Code security | **A** (1.0) |
| New Code maintainability | **A** (1.0) |
| New Code coverage | **88.5%** |
| New Code duplication | **0.0%** |
| New Code hotspot review | **100%** |

Gate thresholds were not weakened.

### 39.3 Railway production

| Item | Evidence |
| --- | --- |
| Auto-deploy | GitHub environment `Universal Athlete Platform / production` deployment **6574191471** for SHA `1563b68…` — **success** (`2026-09-21T17:30:34Z`). No manual recovery deploy |
| `UAP_Server` | `https://uapserver-production.up.railway.app` **UP** after auto-deploy |
| `UAP_Client_Web` | `https://uapclientweb-production.up.railway.app` HTTP **200**; entry `index-DwAHCcB2.js`; lazy `OrganizationBillingPage-BMQrpzIp.js` contains owner usage / `/capacity`; lazy `errors-DZPGJGSa.js` maps `ORGANIZATION_ATHLETE_CAPACITY_UNAVAILABLE` to the generic invitee copy. No `sk_test_`, `rk_test_`, `whsec_`, `sk_live_`, Price IDs, `paywall`, or upgrade CTA in those chunks |
| `/actuator/health` | HTTP 200 `{"groups":["liveness","readiness"],"status":"UP"}` |
| `/actuator/health/liveness` | HTTP 200 `{"status":"UP"}` |
| `/actuator/health/readiness` | HTTP 200 `{"status":"UP"}` |

Railway dashboard variable listing was **not** available (no Railway CLI / token in this environment). Capacity-off, entitlement-off, and Stripe-off conclusions below do **not** claim a direct Railway variable dump. Repository defaults `UAP_BILLING_ORGANIZATION_CAPACITY_ENFORCEMENT_ENABLED=false`, `UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED=false`, and `UAP_BILLING_STRIPE_ENABLED=false` apply unless a production variable was set `true`. Runtime behavior is consistent with all remaining **false**: healthy boot without Stripe credentials; Stripe webhook controller not registered; owner capacity GET registered; no **402** or `ORGANIZATION_ATHLETE_CAPACITY_UNAVAILABLE` on unauthenticated or CSRF-denied requests.

### 39.4 V36

No Slice D Flyway migration. Latest repository migration remains `V36__create_billing_stripe_org_foundation.sql`.

Production Hibernate `ddl-auto=validate` plus existing billing/organization JPA entities would fail startup if V36 tables were missing. The server stayed healthy after the `1563b68` auto-deploy. Direct `flyway_schema_history` evidence was not available.

### 39.5 Production capacity enforcement remained OFF

| Check | Result |
| --- | --- |
| Repository default | `uap.billing.organization-capacity-enforcement.enabled: ${UAP_BILLING_ORGANIZATION_CAPACITY_ENFORCEMENT_ENABLED:false}` |
| `application-prod.yaml` | Does **not** override the flag |
| This promotion | Railway variables were **not** mutated. The flag was **not** set `true` for smoke testing |
| Runtime | Unauthenticated / CSRF-denied requests never returned **409** `ORGANIZATION_ATHLETE_CAPACITY_UNAVAILABLE` or **402**. Capacity GET is authenticated **401** `UNAUTHENTICATED` (controller registered), not missing-controller `/error` |
| Certified 409 behavior | Remains on develop/CI (`OrganizationAthleteCapacityHttpIntegrationTests`, concurrency class). **Not activated** in production |
| Commercial launch | **Not declared.** Production is **not** commercially enforcing 25/75/250 bands |

### 39.6 Production entitlement enforcement remained OFF; Stripe remained disabled

| Check | Result |
| --- | --- |
| Entitlement default | `uap.billing.entitlement-enforcement.enabled: ${UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED:false}` |
| Stripe default | `uap.billing.stripe.enabled: ${UAP_BILLING_STRIPE_ENABLED:false}` |
| This promotion | No Railway mutation. No sandbox/live `sk_test_` / `rk_test_` / `whsec_` / Price IDs / Checkout URLs written |
| Stripe webhook | `POST /api/v1/billing/webhooks/stripe` with dummy `Stripe-Signature` → HTTP **401** `/error` (controller not registered). Enabled Stripe would return **400** for an invalid signature |
| Live / sandbox Stripe | **Untouched** (no Products/Prices/Customers/Checkout/Subscriptions/quantity/usage/webhooks/Tax) |

### 39.7 Owner capacity endpoint vs Stripe-conditional billing

`GET /api/v1/billing/organizations/{organizationId}/capacity` is always registered (`OrganizationCapacityController` is **not** `@ConditionalOnProperty` Stripe). Unauthenticated production request → **401** `UNAUTHENTICATED` with that path — not 404-from-missing-controller, not 402, not capacity 409.

Authenticated owner snapshot semantics (ORG_OWNER via `canManageOrganization`; foreign 404 without usage) remain covered by develop integration tests. No production ORG_OWNER identity was invented for this promotion.

Slice B `OrganizationBillingController` / Stripe webhook remain Stripe-conditional and absent while Stripe is off.

### 39.8 Auth / CSRF / Option B / invitation safety

| Check | Result |
| --- | --- |
| Unauthenticated protected GETs | identity `/me`, organizations, athletes, assignments, billing GET, capacity GET → **401** `UNAUTHENTICATED` — **never 402** / never capacity **409** |
| CSRF intact | `POST` logout, create Organization, org invitations without CSRF → **403** `CSRF_INVALID` — **never 402** / never capacity **409** |
| Login CSRF exemption intact | `POST /api/v1/identity/login` invalid credentials → **401** `INVALID_CREDENTIALS` |
| Option B in deployed source | `AcceptInvitationUseCase` admits `NoEffectiveBand` (not a zero-seat band). Flag off skips the gate entirely |
| Production invitation mutation | **Not performed.** With the flag off, accept cannot be denied for band capacity |

### 39.9 Concurrency evidence (certified, not reproduced in production)

Do **not** manufacture production 24/25 roster load.

| Case | Evidence |
| --- | --- |
| Two distinct athletes at 24/25 | `OrganizationAthleteCapacityConcurrencyIntegrationTests` — one success, one 409, final distinct **25**. Certified on develop Verify **35623777521** and main Verify **35632195827** |
| Same athlete, two Teams at 24/25 | Same class — both may succeed, final distinct **25** |

### 39.10 Athlete Intelligence boundary

Promotion range contains **no** State Engine, readiness, recovery, recommendation, Team Readiness, consent, training-load, or athlete-history files. Capacity remains membership/commercial infrastructure only. Min cohort 5 and complementary suppression are unchanged.

### 39.11 Independent reviews (preserved)

| Role | Verdict |
| --- | --- |
| QA / Test Automation | **PASS-WITH-NOTES** after REMOVED rejoin and both-flags 409≠402 HTTP pins were added. Those notes are not promotion blockers after final implementation and Verify |
| Security / Code Quality | **PASS-WITH-NOTES**. Notes (ambiguous owner snapshot honesty, additional oracle-matrix cells) are not promotion blockers with production capacity **OFF** |

### 39.12 Undersized-plan requirement (deferred)

`activeAthleteCount` must constrain a future initial purchase / downgrade / plan change (example: 40 athletes → `BAND_25` must not become effective; 100 → 25 and 75 insufficient; >250 → no current self-service band). **Not implemented** in this promotion. Remains Slice E / launch hardening.

### 39.13 Docs-evidence tip

The documentation commit that records this section is pushed on `develop` and fast-forwarded to `main` after its Verify succeeds. Runtime promotion Verify remains **35632195827**. Develop and main docs-tip Verify run IDs are distinct from that runtime promotion Verify.

Slice E is **not** started. Production `UAP_BILLING_ORGANIZATION_CAPACITY_ENFORCEMENT_ENABLED=true` remains a later explicit commercial-launch gate.

V4 is **not** complete.

---

## 40. Pre-Slice-E — Organization Billing Management Lock

**Status:** Pre-Slice-E billing-management contract is documented; Slice E runtime is **not** authorized. Does not authorize Slice F, Slice G, production Stripe, entitlement enforcement, capacity enforcement, Railway mutation, live or sandbox Stripe mutation, Flyway, or commercial launch.

**Baseline:** `main` = `develop` = `c3e6ffd4ebacdd44d723269d1b60637de90e8b49` (Slice D **PRODUCTION VERIFIED**). Schema **V36**. `stripe-java` **33.4.2**. Production flags remain false / unset.

**Question:** How may an ORG_OWNER manage an existing Organization subscription without bypassing active-athlete capacity, creating a second subscription, leaking provider identity, or requiring a commercial entitlement merely to repair billing?

**Split (unchanged):** authorization = who (`ORG_OWNER` via `canManageOrganization`, else **404**). Commercial validity = billing. Provider execution = Stripe adapter only.

### 40.1 Runtime inventory (verified in code)

| Fact | Evidence |
| --- | --- |
| Provider operations today | `OrganizationBillingProvider`: `createCustomer`, `createCheckoutSession`, `fetchCheckoutSubscription`, `verifyWebhook`, `fetchAuthoritativeSnapshot`. **No** Portal session, subscription update, cancel, or reactivate |
| Stripe controller | `OrganizationBillingController` is `@ConditionalOnProperty` `uap.billing.stripe.enabled=true`. Checkout, sync, and current GET only |
| Capacity GET | `OrganizationCapacityController` is **always** registered and Stripe-independent |
| `Subscription.planKey` / `billingCadence` | `final`. No domain mutator |
| JPA `plan_key` | `updatable = false` |
| JPA `billing_cadence` | column exists (V36); **not** `updatable = false` |
| `applyDomainState` | writes lifecycle, refs, period timestamps. **Does not** write plan or cadence |
| `ProviderSubscriptionSnapshot` | status, `cancelAtPeriodEnd`, trial end, period end, `providerStateAsOf`. **No** plan key or cadence |
| Checkout | `OrganizationCheckoutService.startCheckout` does **not** read `activeAthleteCount` |
| Open-subscription rule | non-EXPIRED row blocks a new checkout (`BILLING_CHECKOUT_IN_PROGRESS` if PENDING, else `BILLING_SUBSCRIPTION_EXISTS`) |
| Owner read | `currentStatus` picks the **newest** non-EXPIRED row. That is **not** fail-closed |
| Catalog | six server Prices: `ORG_BAND_25` / `75` / `250` × `MONTHLY` / `ANNUAL`. `StripeBillingProperties.priceId` / `requireMatchingPrice` |
| Schema | V35 `plan_key` VARCHAR + CHECK of the four catalog keys. No DB trigger forbidding UPDATE. Latest migration remains **V36** |

### 40.2 Aggregate model

**One Stripe subscription remains one Athlete Readiness `Subscription`.** `planKey` and `billingCadence` become mutable commercial attributes, changed only when an authoritative provider snapshot maps to an allow-listed Price.

Rejected: ending the row and inserting a replacement subscription for every plan change (duplicates the open-subscription invariant, splits webhook identity, and fights `providerSubscriptionRef` continuity).

**No Flyway V37** for this mutability. Slice E runtime, when authorized, drops domain `final` and JPA `updatable = false` on `plan_key`, and teaches `applyDomainState` / `synchronizeProviderSnapshot` to persist plan and cadence. Historical transitions stay in audit and the provider-event inbox, not in a second ACTIVE row.

`currentStatus` and every management action **must stop newest-wins**. Two or more non-EXPIRED Organization subscriptions → **409** `BILLING_SUBSCRIPTION_STATE_CONFLICT`. Do not union, pick highest, or update both.

### 40.3 Portal vs server

Stripe Customer Portal (`billing_portal.configuration`, stripe-java 33.4.2 / current Billing Portal API) can enable payment-method update and invoice history **independently** of `subscription_update` and `subscription_cancel`. Portal subscription update **cannot** enforce `COUNT(DISTINCT athlete_id)`. It stays **off**.

| Responsibility | Owner |
| --- | --- |
| Payment method | Portal |
| Invoice / receipt history | Portal |
| Customer email / tax id edits | **Off** (would diverge from Athlete Readiness identity) |
| Band or cadence change | Server `plan-changes` |
| Cancel at period end | Server `cancel` is the **recommended** owner. Whether Portal `subscription_cancel` may also be on is the open confirmation in §40.21 |
| Reactivate before period end | Server `reactivate` (`cancel_at_period_end=false`) |
| Portal `subscription_update` | **Off.** Portal cannot enforce active-athlete counts. This is not an open product choice |
| Portal `subscription_cancel` | **Open.** Recommended **off** so `mode=immediately` cannot violate §22. Portal can also be configured `at_period_end` only. §22 locks when cancel takes effect, not which UI starts it |

Future sandbox configuration is **not** executed in this lock. Required regardless of the open cancel choice: a **server-selected** Portal Configuration id (not “whatever the Dashboard default is”), `payment_method_update` on, `invoice_history` on, `subscription_update` off, `customer_update` off, no login page. The session Customer is the Organization’s persisted `providerCustomerRef` only. The client cannot send a Customer, Subscription, Configuration, or Price id. Fail closed if the selected configuration would allow a subscription Price change. Return URL is the server allow-listed coach billing URL. Do not persist or log the Portal URL.

`POST /api/v1/billing/organizations/{organizationId}/portal-sessions` lives on the Stripe-conditional controller. ORG_OWNER only. Response: hosted URL. Client cannot supply an arbitrary return URL. Unavailable when Stripe is disabled (controller absent). Creating a short-lived session is not request-id idempotent; a double click may open two sessions. Do not store them.

### 40.4 Management APIs (design only)

All on the existing Stripe-conditional organization billing controller. Body uses internal keys only: `targetPlanKey`, `targetCadence`, `requestId`. No Price id, amount, Product id, or Customer id.

Checkout uses `requestId` as the new `SubscriptionId`. Plan-change, cancel, and reactivate **must not**. Their `requestId` is an idempotency key for that mutation. The path `subscriptions/{id}` is the existing aggregate. A replay never inserts a second row.

| Action | Shape |
| --- | --- |
| Portal | `POST …/portal-sessions` |
| Plan or cadence | `POST …/subscriptions/{id}/plan-changes` |
| Cancel renewal | `POST …/subscriptions/{id}/cancel` |
| Reactivate | `POST …/subscriptions/{id}/reactivate` |

Authorization for every call: active `ORG_OWNER` or **404** `ORGANIZATION_NOT_FOUND` (existing non-oracle). ORG_ADMIN, TEAM_ADMIN, COACH, HEAD_COACH, ATHLETE, and foreign owners are 404. Unauthenticated **401**. Missing CSRF **403**. Never 402 for these conflicts. Never 403 to reveal billing authority.

### 40.5 Band eligibility

`activeAthleteCount` = Slice D `COUNT(DISTINCT athlete_id)`.

| Count | Allowed targets |
| --- | --- |
| 0–25 | `ORG_BAND_25`, `ORG_BAND_75`, `ORG_BAND_250` |
| 26–75 | `ORG_BAND_75`, `ORG_BAND_250` |
| 76–250 | `ORG_BAND_250` |
| >250 | none |

Same rule for initial Checkout, upgrade, downgrade, and any change whose **target band** is smaller than or equal to the current band. A cadence-only change (same band) does not change capacity and is allowed when a manageable subscription exists. Combined band+cadence is one Price swap: the target band must be eligible.

Denial: HTTP **409** `ORGANIZATION_PLAN_CAPACITY_CONFLICT`. Message does not include Stripe ids. Owner Web may compose “40 active athletes… reduce to 25 before selecting Starter” from the owner capacity snapshot. Invitees never see it.

### 40.6 Checkout race (resolved by existing locks)

Preflight at Checkout start is necessary and **not sufficient**.

Example: count 20, owner starts `ORG_BAND_25`, row is PENDING (not an effective band). Option B still lets valid athletes accept. Count becomes 26. Stripe then completes `ORG_BAND_25`.

| Option | Result |
| --- | --- |
| A. Apply the provider snapshot; keep members; owner sees `overCapacity` | Satisfies Option B, “do not reject a legitimate webhook”, “do not auto-remove”, “do not auto-upgrade” |
| B. Refuse the webhook or auto-void / auto-upgrade | Violates those locks |
| C. Freeze invitation accept while PENDING | Turns PENDING into a synthetic band and violates Option B |

**Locked recommendation:** A. Request-time 409 stops an already-oversized Checkout. Activation-time growth is an over-capacity Organization under §37, not a rejected payment and not a silent higher charge. Say this explicitly in Slice E tests. Do not claim preflight closes the race.

### 40.7 Upgrade, downgrade, cadence

**Upgrade** (`ORG_BAND_25` → `75` → `250`): **immediate** Price replacement. `proration_behavior=always_invoice`. `payment_behavior=error_if_incomplete` so a failed collection does **not** leave the Subscription on the higher Price. Higher capacity applies only after the authoritative snapshot is stored. Trial `trial_end` is sent unchanged. No second 14-day trial and no trial-clock reset.

**Downgrade:** **immediate**, and only when count ≤ target at the pre-call check **and** again before the smaller band is persisted. No Subscription Schedule and no Portal `schedule_at_period_end` in this contract. A period-end downgrade would need pending-plan state and would either constrain +1 below the still-purchased band (conflicts with the current effective band) or allow count to grow past the target before effect. The race after the provider call is closed by restoring the previous Price (§40.8), not by holding a database lock during Stripe I/O.

**Cadence:** monthly ↔ annual is the same immediate Price swap, including with a band change, for all six allow-listed Prices. Same proration and payment behavior. Not deferred. Coupons stay deferred.

**Trial (`TRIALING`):** band and cadence changes are allowed when the target band fits and the original `trialEndsAt` is preserved. The provider call must not set `trial_end` and must not set `trial_from_plan=true` (Stripe would otherwise apply the Price’s `trial_period_days` and reset the trial). Cancel schedules non-renewal at the trial/period end (`CANCEL_AT_PERIOD_END`). Reactivate clears that schedule. No extra trial.

Stripe’s own `payment_behavior=error_if_incomplete` can return provider HTTP 402 when collection fails and the Price is left unchanged. Athlete Readiness must map that to **409** `BILLING_PAYMENT_NOT_APPLIED`. It is not `COMMERCIAL_ENTITLEMENT_REQUIRED` and must not be forwarded as product-edge 402.

### 40.8 Concurrency with athlete accept

Accept locks Invitation `FOR UPDATE`, then Organization `FOR UPDATE`. Management locks **Organization only**, and only around local reads and writes. It must **not** hold that lock across the Stripe call: an accept that already holds the invitation lock would sit on the organization lock for the whole provider RTT, and taking the invitation lock after the organization lock would deadlock. Athlete Intelligence paths do not take this organization lock and must not be called inside it.

Downgrade (target maximum **below** the current band):

1. Authorize.
2. Short transaction: Organization `FOR UPDATE`, re-read distinct active athletes. If count > target, **409** `ORGANIZATION_PLAN_CAPACITY_CONFLICT` and do not call Stripe. Commit and release.
3. Stripe Price swap with the rules in §40.7. The lock is **not** held here.
4. Short transaction again: Organization `FOR UPDATE`, re-read count.
5. If count is still ≤ target, persist the authoritative snapshot and commit. The smaller band becomes effective only in this commit.
6. If count is now > target, **compensate**: Stripe update back to the previous allow-listed Price (not a new higher tier, not a member deletion). Persist that restored snapshot. Return **409** `ORGANIZATION_PLAN_CAPACITY_CONFLICT`.

A webhook for the rejected downgrade is still applied as provider truth, then compensation writes the restored Price. The undersized band must not remain the effective plan. Members stay. There is a short over-capacity window until compensation lands; that window is not permission to drop the webhook or to auto-remove athletes. If compensation fails, retry compensation. Do not leave the local `planKey` on the undersized band once the restored snapshot is known.

Upgrade and same-band cadence changes do not compensate when count rises: a larger or equal band still fits. They still persist only from the authoritative snapshot after the provider call returns.

### 40.9 Lifecycle policy

| State | Portal (payment/invoices) | Plan/cadence change | Cancel | Reactivate |
| --- | --- | --- | --- | --- |
| `PENDING` | Allowed if a Customer exists | **No** — `BILLING_CHECKOUT_IN_PROGRESS` / not manageable | **No** (abandon is not cancel-at-period-end) | **No** |
| `TRIALING` / `ACTIVE` | Yes | Yes if band eligible | Yes | No (not scheduled) |
| `CANCEL_AT_PERIOD_END` before `currentPeriodEndsAt` | Yes | **No** until reactivated (avoid changing a subscription that is ending) | Idempotent success | Yes |
| After period end / `EXPIRED` | Payment history only if Customer exists | **No** — new Checkout, not reactivate | **No** | **No** |
| `PAST_DUE` / `GRACE_PERIOD` | **Yes** (repair must not require entitlement) | **No** — `BILLING_LIFECYCLE_CONFLICT` | **No** — `BILLING_LIFECYCLE_CONFLICT` | **No** |
| Two or more non-EXPIRED | **No** provider mutation | **No** | **No** | **No** — `BILLING_SUBSCRIPTION_STATE_CONFLICT` |

Cancel from `ACTIVE` or `TRIALING` sets provider `cancel_at_period_end=true`, then stores `CANCEL_AT_PERIOD_END` only from the snapshot. It does not expire immediately and does not delete membership, consent, athlete data, readiness, or history.

Cancel from `PAST_DUE` or `GRACE_PERIOD` is **rejected**. ADR-040 allows `CANCEL_AT_PERIOD_END` only from `ACTIVE` or `TRIALING`, and that state is commercially entitled. Mapping an unpaid `past_due` cancel into `CANCEL_AT_PERIOD_END` would re-entitle the Organization. A provider snapshot that is `past_due` with `cancel_at_period_end` stays `PAST_DUE` (or `GRACE_PERIOD` when grace rules say so). It does not become `CANCEL_AT_PERIOD_END`. Payment repair stays on the Portal. Slice F still owns dunning.

Reactivate sets `cancel_at_period_end=false` only while paid-through is still in the future. After that, the owner starts a **new** Checkout. No silent new trial.

Slice F still owns dunning, the grace scheduler, and broad reconciliation. Slice E only needs webhook snapshots to carry plan, cadence, and cancel-at-period-end so a management change cannot diverge.

### 40.10 Provider snapshot and webhook minimum

Extend `ProviderSubscriptionSnapshot` with the provider Price id mapped through `StripeBillingProperties` to `CommercialPlanKey` + `BillingCadence`. The subscription **item Price** is the source of truth. Stripe metadata may still name the previous plan after a Price swap. Slice E must not keep today’s adapter rule that rejects a snapshot when metadata and Price disagree. Unknown Price: **fail closed** — do not invent a tier; **409** `BILLING_PROVIDER_PRICE_REJECTED`; leave `planKey` unchanged. Stale `providerStateAsOf` remains a no-op.

`synchronizeProviderSnapshot` writes the Price-mapped plan and cadence when the snapshot is accepted. When Athlete Readiness performs the update, it should also refresh metadata, but metadata is never the trust root. An undersized Price that arrives from outside the app (Portal or Dashboard) is still provider truth: apply it only together with the §40.8 compensation rule so it does not remain the effective band while count exceeds it. Do not drop the webhook.

Out of Slice E: grace jobs, dunning mail, retry queues beyond safe retry of the management call itself.

### 40.11 Errors, idempotency, failure window

| Condition | Code | HTTP |
| --- | --- | --- |
| Not owner / foreign org | existing not-found | 404 |
| No manageable subscription | `BILLING_SUBSCRIPTION_NOT_MANAGEABLE` | 409 |
| Second checkout / plan change while PENDING | `BILLING_CHECKOUT_IN_PROGRESS` | 409 |
| Ambiguous non-EXPIRED rows | `BILLING_SUBSCRIPTION_STATE_CONFLICT` | 409 |
| Target band too small | `ORGANIZATION_PLAN_CAPACITY_CONFLICT` | 409 |
| PAST_DUE / GRACE / cancel-scheduled plan change | `BILLING_LIFECYCLE_CONFLICT` | 409 |
| Same `requestId`, different target | `BILLING_REQUEST_CONFLICT` | 409 |
| Optimistic version conflict | `BILLING_CONCURRENT_MODIFICATION` | 409 |
| Stripe outage | `BILLING_PROVIDER_UNAVAILABLE` | 503 (existing) |
| Unknown Price | do not apply; operational failure, no client-chosen tier | 409 `BILLING_PROVIDER_PRICE_REJECTED` |
| Provider payment failed; Price unchanged (`error_if_incomplete`) | `BILLING_PAYMENT_NOT_APPLIED` | 409 (never product-edge 402) |

Not 400 for these. Not 402.

`requestId` on plan-change, cancel, and reactivate is idempotency only. It is **not** a new `SubscriptionId` (that pattern belongs to Checkout). Same id + same intent returns the current result and does not insert a row. Same id + a different target → **409** `BILLING_REQUEST_CONFLICT`. Already-canceled cancel and already-active reactivate are success, not a second provider mutation. Portal sessions are not keyed. A subscription id that belongs to another Organization is the existing **404** non-oracle, and it makes no provider call.

Order for every mutating action: authorize → short organization-row transaction for preconditions → release → provider mutation → refetch snapshot → short transaction to persist. A timed-out client retries with the same `requestId`. A webhook that arrives first is the snapshot the HTTP retry observes. Local state is never treated as more authoritative than Stripe. Downgrade compensation is §40.8.

### 40.12 Audit

On authoritative apply, not merely on click:

- `BILLING_PLAN_CHANGED` (from plan, to plan, cadence; no Price id)
- `BILLING_CANCEL_REQUESTED`
- `BILLING_SUBSCRIPTION_REACTIVATED`
- existing `BILLING_SUBSCRIPTION_ENDED` when the period actually ends (webhook)

Do not audit Portal URLs, card data, or raw provider payloads. Opening Portal is not a security-audit event.

### 40.13 Owner read model

Keep two reads until a later UX slice: Stripe-conditional subscription GET (plan, cadence, lifecycle, trial end, period end — no provider ids) and Stripe-independent capacity GET (count, band capacity, remaining, at/over capacity). Web may call both. Do not put usage on the Stripe-conditional controller in a way that disappears when Stripe is off. Non-owners still 404 with no usage body.

Ambiguous commercial state: capacity GET keeps Slice D behavior (count, null band). Management and the Stripe subscription GET return `BILLING_SUBSCRIPTION_STATE_CONFLICT` instead of newest-wins.

### 40.14 Rollout

**No new Slice E flag.** Provider-mutating routes stay on `UAP_BILLING_STRIPE_ENABLED`. Production remains false, so deploying a future Slice E build does not open Portal or plan changes. Capacity GET stays independent. Entitlement and capacity flags stay false. This lock does not authorize setting any of them true. Live Products/Prices and Stripe Tax remain later gates.

### 40.15 Web (future)

Owner billing page may show current plan, cadence, status, usage, “Manage payment method and invoices”, “Change plan”, “Cancel renewal”, and “Reactivate”. Marketing names (Starter / Team / Organization) are labels only. Tax display stays tax-exclusive. No coupons, no enterprise invoicing, no provider ids, no invitee paywall. The coach route is not authorization: render management only after the owner status/capacity calls succeed; a 404 is a generic unavailable state with no portal or checkout flash. Hide “Start Checkout” when a non-PENDING manageable subscription already exists. `ORGANIZATION_PLAN_CAPACITY_CONFLICT` maps as conflict, not 402 or unauthorized. The server remains the band check even if the client disables an undersized option.

### 40.16 Athlete boundary

No membership, consent, State Engine, readiness, recovery, recommendation, Team Readiness, or history changes. The organization row lock does not rewrite `team_memberships`. Cancel and downgrade never delete athletes.

### 40.17 Future sandbox certification (do not run now)

Using the existing Athlete Readiness **sandbox** Customer only: Portal opens for that Customer; non-owner makes **no** provider call; return URL is the allow-listed app URL; upgrade; eligible downgrade; ineligible downgrade; cadence swap; cancel at period end; reactivate; duplicate `requestId`; webhook shows the new Price and cancel flag; stale event ignored; unknown Price does not change `planKey`; no second Customer; no second Subscription; no live-mode object; Portal cannot change Price because `subscription_update` is off.

### 40.18 Mandatory QA matrix (implementation later)

- AuthZ on **each** of portal, plan-change, cancel, and reactivate: owner success; `ORG_ADMIN`, `TEAM_ADMIN`, `COACH`, `HEAD_COACH`, `ATHLETE`, and foreign owner **404** with **no** Stripe call; **401**; CSRF **403**.
- Stripe disabled: all four management routes absent. Capacity GET still registered.
- Portal session uses the persisted Customer ref and the server Portal Configuration. Client cannot set the return URL. Configuration keeps `subscription_update` off.
- Checkout: count within band; count 40 → `ORG_BAND_25` **409**; count 100 → `ORG_BAND_75` **409**; count >250 **409**; replay; PENDING growth then webhook still applies and `overCapacity` is true; no member delete.
- Upgrade 25→75 and 75→250 only after snapshot. `error_if_incomplete` leaves the old Price. Capacity stays on the old band until that snapshot commits. Webhook-before-commit and provider-success/local-fail still apply provider truth. No auto-remove and no auto-upgrade.
- Downgrade at count == target succeeds. One athlete over fails **before** any provider call. A concurrent accept after the provider call and before persist restores the previous Price and returns **409**, with no member deletion.
- Cadence-only monthly ↔ annual does not change capacity. Combined band+cadence uses the target band rule. All six eligible Prices.
- Cancel from `ACTIVE` and `TRIALING` only; not immediately `EXPIRED`. Duplicate cancel does not call the provider twice. `PAST_DUE` and `GRACE_PERIOD`: portal and no plan change; cancel **409** `BILLING_LIFECYCLE_CONFLICT` and the row does not become `CANCEL_AT_PERIOD_END`. `CANCEL_AT_PERIOD_END`: plan change blocked; cancel idempotent; reactivate allowed. `PENDING` and `EXPIRED`: plan change, cancel, and reactivate fail closed. After period end, reactivate is rejected.
- Reactivate before end; duplicate reactivate; after end rejected. No second trial.
- `requestId` replay vs different-target **409**. Portal is not request-id keyed. Foreign subscription id on this Organization → **404**, no provider call. Plan-change `requestId` does not create a Subscription row.
- Unknown Price does not change `planKey`. Stale `providerStateAsOf` is a no-op. Metadata lag after a real Price change does not fail the webhook.
- Two non-EXPIRED rows: management and the Stripe subscription GET return `BILLING_SUBSCRIPTION_STATE_CONFLICT`. Capacity GET still returns the count and a null band.
- Entitlement enforcement on does not turn these denials into **402**.

### 40.19 Reviews

Independent review of this contract (no runtime edits). Verdicts:

| Role | Verdict |
| --- | --- |
| Lead / Architect | **FAIL** on the first draft, **addressed:** no cancel from `PAST_DUE`/`GRACE` into entitled `CANCEL_AT_PERIOD_END`; Price not metadata is the plan source; plan-change `requestId` is not a new Subscription id. No new ADR |
| Backend | **FAIL** on holding Organization `FOR UPDATE` across Stripe I/O. **Addressed in §40.8.** Flyway V37 **not** required |
| External Integration | **PASS.** `trial_from_plan=true` stays unset. Portal cannot enforce athlete counts |
| QA / Test Automation | **PASS-WITH-NOTES.** Missing cells are now in §40.18 |
| Security / Code Quality | **PASS.** Notes folded in: server-selected Portal Configuration, persisted Customer ref only, unknown Price fail-closed |
| DevOps / CI-CD | **PASS.** No fourth billing flag. No V37. No Railway or Stripe mutation in this lock |
| Web | **PASS-WITH-NOTES.** Notes folded into §40.15 |
| Athlete Intelligence / Data | **PASS.** Organization-row lock does not rewrite intelligence or membership data |
| Documentation / Release | **PASS-WITH-NOTES.** Portal cancel on vs off remains an open Product Owner confirmation (§40.21). Checkout-race tolerance and immediate downgrade are derived from existing locks |

### 40.20 Product Owner resolutions (recommended; acceptance of this section is the lock)

These are not silent inventions. Each is the only option that keeps §16, §22, Option B, and “do not reject a legitimate webhook” together.

| ID | Topic | Resolution |
| --- | --- | --- |
| A | Portal features that are closed | Payment method + invoices on. `subscription_update` **off** (cannot enforce athlete counts). `customer_update` off |
| B | Upgrade timing | Immediate |
| C | Upgrade proration | `always_invoice`, and `error_if_incomplete` so a failed payment does not change the Price |
| D | Downgrade timing | Immediate. No period-end schedule in this contract |
| E | Downgrade race | Organization `FOR UPDATE` only around local read and persist. If count exceeds the target after the provider call, restore the previous allow-listed Price. Do not hold the lock across Stripe. Do not lock Invitation |
| F | Cadence | Immediate Price swap, alone or with a band change, all six Prices, same proration rules |
| G | Trial changes | Allowed when the target band fits. Original trial end preserved. No second trial |
| H | PAST_DUE / GRACE | Portal payment repair allowed. Plan change **and** cancel blocked. Do not enter entitled `CANCEL_AT_PERIOD_END` from an unpaid state |
| I | Aggregate | Mutable plan/cadence on the same Subscription. No replacement rows. No Flyway for that |
| J | Checkout race | Preflight 409 plus apply-webhook and keep over-capacity. Do not freeze Option B during PENDING |
| K | Where cancel starts | **Open.** Recommended: app-owned `cancel_at_period_end` and Portal `subscription_cancel` **off**. Alternative: Portal cancel **on** with `mode=at_period_end` only, plus the same server reconciliation. §22 does not choose the UI |
| L | New flag | **No.** Stripe-enabled controller is the gate |

Rows B–J and L follow from §16, §22, Option B, ADR-040 entitlement, and “do not reject a legitimate webhook.” They are not separate product forks.

### 40.21 Open Product Owner confirmation

**Portal cancel.** Either:

1. **Recommended:** Portal `subscription_cancel` **off**. ORG_OWNER cancels and reactivates only through the server APIs. Portal is payment method and invoices.
2. Portal `subscription_cancel` **on** with `mode=at_period_end` only (never `immediately`), and webhooks still reconcile `CANCEL_AT_PERIOD_END`. Reactivation remains the server API unless the same Portal screen can clear `cancel_at_period_end` without resetting the trial.

`subscription_update` stays **off** in both choices.

Slice E **runtime** is **not** authorized by this section. Production flags stay off.

V4 is **not** complete.

V4 Pre-Slice-E billing management: PRODUCT OWNER DECISIONS REQUIRED




