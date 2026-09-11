# ADR-041 — Seat and usage semantics (active-athlete bands)

## Status

**Accepted** — V4 Product Owner decision lock (§22.1 #2, #3, #10).

## Context

Organization commercialization needs deterministic athlete counting without usage-metered per-athlete monthly billing.

## Decision

- Pricing shape: **fixed active-athlete bands** (architecture supports **up to 25 / 75 / 250**). Exact Organization **dollar** prices are locked commercially **before** Slice B sandbox Product/Price creation — not invented in this ADR.  
- Billable unit: one **ACTIVE** Athlete identity counts **once per Organization**, even across multiple Teams.  
- Not billable: pending invitation, LEFT, REMOVED.  
- Cross-Organization: independent counts.  
- Downgrade: **cannot become effective** while active billable count exceeds the target band; ORG_OWNER remediates; never automatically delete/remove athletes or memberships.

## Consequences

- Band enforcement needs transactional reads of ACTIVE athlete memberships  
- Billing disputes resolvable from domain data  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§14, 22
