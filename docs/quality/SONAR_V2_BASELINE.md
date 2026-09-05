# Athlete Readiness V2 — Sonar Quality Baseline

Authoritative Clean-as-You-Code and Overall metrics for the completed Athlete Readiness **V2** release baseline.

This document exists so **V3+** work can be compared objectively. It does **not** authorize weakening Quality Gate thresholds, inventing exclusions, suppressing findings, or starting V3 product implementation.

Canonical stewardship: [`docs/agents/security-code-quality.md`](../agents/security-code-quality.md) (Security / Code Quality Engineer — Independent Quality Gate Steward).

---

## Identity

| Field | Value |
| --- | --- |
| Product | Athlete Readiness (Devino Labs LLC) |
| Internal repo name | Universal Athlete Platform / UAP |
| Sonar project version | **2.0.0** |
| Main / develop SHA | `92ff2953f56a7b4b8882be585b5d5008b3ec5dc1` |
| Authoritative certification Verify run | [33890084078](https://github.com/Devino-Labs-LLC/Universal-Athlete-Platform/actions/runs/33890084078) |

At certification, `main` and `develop` both pointed at the SHA above.

---

## V2 Quality Gate (New Code)

| Measure | Value |
| --- | --- |
| Quality Gate | **PASSED** |
| New Code definition | **Specific version = 2.0.0** (`sonar.leak.period=2.0.0`, `sonar.leak.period.type=version`) |
| New Code LOC | **0** at the finalized V2 baseline |
| New Reliability | **A** |
| New Security | **A** |
| New Maintainability | **A** |
| New bugs | **0** |
| New vulnerabilities | **0** |
| New Security Hotspots reviewed | **100%** |

Coverage and duplication New Code thresholds were **not evaluated** on the final baseline analysis because there were **zero** post-baseline New Code lines to cover or duplicate. Those thresholds (**≥ 80%** coverage, **≤ 3%** duplicated lines on New Code) remain hard requirements for all subsequent V3+ New Code.

---

## Overall V2 metrics

| Measure | Value |
| --- | --- |
| LOC (ncloc) | approximately **113k** (`113090` at certification) |
| Overall coverage | **74.2%** |
| Overall duplication | **6.3%** |
| Overall Reliability | **C** |
| Overall Security | **D** |
| Overall Maintainability | **A** |
| Security Hotspots | **0** |
| Security Hotspots reviewed / review rating | **100%** / **A** |

**Overall Security D and Reliability C are not acceptable quality.** They are known historical debt that remains visible on Overall code and must be independently reviewed. They are not silently accepted, and they are not automatic V2 release blockers after the Specific-version `2.0.0` baseline (they sit outside New Code unless V3 work reopens those paths under the campground rule).

---

## Known historical debt (non-exhaustive)

Recorded so V3+ stewardship can track campground cleanup and Overall trend without pretending the debt is gone.

| Finding | Location / scope | Classification | Notes |
| --- | --- | --- | --- |
| `java:S4502` | `IdentitySecurityConfiguration` | Security | CSRF protection disabled — requires independent security review |
| `java:S3330` | `IdentitySecurityConfiguration` | Security | Cookie / HttpOnly finding — requires independent security review |
| `java:S2583` (multiple) | JPA repository paths and adaptation service | Reliability | Static-analysis always-false conditions — reliability technical debt requiring inspection |
| Historical web/mobile contract and schema duplication | Client apps (`apps/web`, `apps/mobile`) | Maintainability | High-value duplication candidates; do not mass-refactor solely for Overall ≤ 3% |
| Broader maintainability / code-smell backlog | Repository-wide (~1.2k maintainability issues at V2) | Maintainability | Overall Maintainability remains **A**; do not mass-refactor smells without architectural justification |

Raw issue count alone is not a health score. See steward guidance in [`docs/agents/security-code-quality.md`](../agents/security-code-quality.md).

---

## Desired Overall trend for V3+

Every substantial V3 slice should leave overall quality equal to or better than it found it.

| Metric | V2 baseline | Desired direction |
| --- | --- | --- |
| Coverage | 74.2% | upward |
| Duplication | 6.3% | downward |
| Reliability | C | toward **A** |
| Security | D | toward **A** |
| Maintainability | A | remain **A** |

There is **no** invented deadline requiring all historical debt to be eliminated before V3 begins. There is **no** permission to hide Overall debt by rebasing New Code or weakening the gate.

---

## Hard New Code requirements for V3+

Inherited by the Quality Gate Steward and all implementation agents:

- Reliability A (New Code)
- Security A (New Code)
- Maintainability A (New Code)
- Coverage ≥ 80% (New Code)
- Duplicated lines ≤ 3% (New Code)
- Security Hotspots reviewed 100%
- Sonar Quality Gate **PASSED**

Never weaken the gate, lower thresholds, add broad exclusions, add `NOSONAR`, fake coverage, remove tests for metrics, or mark false positives without a defensible reason.

---

## Change control

Updating this baseline (new release version, new SHA, new metrics) is a Documentation / Release + Quality Gate Steward action after an authorized certification analysis. Do not edit these numbers to make an incomplete analysis look complete.
