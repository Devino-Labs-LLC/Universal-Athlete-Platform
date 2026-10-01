# ADR-047 — Connected evidence & provenance

- **Status:** Accepted
- **Date:** 2026-09-30
- **Product:** Athlete Readiness V5 — Connected Athlete
- **Product Owner locks:** D5 = B (store-only ingest); D6 = B (manual precedence); D13 = A (V5A only)

## Context

Manual recovery check-ins and State Engine snapshots today assume athlete-reported knowledge. Connected devices supply observations that must not silently become readiness truth or overwrite athlete agency.

## Decision

1. Persist **provider-neutral connected evidence** (normalized values + units + time + provenance), not provider SDK types.
2. Required provenance concepts: provider key, external record id (or content hash), source device/app when known, observed time, provider-updated time when known, ingested time, sync run id, quality/confidence when applicable.
3. Minimize raw payload retention; do not build a raw health-data warehouse by default.
4. Athlete-entered check-in facts remain athlete-owned; connected evidence is **parallel evidence** and must not silently overwrite manual check-in truth (D6 = B).
5. Missing connected data must not create a readiness penalty.
6. V5 ships **V5A only** (store / map / display). Ingest must not automatically generate readiness. Formula changes require a separate V5B Product Owner lock.

## Consequences

- V5A stores evidence without changing `READINESS_V1`.
- V5B (formula / State Engine input expansion) remains deferred.

## References

- ADR-029
- `docs/V5_IMPLEMENTATION_PLAN.md`
