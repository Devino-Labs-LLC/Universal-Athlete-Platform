# ADR-040 — Subscription lifecycle

## Status

**Proposed** — awaiting grace, cancellation, and trial Product Owner decisions.

## Context

Stripe/Apple/Google expose many native statuses. Product code should not mirror every provider enum.

## Decision (proposed)

Maintain a canonical internal lifecycle such as:

`PENDING` → `ACTIVE` → (`PAST_DUE` / `GRACE_PERIOD`) → `CANCEL_AT_PERIOD_END` → `EXPIRED` / `CANCELLED`

Adapters map provider states into this machine. Entitlement is derived from internal state + paid-through timestamps, not raw provider strings in feature code.

Exact transitions finalize after PO decisions on grace, cancel-at-period-end, and trials.

## Consequences

- Stable product checks across providers  
- Requires webhook/refetch discipline (ADR-044)  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§15–16
