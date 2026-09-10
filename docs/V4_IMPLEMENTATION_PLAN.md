# Athlete Readiness V4 — Implementation Plan

**Theme:** Commercialization / Billing / Entitlements  
**Product question:** Can Athlete Readiness become a commercially operable SaaS product where individuals and organizations can pay through an appropriate billing channel, receive provider-neutral entitlements, manage subscriptions safely, and retain access across Web/iOS/Android without billing logic contaminating the product domain?

**Document type:** PLANNING ONLY — Product Owner decision lock  
**Baseline:** `main` = `develop` = `5b714f787b690acda8240402d26fe955577b2fc8`  
**Production schema:** Flyway **V34**  
**Prior version:** Athlete Readiness V3 — **COMPLETE — PRODUCTION VERIFIED**

**This document does not authorize V4 Slice A or any runtime implementation.**

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
| Documentation / Release | This plan + proposed ADRs |
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
| Org manage capability today | Invitation/admin ops: ORG_ADMIN or ORG_OWNER; `canManageOrganization` currently **ORG_OWNER-only** for some paths — **billing authority is a separate PO decision** |
| Account / Athlete | One Account; ≤1 Athlete; coach-only accounts OK (ADR-030) |
| Audit | Durable append-only `security_audit_events` (V34 / ADR-035); adapters in org/consent/training |
| Web IA | Athlete `/app/*`; coach sibling `/coach/*`; no billing routes |
| Mobile IA | Athlete-first tabs; no coach console |
| Config pattern | Fail-fast required secrets; no silent prod/sandbox fallbacks |
| Highest accepted ADR | **ADR-035** |

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

| Item | Planning stance |
| --- | --- |
| Customer | School, club, team organization, other Organization account |
| Preferred channel | **Stripe / Web** |
| Not forced through | Apple App Store or Google Play billing |
| Later options (not locked) | Monthly/annual, athlete/team limits, org features, invoices, commercial contracts |

### 3.2 Individual athlete subscription

| Item | Planning stance |
| --- | --- |
| Customer | Individual Athlete Readiness Account |
| Potential providers | `STRIPE`, `APPLE_APP_STORE`, `GOOGLE_PLAY` |
| Cross-client rule | One active individual entitlement → same product capability on **Web + iOS + Android** after sign-in to the same Account |
| Separation | **Billing owner** (who charges) ≠ **entitlement** (what product allows) |

**Whether V4 ships individual monetization at all is Product Owner Decision #1** (§22). Do not silently assume.

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

## 7. Apple / Google individual billing (conditional)

Only if PO includes individual monetization in V4:

| Platform | Approach |
| --- | --- |
| iOS | Apple auto-renewable subscription; **server-side** validation/sync (App Store Server API / notifications) |
| Android | Google Play recurring subscription; **server-side** validation/sync (Play Developer API / RTDN) |

Mobile clients must not be the sole trust root for entitlement. Map provider notifications into the same internal lifecycle + entitlements.

---

## 8. Cross-platform entitlement UX (desired lock)

| Origin | Web | iOS | Android |
| --- | --- | --- | --- |
| Stripe | ✅ | ✅ | ✅ |
| Apple | ✅ | ✅ | ✅ |
| Google | ✅ | ✅ | ✅ |

Billing continues with the **originating** provider until cancel/expire/explicit switch. Do **not** copy payment credentials across providers.

---

## 9. Duplicate subscription prevention & provider switch

### 9.1 Prevention

If Account already has **ACTIVE** (or grace-valid) individual entitlement from any provider:

- Show paid/premium state
- Identify billing provider appropriately
- **Do not** present normal Stripe/Apple/Google duplicate checkout
- Direct management to the correct provider channel

### 9.2 Explicit switch (not accidental)

