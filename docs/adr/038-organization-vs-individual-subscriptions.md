# ADR-038 — Organization vs individual subscription ownership

## Status

**Proposed** — awaiting Decision #1 (org-only vs org+individual) and related pricing decisions.

## Context

V4 serves two customer types: Organizations (schools/clubs) and, optionally, individual athletes. Mixing their billing channels or ownership would create duplicate charges and confused authority.

## Decision (proposed)

- **Organization subscriptions** are owned by the Organization commercial subject; preferred channel Stripe/Web.  
- **Individual subscriptions** (if in V4) are owned by the Account; channels may be Stripe, Apple, or Google.  
- Effective capabilities use a **safe union** of valid entitlements unless a capability has exclusive semantics.  
- Losing org membership removes org-provided commercial access only; cancelling personal subscription does not remove valid org-granted access.

## Consequences

- Separate customer/subscription rows per subject type  
- Individual slice may be deferred entirely if PO chooses org-only  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§3, 13
