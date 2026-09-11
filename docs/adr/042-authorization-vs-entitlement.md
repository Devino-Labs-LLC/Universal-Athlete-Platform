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

## Consequences

- Clear test matrix: authZ × entitlement cells  
- Aligns with Athlete Intelligence boundary (math/authZ never Stripe-aware)  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §12, 22; ADR-032; ADR-033
