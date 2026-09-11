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

## Index (V3 lock — Accepted)

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

## Index (V4 commercialization — Accepted)

Product Owner decisions: [`docs/V4_IMPLEMENTATION_PLAN.md`](../V4_IMPLEMENTATION_PLAN.md) §22. Do not begin V4 Slice A until explicitly authorized to implement.

| ADR | Title |
| --- | --- |
| [036](036-billing-bounded-context.md) | Billing bounded-context ownership |
| [037](037-provider-neutral-entitlements.md) | Provider-neutral entitlement model |
| [038](038-organization-vs-individual-subscriptions.md) | Organization vs individual subscription ownership |
| [039](039-billing-provider-channel-strategy.md) | Billing-provider / channel strategy |
| [040](040-subscription-lifecycle.md) | Subscription lifecycle |
| [041](041-seat-and-usage-semantics.md) | Active-athlete band / usage semantics |
| [042](042-authorization-vs-entitlement.md) | Authorization vs entitlement |
| [043](043-tax-channel-boundaries.md) | Tax responsibility boundaries |
| [044](044-provider-webhook-idempotency.md) | Provider webhook / idempotency model |
| [045](045-duplicate-subscription-and-provider-switch.md) | Duplicate-subscription / provider-switch policy |
