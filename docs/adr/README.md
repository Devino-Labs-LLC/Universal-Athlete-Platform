# Architecture Decision Records (ADRs)

Canonical ADRs for Athlete Readiness (Universal Athlete Platform / UAP).

## Format

Each ADR is a Markdown file:

`NNNN-short-kebab-title.md`

Typical sections:

1. **Status** — Proposed | Accepted | Superseded | Deprecated  
2. **Context** — Forces and repository reality  
3. **Decision** — What we will do  
4. **Consequences** — Positive, negative, follow-ups  
5. **References** — Related plans, contracts, ADRs  

## Numbering

| Rule | Detail |
| --- | --- |
| Width | Four-digit zero-padded (`0029`, file prefix `029` historically accepted as `ADR-029`) |
| ADR-029 | **Reserved / externally authoritative** for State Engine vs Planning ownership. Formalized in-repo even though the number predated `docs/adr/`. |
| ADR-001–028 | **Not present** in this repository. Assumed reserved or historical outside this tree. **Do not reuse** those numbers for new in-repo ADRs. |
| ADR-030 onward | Assigned sequentially for significant V3+ decisions created in this repository. |

Ordinary implementation details (invitation TTL defaults, UI route prefixes, copy) belong in `docs/V3_IMPLEMENTATION_PLAN.md` or slice contracts — **not** new ADRs.

## Index (V3 lock)

| ADR | Title |
| --- | --- |
| [029](029-state-engine-vs-planning-ownership.md) | State Engine vs Planning ownership |
| [030](030-multi-persona-account.md) | Multi-persona Account |
| [031](031-organization-and-consent-bounded-contexts.md) | Organization + Consent bounded-context split |
| [032](032-authorization-and-idor-posture.md) | Authorization / IDOR posture |
| [033](033-consent-and-sharing-authority.md) | Consent / sharing authority |
| [034](034-training-plan-ownership-and-coach-collaboration.md) | TrainingPlan ownership and coach collaboration |
| [035](035-audit-model.md) | Audit model |

Product Owner decisions that ground these ADRs are recorded in [`docs/V3_IMPLEMENTATION_PLAN.md`](../V3_IMPLEMENTATION_PLAN.md) §20.
