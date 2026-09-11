# ADR-037 — Provider-neutral entitlement model

## Status

**Accepted** — V4 Product Owner decision lock (§22.1 #1, #4, #11).

## Context

Subscriptions may originate from Stripe, Apple App Store, or Google Play. Product features must not branch on provider identity.

## Decision

**Billing provider can vary. Entitlement cannot.** Canonical product checks ask whether a subject (Account or Organization) currently possesses capability **X**.

Locked free surfaces (when otherwise authorized) are never paywalled: authentication/account access; invitation accept/decline; leave Team; consent grant/revoke/re-grant; athlete transparency/activity; athlete-owned retained data/history per existing contracts.

Organization commercial entitlements gate paid org/coach product surfaces (candidates include active Team management, coach collaboration, consent-aware coach views, Team Readiness). Individual commercial tier is internal **PREMIUM** (richer/advanced capability without arbitrarily removing basic athlete functionality).

**Final capability enum/matrix** is defined in Slice A/C before enforcement — this ADR locks the model, not every capability string.

## Consequences

- Cross-client Premium after one legitimate individual purchase  
- AuthZ and entitlement remain independent (ADR-042)  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§2, 11, 22
