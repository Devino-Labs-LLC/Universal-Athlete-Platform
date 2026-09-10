# ADR-039 — Billing-provider / channel strategy

## Status

**Proposed** — awaiting Product Owner scope and channel decisions.

## Context

Ordinary SaaS billing does not require Stripe Connect. Athlete Readiness is not a marketplace paying third-party sellers.

## Decision (proposed)

- Organization billing: **Stripe Billing + Checkout Sessions + Customer Portal** on Web.  
- Individual billing (if approved): Stripe and/or Apple App Store and/or Google Play with server-side validation.  
- **No Stripe Connect** unless a genuine multi-party payout requirement appears later.  
- Sandbox/test first; live catalog only after V4 gates. Separate secrets/IDs per environment; fail fast on mismatch.

## Consequences

- Hosted Checkout/Portal reduce PCI surface  
- Mobile stores remain channel-specific tax/commerce processors  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§6–7, 21
