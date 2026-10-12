# ADR-053 — Sponsorship as a presentation boundary

- **Status:** Accepted
- **Date:** 2026-10-12
- **Product:** Athlete Readiness V7 Sponsor & Partner Ecosystem V1

## Context

V7 adds UAP-controlled sponsors and campaigns. Historical plans called V7 a marketplace; the product lock is narrower: approved partners, labeled placements, no ad auction.

Readiness, State Engine, recommendations, connected evidence, and AI Coach already have owners. A sponsorship module that imported those packages could leak health into targeting or Coach prompts.

There is no platform-admin role. Organization roles are tenant-scoped. Inventing `ROLE_ADMIN` would contradict ADR-030 and ADR-032.

There is no object-storage abstraction.

## Decision

1. Add a `sponsorship` Modulith module. Allowed dependencies: `identity :: auth` and `organization :: membership` only. It must not depend on training, athlete context, integrations, or aicoach.
2. Operators are account IDs in `uap.sponsorship.operator-account-ids`. Admin APIs 404 for everyone else.
3. Server `Clock` decides whether a campaign is presentable. Clients only display returned placements.
4. Creatives and destinations are HTTPS URL strings. Reject non-https schemes. Sponsor text is untrusted plain text.
5. Eligibility uses membership (account, org, team) only. Never readiness, evidence, body data, or Coach messages.
6. Reporting stores first-party impression/click aggregates. Reports never include athlete health or identity lists.
7. Athlete GET placements is read-only aside from explicit impression/click POSTs into `sponsorship_events`.

## Consequences

- A later marketplace or billing-for-impressions needs a new version and ADR.
- Operators must be listed in configuration until a real staff model exists.
- Missing logos fail visually, not as a platform media outage.

## References

- [`docs/V7_IMPLEMENTATION_PLAN.md`](../V7_IMPLEMENTATION_PLAN.md)
- ADR-032 authorization / IDOR
- ADR-052 AI Coach model gateway
