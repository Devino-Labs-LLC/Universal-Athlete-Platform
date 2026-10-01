# ADR-049 — Connected sync is explicit (no hidden sync on GET)

- **Status:** Accepted
- **Date:** 2026-09-30
- **Product:** Athlete Readiness V5 — Connected Athlete
- **Product Owner locks:** D5 = B; D11 = B (user-explicit sync MVP)

## Context

V1 no-hidden-write and ADR-029 forbid GET/bootstrap/dashboard from secretly creating athlete state. Connected sync must obey the same rule: UI reads must not silently hit providers or enqueue heavy sync.

## Decision

1. GET connection status, evidence summaries, bootstrap, dashboard, readiness, and coach facades **must not** create connections, sync runs, evidence rows, provider events, or tokens.
2. V5 MVP sync starts only from explicit athlete product actions (e.g. `POST …/sync` / mobile upload completing an athlete-initiated sync). **No server scheduled polling jobs** in MVP (D11 = B). Verified provider webhooks remain out of V5 OS-hub scope; future OAuth connectors may add them under a later lock.
3. Ingest stores evidence; State Engine / readiness generation remains a **separate explicit** mutation — ingest must not silently rewrite derived readiness (D5 = B).
4. Fail honestly on outages, rate limits, and reauth; never fake provider success.
5. Mobile durable upload queues that finish an athlete-initiated sync are allowed when idempotent.

## Consequences

- Clients must show sync CTAs / queue status rather than assuming refresh-on-open syncs.
- QA must assert read paths create zero integration rows.
- Future scheduled sync requires a new Product Owner lock.

## References

- `AGENTS.md` V1 no-hidden-write
- ADR-029
- `docs/V5_IMPLEMENTATION_PLAN.md`
