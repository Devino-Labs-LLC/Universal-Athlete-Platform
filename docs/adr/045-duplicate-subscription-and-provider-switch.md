# ADR-045 — Duplicate-subscription and provider-switch policy

## Status

**Proposed** — awaiting individual monetization and provider-switch Product Owner decisions.

## Context

Users may subscribe on Apple then open Web and be offered Stripe checkout, causing double payment. Silent provider migration is unsafe.

## Decision (proposed)

- Before presenting individual checkout on any channel, server checks for an existing valid individual entitlement.  
- If entitled: show paid state, disclose billing provider, route manage-subscription to that provider — **no** normal duplicate checkout.  
- Provider switch is an **explicit** lifecycle (cancel/stop renewals → retain until paid-through → start new provider), never silent.  
- Do not copy payment credentials across providers.

Exact overlap/grace during switch is a Product Owner decision.

## Consequences

- Prevents accidental double charge  
- Requires consistent entitlement reads on Web and Mobile  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§8–9
