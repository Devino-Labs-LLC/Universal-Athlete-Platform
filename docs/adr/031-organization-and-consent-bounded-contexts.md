# ADR-031 — Organization + Consent bounded-context split

- **Status:** Accepted
- **Date:** 2026-09-05
- **Product:** Athlete Readiness V3 lock

## Context

V1/V2 modules are only `identity`, `athlete`, and `training`. Dumping tenancy, invitations, and wellness sharing into one “organization” module would create a god-module and couple roster lifecycle to consent threat models.

## Decision

1. Introduce Modulith module **`organization`**: Organization, Team, Membership, Invitation, contextual roles, org/team lifecycle. Flat topology only: **Organization → Teams**. No nested organizations in V3.
2. Introduce Modulith module **`consent`**: ConsentGrant, scopes, revocation, grantor=athlete authority.
3. Publish named interfaces early: `organization :: membership`, `consent :: grants`.
4. `training` may depend on those **ports** only — no shared JPA entities across modules.
5. `identity` remains AuthN; do not grow it into tenancy.
6. Cross-cutting **AuditPort** may persist under `organization` or a thin audit package; audit rules are ADR-035.

## Consequences

- Slice A can land org/team structure without shipping wellness sharing APIs.
- Consent remains independently reviewable by Security / Quality Gate Steward.
- Illegal Modulith edges fail `ModularityTests`.

## References

- `docs/V3_IMPLEMENTATION_PLAN.md`
- ADR-029, ADR-033, ADR-035
