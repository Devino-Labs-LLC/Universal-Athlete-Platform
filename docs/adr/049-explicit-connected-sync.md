# ADR-049 — Connected sync is explicit (no hidden sync on GET)

- **Status:** Proposed (awaiting Product Owner / Lead lock)
- **Date:** 2026-09-30
- **Product:** Athlete Readiness V5 — Connected Athlete

## Context

V1 no-hidden-write and ADR-029 forbid GET/bootstrap/dashboard from secretly creating athlete state. Connected sync must obey the same rule: UI reads must not silently hit providers or enqueue heavy sync.

## Decision (proposed)

1. GET connection status, evidence summaries, bootstrap, dashboard, readiness, and coach facades **must not** create connections, sync runs, evidence rows, provider events, or tokens.
2. Sync starts only from: explicit athlete `POST …/sync` (or equivalent), verified provider webhook, or an authorized scheduled job that Product Owner unlocks after MVP.
3. Ingest stores evidence; State Engine / readiness generation remains a **separate explicit** mutation (or later authorized job) — ingest must not silently rewrite derived readiness.
4. Fail honestly on outages, rate limits, and reauth; never fake provider success.

## Consequences

- Clients must show sync CTAs / background-job status rather than assuming refresh-on-open syncs.
- QA must assert read paths create zero integration rows.

## References

- `AGENTS.md` V1 no-hidden-write
- ADR-029
- `docs/V5_IMPLEMENTATION_PLAN.md`
