# ADR-035 — Audit model

- **Status:** Accepted
- **Date:** 2026-09-05
- **Product:** Athlete Readiness V3 lock

## Context

The repository has training revision/history tables but no security audit stream. V3 requires auditability for sensitive org/membership/consent/assignment actions and athlete transparency without exposing raw internal security logs.

## Decision

1. Introduce **append-only** server-written security audit records (via `AuditPort`). Clients cannot write audit fields.
2. Do **not** conflate: transactional domain events, security audit events, and analytics events.
3. Mandatory audit categories include at least: organization/team create/archive, invitations, membership accept/leave/remove, role changes, consent grant/revoke, workout/assignment actions, exports when shipped.
4. **Athlete-facing transparency** exposes a **subset** of relevant events (e.g. consent grant/revoke; membership join/leave/remove; coach collaboration/assignment where appropriate). It is **not** the raw internal security audit stream.
5. Audit payloads must not contain raw wellness bodies, invite tokens, or credentials.
6. Internal retention of audit after membership ends is allowed; product coach/org views of sensitive wellness remain governed by ADR-033.

## Consequences

- Slice H hardens completeness; earlier slices emit audits when those actions ship.
- Web/mobile athlete transparency UI consumes a dedicated projection API, not admin audit dumps.
- Training content revisions remain separate from security audit.

## References

- `docs/V3_IMPLEMENTATION_PLAN.md` §13, §20
- ADR-032, ADR-033, ADR-034
