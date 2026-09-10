# Athlete Readiness V3 — Authorization Matrix (frozen)

**Status:** Frozen for shipped V3 surfaces (Slices A–H on `develop` RC)  
**Authority:** Server-side only. UI visibility is **not** authorization.  
**Roles:** `ATHLETE` | `COACH` | `HEAD_COACH` | `TEAM_ADMIN` | `ORG_ADMIN` | `ORG_OWNER`

## Denial semantics (ADR-032)

| Situation | Response |
| --- | --- |
| Unauthenticated | **401** |
| Authenticated, inaccessible (foreign tenant, insufficient role, missing consent, unknown id) | **404** (no existence oracle) |
| CSRF filter failure | **403** `CSRF_INVALID` |
| Owned-resource conflict / illegal transition / optimistic lock | **409** or validation **4xx** |

Membership and consent are re-checked from the current store on every sensitive request. JWT is not the access graph.

## Not shipped (explicit)

| Capability | Status |
| --- | --- |
| Role change (`ROLE_CHANGED`) | **Not shipped** — no mutation API |
| `ORG_OWNER` transfer | **Not shipped** |
| Admin/org operational audit dump APIs | **Not shipped** |
| Bulk `EXPORT` product surface | **Deferred** |

**Membership creation:** invitations create memberships (plus Slice A atomic `ORG_OWNER` bootstrap on organization create). There is no separate role-assignment mutation.

## Capability matrix (shipped)

Legend: **Y** = allowed when ACTIVE membership (and team/org ACTIVE as required); **C** = allowed only with effective consent scope(s); **Own** = athlete-self only; **—** = not allowed / not shipped.

| Capability | ATHLETE | COACH | HEAD_COACH | TEAM_ADMIN | ORG_ADMIN | ORG_OWNER |
| --- | --- | --- | --- | --- | --- | --- |
| Create organization | Y (any authenticated account) | Y | Y | Y | Y | Y |
| Manage org/team config (update/archive org; create/update/archive team) | — | — | — | — | — | Y |
| View team / roster (roster-safe identity) | Y* | Y | Y | Y | Y† | Y† |
| Invite ATHLETE to team | — | Y | Y | Y | Y† | Y† |
| Invite COACH to team | — | — | Y | Y | Y† | Y† |
| Invite HEAD_COACH / TEAM_ADMIN to team | — | — | — | Y | Y† | Y† |
| Invite ORG_ADMIN | — | — | — | — | Y | Y |
| Remove team member | Leave self | — | Limited‡ | Y | Y† | Y† |
| Remove ORG_ADMIN | Leave self (non-owner) | — | — | — | Y | Y |
| Grant / revoke consent | Own | — | — | — | — | — |
| Coach athlete overview (consent-scoped sections) | —§ | C | C | C | C† | C† |
| Coach training collaboration (assign / modify) | — | C+W | C+W | —¶ | —¶ | —¶ |
| Athlete assignment actions (decline / unable) | Own | — | — | — | — | — |
| Team Readiness aggregate | — | Y | Y | Y | Y† | Y† |
| Athlete transparency | Own# | — | — | — | — | — |

\* Active teammates see **roster-safe identity only** (not email/contact, wellness, readiness, recovery, or training detail).  
† Via ACTIVE organization membership on the team's parent org (org-scoped), with Team and Organization ACTIVE.  
‡ `HEAD_COACH` may remove `ATHLETE` or `COACH` only.  
§ Overview HTTP requires `canViewTeam`; product coach overview is for staff viewers. Sensitive sections remain consent-gated (`NOT_SHARED` without scope).  
¶ **`TEAM_ADMIN` / `ORG_ADMIN` / `ORG_OWNER` cannot create coach assignments.** Only ACTIVE `COACH` or `HEAD_COACH` on the Team, with effective `TRAINING_COLLABORATION` for the athlete's current membership generation.  
# Athlete-self only (`GET /api/v1/athletes/me/transparency`). Transactional projection — **not** the raw `security_audit_events` stream.  
**C+W** = effective `TRAINING_COLLABORATION`.

## Surface notes

### Org / team management

- Organization create atomically bootstraps ACTIVE `ORG_OWNER` membership for the creator.
- Update/archive organization and create/update/archive team require ACTIVE `ORG_OWNER`.

### Roster

- `GET /api/v1/teams/{teamId}/roster` — ACTIVE ATHLETE memberships only; allow-listed identity fields; foreign → **404**.

### Invitations & membership lifecycle

- Invite / accept / decline / revoke per `InvitationAuthority` and invitation use cases.
- Accept creates exactly one ACTIVE membership (idempotent same-account replay).
- Leave / remove flip membership status; subsequent sensitive reads fail closed.

### Consent

- Athlete-self grant/revoke only. Coach cannot grant or revoke.
- Effective consent requires ACTIVE grant + matching ACTIVE athlete membership generation + ACTIVE team/org + scope present.

### Coach athlete overview

- Base roster-safe fields always when authorized to view the team athlete; each wellness/training section independently gated by consent scope (Slice D).

### Coach training collaboration

- Coach writes/reads require ACTIVE `COACH`/`HEAD_COACH`, ACTIVE athlete membership, ACTIVE team/org, and effective `TRAINING_COLLABORATION`. Missing authority → **404**.

### Team Readiness

- Viewers: ACTIVE `COACH` / `HEAD_COACH` / `TEAM_ADMIN` on the Team, or ACTIVE `ORG_ADMIN` / `ORG_OWNER` on the parent Organization. `ATHLETE` and foreign → **404**.
- Aggregate privacy: `minCohortSize = 5` (Slice F). GET is stored-read only (no hidden writes).

### Athlete transparency

- Athlete-self projection of membership, consent timestamps, and coach-assignment activity.
- Separate from durable security audit (ADR-035).

## References

- ADR-032, ADR-033, ADR-035
- `docs/V3_IMPLEMENTATION_PLAN.md` §5, §23–§23h
- `docs/V3_RELEASE_CERTIFICATION.md`
