# Athlete Readiness V3 — Release Certification

**Document type:** RELEASE CANDIDATE evidence template  
**Scope:** V3 Slices A–H on `develop`  
**Not:** Production-complete certification. **`develop` RC ≠ production certification.** Promotion to `main` is a separate, explicitly authorized decision.

---

## 1. Identity

| Field | Value |
| --- | --- |
| Product | Athlete Readiness (Devino Labs LLC) |
| Internal repo | Universal Athlete Platform / UAP |
| Branch | `develop` |
| RC commit SHA | `e173db718422cb9c06a74239b84e533c854878ee` |
| Flyway schema | **V34** (`security_audit_events`) |
| Authorization matrix | [`docs/security/V3_AUTHORIZATION_MATRIX.md`](security/V3_AUTHORIZATION_MATRIX.md) |
| V2 Sonar baseline | [`docs/quality/SONAR_V2_BASELINE.md`](quality/SONAR_V2_BASELINE.md) |

---

## 2. Slice A–H scope status

All complete on `develop` RC (fill SHA above when certifying):

| Slice | Theme | Status on develop RC |
| --- | --- | --- |
| A | Organization & Team foundation | Complete |
| B | Memberships & invitations | Complete |
| C | ConsentGrant runtime | Complete |
| D | Roster + coach athlete overview | Complete |
| E | Coach training collaboration | Complete |
| F | Team Readiness aggregate | Complete |
| G | Coach/athlete UX completion + transparency | Complete |
| H | Durable audit + hardening / RC gate | Complete |

---

## 3. Verify run

| Field | Value |
| --- | --- |
| Workflow | Verify (`.github/workflows/verify.yml`) |
| Run URL / ID | [34459986682](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/34459986682) |
| Result | **success** (all shards + Sonar quality gate) |
| Commit | `e173db718422cb9c06a74239b84e533c854878ee` |

---

## 4. Sonar — New Code (V3+)

| Measure | Value |
| --- | --- |
| Quality Gate | **PASSED** |
| New Reliability | **A** (1.0) |
| New Security | **A** (1.0) |
| New Maintainability | **A** (1.0) |
| New Code coverage | **95.3%** (require ≥ 80%) |
| New Code duplicated lines | **0.0%** (require ≤ 3%) |
| New Security Hotspots reviewed | **100%** |

---

## 5. Sonar — Overall trend

| Measure | V2 baseline | Slice G (prior RC reference) | This RC |
| --- | --- | --- | --- |
| Overall coverage | **74.2%** | **75.6%** | **75.7%** |
| Overall duplication | **6.3%** | **5.9%** | **5.9%** |
| Overall Security | **D** | (historical) | **A** |
| Overall Reliability | **C** | (historical) | **A** |
| Overall Maintainability | **A** | — | **A** |

Trend: Coverage slightly up vs Slice G; duplication flat; Overall Security/Reliability improved to **A** on this analysis (do not treat as permanent debt erasure — steward continues campground review). New Code Clean-as-You-Code remains the hard gate.

---

## 6. Independent verdicts

| Role | Verdict | Notes / sign-off |
| --- | --- | --- |
| QA / Test Automation | **PASS** | Completeness, org/consent/training rollback, stale authZ, transparency no-hidden-write, RC04; Verify [34459986682](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/34459986682) green |
| Security / Code Quality (Quality Gate Steward) | **PASS** | Durable audit posture; Flyway current=34; no public audit API; New Code QG PASSED; S3330 CSRF cookie residual accepted |
| Athlete Intelligence / Data | **PASS** | No State Engine / readiness calculator change; assignments do not write state; transparency remains read-only projection |

---

## 7. Authorization matrix

Frozen matrix for shipped surfaces only:

→ [`docs/security/V3_AUTHORIZATION_MATRIX.md`](security/V3_AUTHORIZATION_MATRIX.md)

Denial: unauthenticated **401**; inaccessible authenticated **404**; CSRF filter failures **403** `CSRF_INVALID`.

---

## 8. Audit completeness (Slice H)

Durable append-only store: Flyway **V34** `security_audit_events` via module `com.devinolabs.uap.audit`. Adapters behind existing Organization / Consent / Training audit ports.

### Shipped event types

| Event type | Domain |
| --- | --- |
| `ORGANIZATION_CREATED` | Organization |
| `ORGANIZATION_ARCHIVED` | Organization |
| `TEAM_CREATED` | Organization |
| `TEAM_ARCHIVED` | Organization |
| `INVITATION_CREATED` | Organization |
| `INVITATION_ACCEPTED` | Organization |
| `INVITATION_DECLINED` | Organization |
| `INVITATION_REVOKED` | Organization |
| `MEMBERSHIP_ACTIVATED` | Organization |
| `MEMBERSHIP_LEFT` | Organization |
| `MEMBERSHIP_REMOVED` | Organization |
| `CONSENT_GRANTED` | Consent |
| `CONSENT_REVOKED` | Consent |
| `WORKOUT_ASSIGNED` | Training |
| `WORKOUT_ASSIGNMENT_MODIFIED` | Training |
| `WORKOUT_ASSIGNMENT_DECLINED` | Training |
| `WORKOUT_ASSIGNMENT_UNABLE` | Training |

### Deferred / not emitted

| Event | Reason |
| --- | --- |
| `ROLE_CHANGED` | No role-change mutation shipped |
| Export-related audit | `EXPORT` product surface deferred |