Recommended policy options for PO (Decision #12):

1. Cancel/stop renewals on current provider  
2. Retain access until paid-through  
3. Start new provider subscription only after (2) is scheduled or after overlap policy  
4. Never silent migrate  

Overlap avoidance: prefer **no double-charge window**; if brief overlap is unavoidable, document and audit.

---

## 10. Tax architecture (channel-specific)

| Channel | Planning stance |
| --- | --- |
| Stripe / Web | Evaluate Stripe Tax; Product Tax Codes; `automatic_tax` configuration; Devino Labs tax registrations; location collection; tax-inclusive vs exclusive **display** (PO) |
| Apple | App Store tax category; Apple owns storefront tax handling per its rules — **do not** run Apple renewals through Stripe Tax |
| Google | Play product/tax classification; provider jurisdiction rules — **do not** double-tax via Stripe |

**Hard rule:** `automatic_tax.enabled = true` is **not** proof Devino Labs is registered everywhere. Registration ≠ calculation.

Do **not** add `athlete.salesTax` or similar domain booleans.

Planning documents **who is merchant / billing processor / tax handler by channel** without unsupported legal claims. Legal/tax counsel remains a launch dependency for live tax collection.

---

## 11. Entitlement model

Provider-neutral capabilities (examples — **not locked**):

| Subject | Example capabilities |
| --- | --- |
| Individual | paid individual tier, advanced insights, richer history |
| Organization | team management suite, coach collaboration suite, Team Readiness product access, reporting, athlete limits, team limits |

**V1–V3 free vs paid split is Product Owner Decision #4.** Do not retroactively paywall shipped athlete/coach foundations without explicit approval.

Product checks:

```text
if (!entitlementPort.has(orgId, ORG_TEAM_READINESS_PRODUCT)) deny product surface;
// then existing V3 authZ + consent unchanged
```

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

**Preferred principle (proposed):** effective capability = **safe union** of currently valid entitlements, unless a capability has explicit exclusive semantics.

| Event | Effect |
| --- | --- |
| Athlete loses org membership | Lose **organization-provided** commercial access; **keep** personal subscription |
| Personal subscription cancels/expires | Lose personal capabilities; **keep** valid org-granted access |
| Both active | Union |

Document exceptions only with PO approval.

---

## 14. Seat / billable-athlete model (PO required)

Alternatives for evaluation:

1. Fixed organization tiers (soft/hard athlete caps)  
2. Per-athlete seat pricing  
3. Base subscription + seat add-ons  
4. Tiered athlete bands  
5. Custom enterprise pricing (likely deferred)

**Critical proposed default if seats/bands ship:**

> One ACTIVE Athlete Account/Athlete identity inside an Organization counts **once** for that Organization’s billing, even if on multiple Teams.

Still need PO definition of:

- ACTIVE membership definition  
- Pending invitations (usually **not** billable)  
- LEFT/REMOVED (not billable)  
- Cross-Organization (count per org independently)  
- Grace conditions  

Billing disputes must resolve from deterministic domain data.

---

## 15. Canonical internal subscription lifecycle (proposed)

Do not mirror every provider status into product code.

| Internal state | Meaning (planning) |
| --- | --- |
| `PENDING` | Checkout/purchase started; not yet entitled |
| `ACTIVE` | Entitled |
| `PAST_DUE` | Payment failed; may still be in grace |
| `GRACE_PERIOD` | Explicit commercial grace (if PO enables) |
| `CANCEL_AT_PERIOD_END` | Will end; still entitled until paid-through |
| `EXPIRED` | Period ended without renewal |
| `CANCELLED` | Ended by cancel path |

Provider-status mapping tables live in adapters. Exact machine finalized in ADR-040 after PO grace/cancel decisions.

---

## 16. Grace / dunning / cancellation / downgrade (options for PO)

| Topic | Options (not chosen) |
| --- | --- |
| Failed payment | Immediate restrict vs N-day grace; which features remain; admin messaging; recovery on `invoice.paid` |
| Cancellation | Immediate vs end-of-period; paid-through access; reactivate; resubscribe |
| Downgrade over limit | Block until usage fits; schedule + remediation; read-only grace — **never delete athletes/memberships as billing side effect** |
| Data retention | Billing end ≠ delete wellness/training history |

Stripe may retry payments; Athlete Readiness still needs deterministic entitlement behavior.

---

## 17. Trials / monthly-annual / coupons / enterprise

All **PO decisions** (§22). Architecture must support:

- No trial **or** fixed trial with abuse controls  
- Monthly only **or** monthly+annual (discount amount not invented here)  
- Coupons/promotions V4 vs deferred  
- Enterprise/custom invoicing V4 vs deferred  

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

## 22. Required Product Owner decision lock

Answer explicitly before Slice A:

1. **Scope:** Organizations only, or Organizations + Individuals?  
2. **Org pricing model:** fixed tiers / seats / base+seats / bands / other?  
3. **Billable athlete definition:** exact ACTIVE rules; multi-team same-org counting?  
4. **Free vs entitled:** which V1–V3 capabilities remain free?  
5. **Org billing authority:** ORG_OWNER only, or ORG_OWNER + ORG_ADMIN?  
6. **Cadence:** monthly only, or monthly + annual?  
7. **Trial:** none / fixed / org-only / individual — days? payment method up front?  
8. **Grace after failed payment:** duration + which features remain?  
9. **Cancellation:** immediate vs end-of-period?  
10. **Downgrade over limit:** block / schedule / read-only grace?  
11. **Individual tier/pricing** (if individuals in V4)?  
12. **Provider-switch policy** for individuals?  
13. **Advertised pricing:** tax-inclusive vs exclusive?  
14. **Stripe Tax launch posture** + registration prerequisites?  
15. **Coupons/promotions:** V4 or deferred?  
16. **Enterprise/custom invoicing:** V4 or deferred?  

Do **not** invent answers to unblock coding.

---

## 23. Proposed V4 slices

Hypothesis refined after repository inspection. Final lettering depends on Decision #1.

### If Organizations **and** Individuals (A–I)

| Slice | Theme |
| --- | --- |
| **A** | Commercial foundation — `billing` module, ports, entitlement model, ADRs Accepted after PO lock |
| **B** | Stripe Organization subscription — sandbox catalog, Checkout, customer mapping, lifecycle sync |
| **C** | Entitlements — server-side capability enforcement at product edges |
| **D** | Seats & usage — org athlete usage / tier enforcement |
| **E** | Billing management — Customer Portal, upgrade/downgrade/cancel/reactivate UX |
| **F** | Webhooks / dunning / reconciliation — robust events, grace/recovery |
| **G** | Individual monetization — Stripe Web + Apple + Google (only if PO approved) |
| **H** | Commercial UX completion — pricing/billing cohesion |
| **I** | Hardening / RC — threat T12–T22, entitlement matrix, tax/config, production certification |

### If Organizations **only** (A–H)

Omit individual Slice G; renumber H→G, I→H. Defer Apple/Google individual work to a later version.

**Do not implement any slice in this planning task.**

---

## 24. Proposed ADRs (036–045)

Status: **Proposed** — awaiting Product Owner decision lock. Not Accepted.

| ADR | Title |
| --- | --- |
| [036](adr/036-billing-bounded-context.md) | Billing bounded-context ownership |
| [037](adr/037-provider-neutral-entitlements.md) | Provider-neutral entitlement model |
| [038](adr/038-organization-vs-individual-subscriptions.md) | Organization vs individual subscription ownership |
| [039](adr/039-billing-provider-channel-strategy.md) | Billing-provider / channel strategy |
| [040](adr/040-subscription-lifecycle.md) | Subscription lifecycle |
| [041](adr/041-seat-and-usage-semantics.md) | Seat / usage semantics |
| [042](adr/042-authorization-vs-entitlement.md) | Authorization vs entitlement |
| [043](adr/043-tax-channel-boundaries.md) | Tax responsibility boundaries |
| [044](adr/044-provider-webhook-idempotency.md) | Provider webhook / idempotency model |
| [045](adr/045-duplicate-subscription-and-provider-switch.md) | Duplicate-subscription / provider-switch policy |

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

## 28. Delivery notes

| Item | Value |
| --- | --- |
| Branch | `develop` |
| Allowed | Planning docs + proposed ADRs |
| Commit intent | `docs: plan Athlete Readiness V4 commercialization` |
| After push | Verify may run; **no** main merge/tag/deploy |

---

## 29. Planning readiness

Independent planning confirmation:

| Role | Verdict |
| --- | --- |
| Lead / Architect | Plan coherent; PO lock required before ADR Accept / Slice A |
| Backend | `billing` module + ports + webhook model implementable after PO |
| Web / Mobile | IA homes identified; mobile IAP conditional |
| External Integration | Stripe SaaS path clear; Apple/Google conditional; no Connect |
| Security / QA | Threats T12–T22 + test themes defined |
| DevOps | Secrets/env fail-fast pattern exists; Sonar `develop` scope follow-up |
| Athlete Intelligence | Boundary PASS for planning |
| Documentation | This plan + proposed ADRs |

**V4 planning: READY FOR PRODUCT OWNER DECISION LOCK**

Do not begin Slice A until decisions in §22 are recorded and ADRs Accepted.
