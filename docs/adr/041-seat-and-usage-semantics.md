# ADR-041 — Seat and usage semantics

## Status

**Proposed** — awaiting Product Owner pricing and billable-athlete definition.

## Context

Organization commercial plans may include athlete limits or seat pricing. Counting must be deterministic and fair across multi-team memberships.

## Decision (proposed)

If seats/bands/limits ship:

- Define **billable athlete** from domain membership data (typically ACTIVE athlete memberships).  
- **Default proposal:** one Athlete counts **once per Organization**, even if on multiple Teams in that Organization.  
- Pending invitations are not billable unless PO explicitly says otherwise.  
- LEFT/REMOVED are not billable.  
- Cross-Organization memberships count independently per Organization.  
- Downgrades that exceed limits must not delete athletes or memberships; use block/schedule/read-only policy (PO).

If fixed tiers without seats are chosen, this ADR records “no seat metering” explicitly.

## Consequences

- Seat races need transactional guards  
- Billing disputes resolvable from domain queries  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §14
