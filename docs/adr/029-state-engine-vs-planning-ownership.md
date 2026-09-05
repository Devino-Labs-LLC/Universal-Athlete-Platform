# ADR-029 — State Engine vs Planning ownership

- **Status:** Accepted
- **Date:** 2026-09-05
- **Product:** Athlete Readiness V3 lock (Coaches, Teams & Schools)

## Context

External references already used **ADR-029** for the Knowledge → State → Planner → Recommendation → Execution → Evaluation → Learning model. The repository had **no** `docs/adr/` tree and no on-disk ADR-029. Implementation reality:

- State Engine = `DailyAthleteState*` (and downstream readiness/recommendation generation) under Modulith module `training`.
- “Planning Orchestrator” appears in V2 docs as a **logical label** only — there is no separate Modulith module or class by that name.
- Planning today = athlete-owned `TrainingPlan` / schedule / occurrence / adaptation use cases in `training`.
- V3 introduces coaches who must write assignments **without** becoming a second source of truth for readiness/state.

## Decision

1. **State Engine** owns derived athlete state (`DailyAthleteState*`) and the explicit generation chain into readiness and recommendations. Module owner: `training` (Athlete Intelligence discipline).
2. **Planning** owns plan/day/prescription/schedule/occurrence/adaptation negotiation and mutation. Same Modulith module `training` in V3; “Planning Orchestrator” remains a **logical** name, not a new package.
3. Coach/org modules **must not** compute or overwrite State Engine outputs, readiness scores/bands, or recommendation calculator results.
4. Coach-authored inputs enter as **knowledge, constraints, plan/assignment writes, or execution evidence** — classified per the V3 plan — never as silent State Engine writes.
5. GET facades remain **no-hidden-write**. Team readiness aggregates consume **stored** readiness only.
6. Coach assignment and Athlete Readiness recommendation **may disagree**; both remain visible as distinct facts (see ADR-034).

## Consequences

- V3 coach APIs call training planning use cases via authorization/consent ports; they do not add a parallel readiness engine in `organization`.
- Formalizing ADR-029 does not require extracting a new Modulith module in V3.
- Superseding this ADR requires Lead + Athlete Intelligence agreement and an explicit new ADR.

## References

- `docs/V3_IMPLEMENTATION_PLAN.md`
- `docs/V2_IMPLEMENTATION_PLAN.md`
- `docs/TRAINING_API_V1.md`
- ADR-034 (TrainingPlan ownership and coach collaboration)
