# ADR-044 — Provider webhook / idempotency model

## Status

**Accepted** — V4 Product Owner decision lock (technical pattern + lifecycle from §22).

## Context

Provider webhooks are at-least-once and may arrive out of order. Trusting redirects or unverified payloads enables free entitlement.

## Decision

For each provider event:

1. Verify signatures (mandatory).  
2. Persist/dedupe by provider event id (idempotent).  
3. Prefer refetch of authoritative subscription/purchase objects when applying state.  
4. Guard against stale updates (provider timestamps/versions).  
5. Derive internal lifecycle (ADR-040) + entitlements transactionally with commercial audit where required.  
6. Do not store arbitrary raw payloads by default; keep minimal refs/evidence.

Fulfillment is webhook/sync driven — not success-URL driven. Grace recovery and cancel-at-period-end follow ADR-040.

The provider subscription snapshot remains the source of truth for plan, cadence, period end, cancel-at-period-end, and ended. A qualifying failure event supplies the grace anchor when that snapshot is payment attention and no earlier anchor exists. A newer successful snapshot clears grace. A stale failure event must not re-enter grace or regress `ACTIVE`. Reconciliation refetches the same provider subscription. It does not invent a grace window, override a newer `providerStateAsOf`, or store raw payloads. `FAILED` receipts stay retryable by event id; recovery of missed provider truth is a refetch, not a stored body. The Slice F event and worker contract is `docs/V4_IMPLEMENTATION_PLAN.md` **§44** and does not authorize runtime.

## Consequences

- Replay-safe commercial state  
- Operational need for monitoring failed processing  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§6, 18, 20, 22; ADR-040
