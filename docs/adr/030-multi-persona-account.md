# ADR-030 — Multi-persona Account

- **Status:** Accepted
- **Date:** 2026-09-05
- **Product:** Athlete Readiness V3 lock

## Context

Identity today is `Account` + cookie JWT `AccountPrincipal` with athlete-self ownership (`Athlete` 1:1 via unique `account_id`). V3 needs coaches and org admins without inventing a second login identity system.

## Decision

1. **One Account** may hold multiple personas simultaneously (e.g. Athlete + Coach/Admin).
2. **Athlete** remains at most one row per Account (existing uniqueness). Athlete persona is optional.
3. **Coach-only accounts are allowed.** Org/team coaching or admin memberships do **not** require an Athlete row.
4. Coach/admin capability is expressed as **membership roles**, not a parallel 1:1 Coach aggregate and not Spring `ROLE_COACH` as sufficient authorization.
5. Athlete-linked team membership carries `athleteId` **only where applicable** (athlete roster membership). Coach/admin memberships omit athlete linkage.
6. One Account may hold memberships across **multiple Organizations** and **multiple Teams**.
7. Clients use sibling IA (athlete vs coach/admin). Active org/team context is request-validated server-side; JWT alone is not the access graph.

## Consequences

- Product can invite coaches who never create an athlete profile.
- Authorization always resolves Account → membership (+ consent when required), not “role claim in token.”
- Separate coach-only login accounts are unnecessary for V3.

## References

- `docs/V3_IMPLEMENTATION_PLAN.md` §20
- ADR-031, ADR-032
