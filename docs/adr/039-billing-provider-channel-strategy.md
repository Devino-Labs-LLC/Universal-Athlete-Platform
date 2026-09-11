# ADR-039 — Billing-provider / channel strategy

## Status

**Accepted** — V4 Product Owner decision lock (§22.1 #1, #5–7, #14–16).

## Context

Ordinary SaaS billing does not require Stripe Connect. Athlete Readiness is not a marketplace paying third-party sellers.

## Decision

- Organization billing: **Stripe Billing + Checkout Sessions + Customer Portal** on Web; **ORG_OWNER only** financial authority.  
- Organization trial: **14 days** with payment method required up front.  
- Individual billing (Slice G): Stripe Web and/or Apple and/or Google with server-side validation; **no** individual trial in initial V4.  
- Cadence: monthly + annual as Price variants of the same entitlement.  
- Coupons/promotions and enterprise/custom invoicing: **deferred**.  
- **No Stripe Connect** unless a genuine multi-party payout requirement appears later.  
- Sandbox/test first; live catalog only after V4 gates and org dollar-price catalog lock. Separate secrets/IDs per environment; fail fast on mismatch.

## Consequences

- Hosted Checkout/Portal reduce PCI surface  
- Mobile stores remain channel-specific tax/commerce processors  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§6–7, 17, 21–22
