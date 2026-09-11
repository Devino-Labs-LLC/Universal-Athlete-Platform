# ADR-038 — Organization vs individual subscription ownership

## Status

**Accepted** — V4 Product Owner decision lock (§22.1 #1, #11, #13).

## Context

V4 serves Organizations (schools/clubs) and individual athletes. Mixing billing ownership would create duplicate charges and confused authority.

## Decision

- **Organization subscriptions** are owned by the Organization; channel **Stripe/Web** only.  
- **Individual subscriptions** are owned by the Account; channels **Stripe Web**, **Apple App Store**, and/or **Google Play** (implemented in Slice G; A–F must not depend on Apple/Google).  
- Effective capabilities use a **safe union** of currently valid entitlements unless a capability has exclusive semantics.  
- Losing org membership removes org-provided commercial access only; cancelling personal Premium does not remove valid org-granted access.

## Consequences

- Separate commercial subjects and subscription rows  
- Individual adapters deferred to Slice G without blocking org slices  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§3, 13, 22–23
