# ADR-048 — Connection and sync lifecycles

- **Status:** Proposed (awaiting Product Owner / Lead lock)
- **Date:** 2026-09-30
- **Product:** Athlete Readiness V5 — Connected Athlete

## Context

Provider links and data pulls are different concerns. Collapsing them into one mega-status hides reauth vs partial sync failures.

## Decision (proposed)

### Connection (athlete ↔ provider link)

Statuses: `DISCONNECTED` → `PENDING` → `CONNECTED`; from `CONNECTED` → `NEEDS_REAUTH` | `DISCONNECTED`; `PENDING` may end `DISCONNECTED` | `ERROR`.

### Sync run (per attempt)

Statuses: `REQUESTED` → `RUNNING` → `SUCCEEDED` | `PARTIAL` | `FAILED`.

Rules:

1. Connection may remain `CONNECTED` while a sync run `FAILED`.
2. Disconnect / revoke stops future sync immediately and invalidates stored credentials.
3. Re-grant prefers a new connection identity (mirrors ConsentGrant re-grant spirit in ADR-033) unless Product Owner locks upsert-in-place.
4. One open connection per `(athleteId, providerKey)` unless Product Owner unlocks multi-active same-provider links.

## Consequences

- UI can show honest “connected but last sync failed” and “needs reauth”.
- Jobs and webhooks create sync runs; they do not invent connection health from empty reads.

## References

- ADR-033, ADR-046
- `docs/V5_IMPLEMENTATION_PLAN.md`
