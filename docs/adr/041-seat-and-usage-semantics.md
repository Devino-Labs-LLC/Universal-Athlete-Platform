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
- Billable unit: one **ACTIVE** Athlete identity (`team_memberships.athlete_id`) counts **once per Organization** (`COUNT(DISTINCT athlete_id)`), even across multiple Teams. Org-scoped memberships are not the athlete roster.  
- Not billable: pending invitation, LEFT, REMOVED, coach/admin TeamMembership (`athlete_id` null).  
- **Archive is not a usage delete:** Team/Organization `ARCHIVED` does **not** rewrite memberships. ACTIVE athlete memberships on an archived Team **still count** until LEFT or REMOVED (`docs/V4_IMPLEMENTATION_PLAN.md` §37.3).  
- Cross-Organization: independent counts.  
- **+1 only at ATHLETE team invitation accept** when that athlete is not already counted in the Organization. Invitation **create** does not reserve capacity.  
- Capacity denial for a **new** distinct athlete: HTTP **409** `ORGANIZATION_ATHLETE_CAPACITY_UNAVAILABLE` — **never 402**. Invitation stays PENDING. Existing counted athletes may still join another Team (zero-delta) even at band max or when billing is inactive.  
- Downgrade: **cannot become effective** while active billable count exceeds the target band; ORG_OWNER remediates; never automatically delete/remove athletes or memberships.  
- Count is **derived** from membership domain data (no mutable billing seat counter).  
- Enforcement is additionally gated by server-owned `UAP_BILLING_ORGANIZATION_CAPACITY_ENFORCEMENT_ENABLED` (default **false**; independent of Stripe and Slice C entitlement flags).  
- This ADR does **not** authorize Stripe Product/Price creation, Slice D runtime, or production capacity activation.

## Consequences

- Band enforcement needs transactional reads of ACTIVE athlete TeamMemberships plus Organization-scoped serialization (T18)  
- Billing disputes resolvable from domain data  
- Slice B sandbox catalog must match §31 prices and keys  
- Invitation accept remains a Slice C **free** surface (no 402); band overflow is a **409** roster-capacity conflict  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§14, 22, 31, 37
