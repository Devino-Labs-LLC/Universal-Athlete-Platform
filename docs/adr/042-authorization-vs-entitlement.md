# ADR-042 — Authorization vs entitlement

## Status

**Accepted** — V4 Product Owner decision lock (§22.1 #4–5).

## Context

V3 authorization (membership, role, consent, IDOR) answers who may act. V4 commercial state answers whether the subject paid for a capability. Conflating them causes free access for unpaid orgs or paid orgs bypassing consent.

## Decision

Keep both gates:

1. **Authorization** — existing V3 rules unchanged as the security boundary.  
2. **Entitlement** — `EntitlementPort` answers commercial capability.

Both may be required. Subscription status never replaces consent or membership. Paid Organization never implies coach authority. Billing never paywalls privacy/account-control rights listed in §22.1 #4. ORG_OWNER alone holds Organization billing authority; ORG_ADMIN operational authority does not imply financial authority.

**Evaluation order (pre-Slice C lock):** AuthN → V3 authorization (membership, role, IDOR, consent-for-existence) → `EntitlementPort`. Commercial denial for an **already-authorized** actor is **HTTP 402** `COMMERCIAL_ENTITLEMENT_REQUIRED`, never 404. Entitlement is never evaluated before authorization (paid status must not become an existence oracle). Durable matrix: `docs/V4_IMPLEMENTATION_PLAN.md` §34.

**Band/capacity (Slice D, §37):** 402 remains **missing commercial capability** only. Organization active-athlete band conflict is **HTTP 409** `ORGANIZATION_ATHLETE_CAPACITY_UNAVAILABLE`, never 402, and never evaluated before V3 invitation validity (ADR-032 non-oracle). No-plan / no-effective-band acceptance is **neither** 402 nor 409 (Option B).


**Rollout:** Product-edge 402s apply only when server-owned `UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED=true`. Default **false** preserves pre-Slice-C V3 product-edge behavior. The flag is not client-controlled and is not implied by `UAP_BILLING_STRIPE_ENABLED`.

## Consequences

- Clear test matrix: authZ × entitlement cells  
- Aligns with Athlete Intelligence boundary (math/authZ never Stripe-aware)  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §12, 22, 34, 37; ADR-032; ADR-033
