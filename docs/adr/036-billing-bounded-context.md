# ADR-036 — Billing bounded-context ownership

## Status

**Accepted** — V4 Product Owner decision lock (`docs/V4_IMPLEMENTATION_PLAN.md` §22).

## Context

V3 shipped Organization, Consent, Training, and durable audit without commercial billing. V4 adds SaaS commercialization without turning `organization` into a billing god-module or contaminating State Engine / readiness.

## Decision

Introduce a Modulith module **`billing`** that owns commercial customer mapping, subscription state, provider references, plan/tier/band identity, entitlement derivation, and billing lifecycle. It references Organization and Account IDs by value only (no shared JPA entities). Product domains depend on a published `billing :: entitlements` (or equivalent) port — never on Stripe/Apple/Google types.

## Consequences

- Clear seam for Checkout/webhooks/IAP adapters  
- Organization retains identity/membership authority graph only  
- Flyway tables under billing ownership when Slice A is authorized  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§4, 22
