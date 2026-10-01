# ADR-048 — Connection and sync lifecycles

- **Status:** Accepted
- **Date:** 2026-09-30
- **Product:** Athlete Readiness V5 — Connected Athlete
- **Product Owner locks:** D7 = A (one active provider MVP); D9 = B (retain history, drop credentials)

## Context

Provider links and data pulls are different concerns. Collapsing them into one mega-status hides reauth vs partial sync failures.

## Decision

### Connection (athlete ↔ provider link)

Statuses: `DISCONNECTED` → `PENDING` → `CONNECTED`; from `CONNECTED` → `NEEDS_REAUTH` | `DISCONNECTED`; `PENDING` may end `DISCONNECTED` | `ERROR`.

### Sync run (per attempt)

Statuses: `REQUESTED` → `RUNNING` → `SUCCEEDED` | `PARTIAL` | `FAILED`.

Rules:

1. Connection may remain `CONNECTED` while a sync run `FAILED`.
2. Disconnect / revoke stops future sync immediately and invalidates stored credentials; normalized athlete-owned historical evidence may remain (D9 = B). Explicit privacy deletion is a separate flow.
3. Re-grant prefers a new connection identity (mirrors ConsentGrant re-grant spirit in ADR-033).
4. V5 MVP enforces **one active health provider/hub per athlete** (D7 = A). Persistence must not preclude later multi-provider support.

## Consequences

- UI can show honest “connected but last sync failed” and “needs reauth”.
- Switching providers requires disconnect of the prior active connection before connecting another.

## References

- ADR-033, ADR-046
- `docs/V5_IMPLEMENTATION_PLAN.md`
