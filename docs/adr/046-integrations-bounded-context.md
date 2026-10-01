# ADR-046 — Integrations bounded context (Connected Athlete)

- **Status:** Accepted
- **Date:** 2026-09-30
- **Product:** Athlete Readiness V5 — Connected Athlete
- **Product Owner lock:** D2 = A (`integrations` module)

## Context

V3 §19 reserved V5 for wearables / provider integrations behind a separate integrations seam. State Engine remains owned by `training` (ADR-029). Billing providers (Stripe / App Store / Play) already live in `billing` and must not absorb health/fitness connectors.

## Decision

1. Create Modulith module **`integrations`** for Connected Athlete connections, credentials, sync orchestration, provider adapters, and provider-neutral observation ports.
2. Do **not** place health/fitness connectors in `training`, `athlete`, `billing`, or `organization`.
3. `training` may depend on published `integrations :: evidence` (or equivalent) ports only — never provider SDKs/DTOs.
4. Commercial billing “provider” vocabulary remains distinct from health integration providers.

## Consequences

- V5 foundation can ship dormant (`@ConditionalOnProperty`) with zero live connectors.
- New Flyway tables for connections/evidence live under the integrations ownership story.
- ADR-044 patterns may be reused for webhook/job idempotency; `billing_provider_events` must not be widened for wearables.

## References

- `docs/V3_IMPLEMENTATION_PLAN.md` §19
- `docs/V5_IMPLEMENTATION_PLAN.md`
- ADR-029, ADR-044
