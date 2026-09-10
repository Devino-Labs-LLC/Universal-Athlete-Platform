# ADR-036 — Billing bounded-context ownership

## Status

**Proposed** — awaiting Athlete Readiness V4 Product Owner decision lock.

## Context

V3 shipped Organization, Consent, Training, and durable audit without commercial billing. V4 must add SaaS commercialization without turning `organization` into a billing god-module or contaminating State Engine / readiness.

## Decision (proposed)

Introduce a Modulith module **`billing`** that owns commercial customer mapping, subscription state, provider references, plan/tier identity, entitlement derivation, and billing lifecycle. It references Organization and Account IDs by value only (no shared JPA entities). Product domains depend on a published `billing :: entitlements` (or equivalent) port — never on Stripe/Apple/Google types.

## Consequences

- Clear seam for Checkout/webhooks/IAP adapters  
- Organization retains identity/membership authority graph only  
- Requires Flyway tables under billing ownership when Slice A is authorized  

## References

`docs/V4_IMPLEMENTATION_PLAN.md`
