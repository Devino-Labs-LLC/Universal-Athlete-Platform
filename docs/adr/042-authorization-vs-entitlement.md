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

## Consequences

- Clear test matrix: authZ × entitlement cells  
- Aligns with Athlete Intelligence boundary (math/authZ never Stripe-aware)  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §12, 22, 34; ADR-032; ADR-033
