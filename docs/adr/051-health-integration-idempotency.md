# ADR-051 — Health integration webhook/job idempotency

- **Status:** Accepted
- **Date:** 2026-09-30
- **Product:** Athlete Readiness V5 — Connected Athlete
- **Product Owner locks:** D4 = B (30-day backfill default); D11 = B (explicit sync)

## Context

ADR-044 defines provider webhook idempotency for **billing**. Connected Athlete needs the same *pattern* for health/fitness providers and mobile upload batches without widening `billing_provider_events`.

## Decision

1. Create an **integrations-scoped** provider event / ingest inbox — do **not** reuse `billing_provider_events` rows for wearables.
2. Reuse ADR-044 pattern: verify authenticity → durable provider event / upload batch id uniqueness → prefer authoritative refetch when available → stale/out-of-order guards → transactional apply → minimal payload retention.
3. Evidence uniqueness at minimum: `(providerKey, athleteId, externalRecordId)` or documented content-hash fallback when providers lack stable ids.
4. Mobile HealthKit / Health Connect uploads are untrusted client observations (`CLIENT_DEVICE` provenance class) bound to authenticated athlete ownership; they still must be idempotent and schema-validated.
5. Replay of the same source event must not duplicate sleep/workout/measurement rows or trigger hidden State Engine writes.
6. Default initial backfill window is **30 days**, centrally configurable — not scattered magic constants (D4 = B). Respect OS history/permission limits.

## Consequences

- Billing and health inboxes remain separate bounded contexts.
- Concurrency and replay tests are mandatory before any connector is PRODUCTION DEPLOYED.

## References

- ADR-044
- ADR-046, ADR-047, ADR-049
- `docs/V5_IMPLEMENTATION_PLAN.md`
