# ADR-050 — Connected consent layers (connect ≠ share)

- **Status:** Proposed (awaiting Product Owner / Lead lock)
- **Date:** 2026-09-30
- **Product:** Athlete Readiness V5 — Connected Athlete

## Context

ADR-033: membership does not auto-grant sensitive sharing. Connecting a wearable must not automatically increase coach/org visibility of athlete data.

## Decision (proposed)

Treat Connected Athlete consent as four independent layers:

| Layer | Meaning |
| --- | --- |
| **A** | Athlete authorizes a provider / OS health connection |
| **B** | Athlete authorizes Athlete Readiness to process/store imported signals |
| **C** | Athlete grants coach visibility of derived product information (existing ConsentGrant scopes) |
| **D** | Organization visibility remains membership + consent constrained |

Rules:

1. Completing A does **not** imply B, C, or D.
2. Completing A+B does **not** imply C.
3. Coaches never receive provider credentials, raw provider payloads, or a wearable debugger console.
4. V5 MVP Lead recommendation: coaches continue to see only existing readiness/recovery projections under current ConsentScopes; raw connected streams are never coach-visible. Adding a new ConsentScope for connected metadata requires Product Owner lock.
5. Disconnect stops future sync; historical normalized athlete self-history retention is a Product Owner lock (Lead recommends retain athlete self-history, drop credentials immediately).

## Consequences

- Sharing UX and Connected Apps UX stay separate.
- Threat model T23–T36 treats connection≠share as STOP-level if violated.

## References

- ADR-033, ADR-032
- `docs/V5_IMPLEMENTATION_PLAN.md`
