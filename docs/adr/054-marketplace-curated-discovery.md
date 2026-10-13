# ADR-054 — Curated Athlete Marketplace as discovery, not commerce infrastructure

- **Status:** Accepted
- **Date:** 2026-10-12
- **Product:** Athlete Readiness V8 Athlete Marketplace V1

## Context

V7 added UAP-controlled sponsors and labeled placements. Athletes still have no in-product catalog of approved equipment, programs, or offers. An open store would require KYC, tax, payouts, and inventory. Those are out of scope.

Marketplace content that entered CoachContext or the State Engine would turn commerce into athlete intelligence.

## Decision

1. Add a `marketplace` Modulith module. Allowed dependencies: `identity :: auth`, `organization :: membership`, `sponsorship :: catalog`.
2. Listings reference an existing V7 sponsor when a partner is associated. No automatic campaign↔listing conversion.
3. Server clock owns availability and offer windows. Clients display returned payloads.
4. Eligibility uses membership only.
5. Purchases leave UAP through validated HTTPS URLs. UAP does not take seller payments in V8.
6. Saves and analytics are first-party and aggregate. Partners never receive athlete health or identity lists.
7. Operators are the V7 allowlist. Admin APIs 404 otherwise.

## Consequences

- A later payments marketplace needs a new version and ADR.
- The same brand can be a sponsor and a listing partner without sharing inventory.

## References

- [`docs/V8_IMPLEMENTATION_PLAN.md`](../V8_IMPLEMENTATION_PLAN.md)
- ADR-053 sponsorship presentation boundary
