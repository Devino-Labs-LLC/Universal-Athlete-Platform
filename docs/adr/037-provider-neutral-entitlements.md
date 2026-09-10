# ADR-037 — Provider-neutral entitlement model

## Status

**Proposed** — awaiting V4 Product Owner decision lock (especially free vs paid capability split).

## Context

Subscriptions may originate from Stripe, Apple App Store, or Google Play. Product features must not branch on provider identity.

## Decision (proposed)

**Billing provider can vary. Entitlement cannot.** Canonical product checks ask whether a subject (Account or Organization) currently possesses capability **X**. Adapters synchronize provider state into entitlements. No scattered `isStripeCustomer` / `hasAppleSubscription` checks in feature code.

## Consequences

- Cross-client individual access after one legitimate purchase  
- Requires capability catalog defined with Product Owner  
- Entitlement changes are auditable commercial events  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§2, 8, 11
