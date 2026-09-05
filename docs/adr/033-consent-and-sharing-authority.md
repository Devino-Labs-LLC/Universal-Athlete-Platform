# ADR-033 — Consent / sharing authority

- **Status:** Accepted
- **Date:** 2026-09-05
- **Product:** Athlete Readiness V3 lock

## Context

Organizations must not treat athletes as owned data objects. Membership alone cannot unlock wellness/readiness/training-sensitive views.

## Decision

1. **Grantor** is the athlete (Account’s Athlete profile). Coaches cannot unilaterally grant or re-grant after revoke.
2. **Active membership** permits **roster-safe identity only**. No sensitive data-sharing scopes auto-grant on join.
3. Readiness, recovery/check-in detail, training-sensitive history, collaboration write, export, and similar scopes require **explicit** ConsentGrant.
4. **Revocation / membership removal / leave** ends organization/coach access to sensitive athlete data **immediately** for product views and APIs.
5. Security/audit records may remain internally retained (ADR-035). Historical sensitive wellness/readiness **must not** remain available through coach/org product views after authorization ends.
6. Athlete self-history remains fully available to the athlete.
7. School organizations may exist as Organization types/labels, but V3 **does not claim** minor-specific legal compliance (FERPA/COPPA/etc.). Parent/guardian product support is **deferred**; legal/product review is a future dependency.
8. ConsentGrant lifecycle: `ACTIVE` → `REVOKED`; re-grant creates a **new** grant id/version.

## Consequences

- Slice C must ship before coach wellness reads (Slices D/F).
- Team readiness inclusion requires appropriate consent scopes plus cohort rules.
- QA matrix treats membership-without-consent as Deny for sensitive fields.

## References

- `docs/V3_IMPLEMENTATION_PLAN.md` §6, §20
- ADR-031, ADR-032, ADR-035
