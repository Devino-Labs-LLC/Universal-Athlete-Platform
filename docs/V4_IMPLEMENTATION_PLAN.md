# Athlete Readiness V4 — Implementation Plan

**Theme:** Commercialization / Billing / Entitlements  
**Product question:** Can Athlete Readiness become a commercially operable SaaS product where individuals and organizations can pay through an appropriate billing channel, receive provider-neutral entitlements, manage subscriptions safely, and retain access across Web/iOS/Android without billing logic contaminating the product domain?

**Document type:** Product Owner decision lock (docs)  
**Planning commit:** `117b37ef95c142daa323da021cc8172565b56803`  
**Production baseline (`main`):** `212f3f44bfe4c8709b636a7839d83c0a978edaa3`  
**`develop`:** not equal to `main` (docs-only ahead). Matrix lock `c64ba79…`; clarification `cedf1049a1fdf2c023114cc3de609963380f2da1`.  
**Production schema:** Flyway **V36** (inferred — see §33)  
**Prior version:** Athlete Readiness V3 — **COMPLETE — PRODUCTION VERIFIED**  
**§22 lock status:** **COMPLETE** (ADR-036–045 Accepted)  
**Slice A status:** **PRODUCTION VERIFIED** — commercial foundation only (see §30).
**Pre-Slice-B Organization catalog lock:** **COMPLETE** (see §31).
**Slice B status:** **PRODUCTION VERIFIED** (see §32 sandbox cert + §33 production).  
**Pre-Slice-C entitlement matrix:** **PRODUCT OWNER-APPROVED** (see §34). Slice C **runtime implementation** is authorized only after this rollout lock is committed. **Production activation** of enforcement remains a later explicit commercial-launch gate. V4 is **not** complete.

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

For each family, Slice C tests must include at least:

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
