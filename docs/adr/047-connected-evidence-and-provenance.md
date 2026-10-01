# ADR-047 — Connected evidence & provenance

- **Status:** Proposed (awaiting Product Owner / Lead lock)
- **Date:** 2026-09-30
- **Product:** Athlete Readiness V5 — Connected Athlete

## Context

Manual recovery check-ins and State Engine snapshots today assume athlete-reported knowledge. Connected devices supply observations that must not silently become readiness truth or overwrite athlete agency.

## Decision (proposed)

1. Persist **provider-neutral connected evidence** (normalized values + units + time + provenance), not provider SDK types.
2. Required provenance concepts: provider key, external record id (or content hash), source device/app when known, observed time, provider-updated time when known, ingested time, sync run id, quality/confidence when applicable.
3. Minimize raw payload retention; do not build a raw health-data warehouse by default.
4. Athlete-entered check-in facts remain athlete-owned; connected evidence is parallel unless Product Owner locks a different precedence rule.
5. Missing connected data must not create a readiness penalty.

## Consequences

- V5A can store evidence without changing `READINESS_V1`.
- V5B (formula / State Engine input expansion) requires an explicit Product Owner lock and versioned calculator identity.

## References

- ADR-029
- `docs/V5_IMPLEMENTATION_PLAN.md`
