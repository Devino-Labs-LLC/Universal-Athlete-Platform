# ADR-042 — Authorization vs entitlement

## Status

**Proposed** — architectural lock candidate; capability catalog still PO-dependent.

## Context

V3 authorization (membership, role, consent, IDOR) answers who may act. V4 commercial state answers whether the subject paid for a capability. Conflating them causes either free access for unpaid orgs or paid orgs bypassing consent.

## Decision (proposed)

Keep both gates:

1. **Authorization** — existing V3 rules unchanged as the security boundary.  
2. **Entitlement** — `EntitlementPort` answers commercial capability.

Product use cases may require both. Subscription status never replaces consent or membership. Paid Organization never implies coach authority.

## Consequences

- Clear test matrix: authZ × entitlement cells  
- Aligns with Athlete Intelligence boundary (math/authZ never Stripe-aware)  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §12; ADR-032; ADR-033
