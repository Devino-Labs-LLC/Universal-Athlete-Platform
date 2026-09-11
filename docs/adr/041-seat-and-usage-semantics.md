# ADR-041 — Seat and usage semantics (active-athlete bands)

## Status

**Accepted** — V4 Product Owner decision lock (§22.1 #2, #3, #10). Organization **dollar** prices Product Owner-locked in `docs/V4_IMPLEMENTATION_PLAN.md` §31.

## Context

Organization commercialization needs deterministic athlete counting without usage-metered per-athlete monthly billing.

## Decision

- Pricing shape: **fixed active-athlete bands** (**up to 25 / 75 / 250**).  
- Organization **dollar** prices (tax-exclusive Web/Stripe) are Product Owner-locked in **§31**:
  - Starter (`ORG_BAND_25`): **$49/month**, **$490/year**
  - Team (`ORG_BAND_75`): **$99/month**, **$990/year**
  - Organization (`ORG_BAND_250`): **$149/month**, **$1,490/year**
  - Annual = **10 × monthly** (~16.7% / two months free). Marketing names must not replace catalog keys.
- Billable unit: one **ACTIVE** Athlete identity counts **once per Organization**, even across multiple Teams.  
- Not billable: pending invitation, LEFT, REMOVED.  
- Cross-Organization: independent counts.  
- Downgrade: **cannot become effective** while active billable count exceeds the target band; ORG_OWNER remediates; never automatically delete/remove athletes or memberships.
- This ADR does **not** authorize Stripe Product/Price creation (Slice B).

## Consequences

- Band enforcement needs transactional reads of ACTIVE athlete memberships  
- Billing disputes resolvable from domain data  
- Slice B sandbox catalog must match §31 prices and keys  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§14, 22, 31
