# ADR-034 — TrainingPlan ownership and coach collaboration

- **Status:** Accepted
- **Date:** 2026-09-05
- **Product:** Athlete Readiness V3 lock

## Context

Training plans are athlete-owned aggregates today. V3 needs coach assignments without a competing coach-owned plan tree or State Engine mutation.

## Decision

1. **`TrainingPlan` remains athlete-owned** in V3. Do **not** introduce a parallel coach-owned training-plan aggregate tree.
2. Coaches are **authorized collaborators / assigners** when membership + `TRAINING_COLLABORATION` (or equivalent) consent allow.
3. Coaches may create **explicit** training / team-session **assignments** (audited). Assignments never overwrite or mutate State Engine or readiness calculation (ADR-029).
4. Assignment and Athlete Readiness **recommendation may disagree**; both remain visible as distinct facts.
5. Athlete may **decline** or mark **unable to perform** an assignment. Conflict/warning and resulting action are **auditable**.
6. Optimistic concurrency on existing training aggregates remains in force.

## Consequences

- Slice E builds on training ports + consent, not a new plan ownership model.
- Clients must present assignment vs recommendation without collapsing them into one status.
- Export/collaboration scopes stay least-privilege.

## References

- ADR-029, ADR-033
- `docs/TRAINING_API_V1.md`
- `docs/V3_IMPLEMENTATION_PLAN.md` §8, §20
