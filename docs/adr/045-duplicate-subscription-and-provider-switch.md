# ADR-045 — Duplicate-subscription and provider-switch policy

## Status

**Accepted** — V4 Product Owner decision lock (§22.1 #12).

## Context

Users may subscribe on Apple then open Web and be offered Stripe checkout, causing double payment. Silent provider migration is unsafe.

## Decision

- One individual provider owns billing at a time.  
- Before presenting individual checkout on any channel, server checks for an existing **ACTIVE** or **grace-valid** individual entitlement.  
- If entitled: show Premium state, disclose billing provider, route management to that provider — **no** normal duplicate checkout.  
- To switch: cancel/stop renewal at current provider → retain entitlement through paid-through date → begin replacement provider **after** the existing paid period.  
- No silent migration; no automatic double subscription.  
- Do not copy payment credentials across providers.

## Consequences

- Prevents accidental double charge  
- Requires consistent entitlement reads on Web and Mobile (Slice G)  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§8–9, 22
