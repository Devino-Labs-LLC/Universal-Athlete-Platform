# ADR-032 — Authorization / IDOR posture

- **Status:** Accepted
- **Date:** 2026-09-05
- **Product:** Athlete Readiness V3 lock

## Context

Athlete-self APIs use ownership queries and return **404** for inaccessible foreign IDs (no existence leak). V3 adds multi-tenant org/team graphs, invitations, and consent. UI route guards must never become the security boundary.

## Decision

### Denial semantics

| Situation | HTTP | Notes |
| --- | --- | --- |
| Unauthenticated | **401** | Existing identity posture |
| Authenticated but resource not accessible (wrong org/team, non-member, insufficient role, missing consent for sensitive projection, foreign id) | **404** | Preserve no-existence-leak; do not use 403 as an existence oracle |
| Authenticated, known self-service conflict (duplicate pending invite policy, optimistic lock, illegal state transition on **owned** resource) | **409** / validation **4xx** | As today for athlete-owned conflicts |
| Invitation token revoked/expired/wrong-account | Fail closed with **non-oracle** responses (prefer 404 or uniform safe code — no “exists but not yours” differentiation that enables enumeration) | Rate-limit accept |

“Known forbidden” for authenticated users on **foreign** tenant resources remains **404**, not 403, unless a future ADR explicitly changes the contract.

### Roles (initial V3 enum)

`ATHLETE` | `COACH` | `HEAD_COACH` | `TEAM_ADMIN` | `ORG_ADMIN` | `ORG_OWNER`

Staff/read-only **deferred**.

### Cardinality

- Flat Organization → Teams.
- Account ↔ many Organizations; Account ↔ many Teams (including across orgs).
- Athletes may hold multiple **ACTIVE** team memberships simultaneously.

### Peer visibility

Active teammates may see **roster-safe identity only** (e.g. display name / roster labels). Sharing a team does **not** expose email/contact, wellness, readiness, recovery, or training detail.

### Runtime checks

Membership and consent are evaluated from **current** store on each sensitive request (JWT is not the access graph). Revocation/removal takes effect immediately for subsequent calls.

## Consequences

- Authorization matrix tests must assert 404 (not 403) for cross-tenant Deny cells.
- Invitation accept endpoints need abuse/rate-limit design without oracle responses.
- Capability matrix lives in the V3 plan and is enforced server-side only.

## References

- `docs/V3_IMPLEMENTATION_PLAN.md` §5, §20
- ADR-030, ADR-033
- Historical pattern: training IDOR → 404
