# ADR-050 — Connected consent layers (connect ≠ share)

- **Status:** Accepted
- **Date:** 2026-09-30
- **Product:** Athlete Readiness V5 — Connected Athlete
- **Product Owner locks:** D8 = A (no coach connected metadata); D9 = B; D10 = A (free in V5)

## Context

ADR-033: membership does not auto-grant sensitive sharing. Connecting a wearable must not automatically increase coach/org visibility of athlete data.

## Decision

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
3. Coaches never receive provider credentials, raw provider payloads, device lists, or a wearable debugger console.
4. V5 MVP: coaches continue to see only existing readiness/recovery projections under current ConsentScopes. **No new coach connected-source/freshness metadata** (D8 = A).
5. Disconnect stops future sync and drops credentials; normalized athlete self-history may remain (D9 = B). Explicit deletion is a separate privacy flow.
6. Connected Athlete capabilities are **free in V5** (D10 = A); keep an entitlement seam but do not wire commercial gating.

## Consequences

- Sharing UX and Connected Apps UX stay separate.
- Threat model T23–T36 treats connection≠share as STOP-level if violated.

## References

- ADR-033, ADR-032
- `docs/V5_IMPLEMENTATION_PLAN.md`