Payloads: identifiers and allow-listed metadata only — no tokens, credentials, or wellness bodies (ADR-035).

---

## 9. Evidence references (fill links/paths at certification)

| Concern | Evidence |
| --- | --- |
| Stale authorization (membership/consent revoke → immediate deny) | `V3StaleAuthorizationIntegrationTests` |
| Privacy (Team Readiness `minCohortSize = 5`, complementary suppression) | `TeamReadinessPrivacySuppressionTests`, `TeamReadinessAggregationIntegrationTests` |
| No-hidden-write on GET / roster / overview / Team Readiness / transparency | Consent/roster/readiness GET suites + `AthleteTransparencyGetNoHiddenWritesIntegrationTests` |
| Durable audit append + completeness + rollback | `SecurityAuditPersistenceIntegrationTests`, `SecurityAuditCompletenessIntegrationTests`, `SecurityAuditRollbackIntegrationTests`, `SecurityAuditConsentRollbackIntegrationTests`, `SecurityAuditTrainingRollbackIntegrationTests` |
| Transparency ≠ raw audit | `AthleteTransparencyNotAuditDumpTests` |
| Cache isolation | Web `RC04.crossAccountIsolation.test.ts` (`consentKeys.transparency`), coach `invalidation.test.ts` |

---

## 12. Threat model T1–T11 disposition

Canonical labels from `docs/V3_IMPLEMENTATION_PLAN.md` §14.1.

| ID | Threat | Mitigation | Evidence | Status |
| --- | --- | --- | --- | --- |
| T1 | Cross-org / cross-team access | Server membership re-check; 404 inaccessible | `OrganizationIdorSecurityIntegrationTests`, training IDOR suites | MITIGATED |
| T2 | Coach impersonation / confused deputy | Actor from session; consent + membership required | Coach assignment/overview authZ suites | MITIGATED |
| T3 | Invitation token abuse | Hash storage; non-oracle; revoke/expire/wrong-account | `InvitationLifecycleHttpIntegrationTests`, concurrency suite | MITIGATED |
| T4 | Membership escalation | Locked role enum; invite authority matrix; no role-change API | `RoleEscalationSecurityIntegrationTests`; matrix freeze | MITIGATED |
| T5 | Consent bypass | Athlete-only grant; effective-access service; revoke immediate | Consent lifecycle + coach overview suites | MITIGATED |
| T6 | Wellness leakage | Consent scopes; roster-safe fields only | Overview projection + roster field tests | MITIGATED |
| T7 | Roster enumeration | 404 foreign; allow-listed roster fields | Coach roster authZ | MITIGATED |
| T8 | Stale authorization | Re-check membership/consent each request | `V3StaleAuthorizationIntegrationTests` | MITIGATED |
| T9 | Audit tampering | Append-only app store; mutation+audit same TX | Persistence, completeness, org/consent/training rollback ITs | MITIGATED |
| T10 | Export / download risks | No export product surface in V3 | Deferred — no EXPORT action shipped | ACCEPTED/DEFERRED |
| T11 | Aggregate re-identification | Team Readiness min cohort 5 + complementary suppression | `TeamReadinessPrivacySuppressionTests`, aggregation IT | MITIGATED |

---

## 13. Transparency vs durable audit

- Athlete transparency (`GET /api/v1/athletes/me/transparency`) remains a **transactional projection** (membership, consent timestamps, assignment activity).
- It is **separate** from durable `security_audit_events` and must not expose the raw security audit stream.
- Athlete-self only (see authorization matrix).

---

## 14. Known accepted residual debt

| Item | Disposition |
| --- | --- |
| `java:S3330` (CSRF cookie / HttpOnly finding on identity security config) | **Intentional accepted residual** pending separate security review — do not “fix” by weakening CSRF |
| Overall duplication **5.9%** | Historical density; New Code duplication **0.0%**; campground on touch |
| Admin operational audit dump UI | Deferred (V4+) |

---

## 15. V4+ deferrals (non-exhaustive)

- Role-change and `ORG_OWNER` transfer APIs
- Bulk `EXPORT` and export audit events
- Coach mobile console
- Season aggregate / nested orgs
- Staff / read-only role
- Parent/guardian / minor-specific product support
- Admin operational audit dump UIs
- Provider/SIS roster sync

---

## 16. Explicit boundary

| Statement | Meaning |
| --- | --- |
| This document certifies a **`develop` RELEASE CANDIDATE** | Evidence for Slice H / V3 RC gate on the integration branch |
| It does **not** certify production readiness alone | Production requires separate `main` promotion authorization, Verify on that commit, and steward/QA confirmation for the release baseline |
| `develop` RC ≠ `main` promotion | Do not treat RC fill-in as license to push, merge, deploy, tag, or publish |

---

## 17. Certification checklist (sign when filled)

- [x] RC SHA recorded
- [x] Verify run green for that SHA
- [x] New Code Sonar filled and gate PASSED
- [x] Overall metrics recorded vs Slice G / V2
- [x] QA / Steward / Athlete Intelligence verdicts recorded
- [x] Audit event list matches shipped adapters
- [x] Residual debt acknowledged
- [x] `main` promotion **not** implied by this document alone

---

## 18. RC verdict

**Athlete Readiness V3 Release Candidate: VERIFIED** on `develop` at `e173db718422cb9c06a74239b84e533c854878ee`.

This is **not** V3 production-complete certification. Do not merge to `main`, tag, publish, or deploy from this document alone.
