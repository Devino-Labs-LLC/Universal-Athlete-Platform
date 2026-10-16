# V9 Implementation Plan — Advanced Wearables & Sensor Fusion V1

Status: **Accepted product lock** (Athlete Readiness / UAP V9).

Starting baseline: `develop` at `c67d48f7df0be57acde06ec6fba136fe58dc2f04` (V8 Marketplace V1).

V9 extends the V5 Connected Athlete module. It does **not** replace HealthKit / Health Connect, rewrite READINESS_V1, or open vendor SDKs into training, Coach, sponsorship, or marketplace.

## Objective

UAP reasons about **devices**, not only provider connections. External sensors enter through adapters, become normalized evidence with provenance and quality, and may be fused deterministically. Athlete intelligence consumes only approved fused summaries. AI Coach never becomes a sensor or a BLE controller.

```
External device / wearable
         ↓
Provider Adapter
         ↓
Connected Evidence Boundary
         ↓
Normalized Sensor Evidence
         ↓
Deterministic Athlete Intelligence
```

Device vendors do not write State Engine, readiness, recovery, recommendations, or AI Coach rows.

## Architecture

Remain inside the existing `integrations` Modulith module (ADR-046). Add published APIs:

- `integrations :: evidence` — original rows + fused views
- `integrations :: connections` — provider accounts (unchanged contract)
- `integrations :: devices` — physical/logical devices, capabilities, quality copy
- `integrations :: wearable-ops` — athlete-owned ingest aggregates (no partner access)

Training may depend on `integrations :: evidence` only for the **WearableReadinessPolicy** gate. READINESS_V1 formula is unchanged. Missing wearable data is not a penalty.

AI Coach may receive high-level fused summaries (families, freshness, quality, missing/unavailable). No raw samples, no BLE identifiers unless product-required (V9: not required), no credentials.

Marketplace and sponsorship must not depend on wearable internals.

## Phases

| Phase | Scope |
| --- | --- |
| V9.1 | This plan + ADR-055 |
| V9.2 | Device domain (identity separate from connection) |
| V9.3 | Provider-neutral sensor families/units |
| V9.4 | Server-owned capability discovery |
| V9.5 | Ingest + dedupe + device association |
| V9.6 | Deterministic fusion (preserve originals) |
| V9.7 | Quality HIGH / MEDIUM / LOW / UNKNOWN |
| V9.8 | Wearable adapter contracts + mock tests (live OAuth deferred) |
| V9.9 | BLE / direct-sensor contracts (mobile hardware, backend revalidate) |
| V9.10 | Connected Devices UI (provider vs device) |
| V9.11 | Athlete data-quality copy |
| V9.12 | Readiness consumption gate (no formula rewrite) |
| V9.13 | Coach fused-summary boundary |
| V9.14 | Ownership, isolation, no partner targeting |
| V9.15 | Certification DISABLED / DEVELOPMENT / CERTIFIED |
| V9.16 | Operational ingest metrics (no raw logs) |
| V9.17 | Tests and hardening |

## Device model

A **connection** is an integration account (Apple Health, Health Connect, future OAuth, or a BLE_DIRECT upload channel).

A **device** is a physical or logical source exposed by that connection (Apple Watch vs iPhone Health app vs chest strap).

Fields: id, provider, external device id, type, manufacturer, model, display name, capabilities, athlete/account ownership, connection id, first/last seen, status, firmware when supplied, last successful data time.

Types used now: `SMARTWATCH`, `FITNESS_TRACKER`, `HEART_RATE_MONITOR`, `CHEST_STRAP`, `GPS_DEVICE`, `POWER_METER`, `FOOT_POD`, `SLEEP_TRACKER`, `PERFORMANCE_SENSOR`, `OTHER`.

One connection may expose many devices. Device id is never the connection id.

## Sensor families

Store families: existing `SLEEP`, `ACTIVITY`, `HEART`, `HRV`, `WORKOUT` plus `POWER`, `CADENCE`, `RESPIRATORY`, `TEMPERATURE`.

GPS-derived workload stays in `WORKOUT` / `ACTIVITY` with explicit signal types. Not every family is a readiness input.

Canonical units remain adapter-owned. Added: `WATT`, `RPM`, `CELSIUS`, `BREATH_PER_MIN`.

## Fusion (deterministic)

Original accepted rows are never deleted to create a fused view.

Group overlapping facts by athlete + family + signal type + UTC calendar day of `observedAt`.

Score, highest wins; ties: newer `observedAt`, then evidence id UUID (stable):

| Factor | Weight |
| --- | --- |
| Provenance `DIRECT_SENSOR` | 400 |
| Provenance `CLIENT_DEVICE` | 350 |
| Provenance `OAUTH_PROVIDER` | 300 |
| Provenance `OS_HUB` | 200 |
| Quality HIGH / MEDIUM / LOW / UNKNOWN | 40 / 20 / 5 / 0 |
| Observed within 6 hours of `now` | +15 |
| Device advertises matching capability | +10 |

No LLM. No undocumented weighted black box.

## Quality

`HIGH` — direct sensor or device-specific sample, fresh, complete, capability-backed.

`MEDIUM` — OS hub / aggregated provider sample that is complete and not stale.

`LOW` — stale, incomplete, or capability mismatch.

`UNKNOWN` — quality not supplied and not derivable.

Stale windows (policy, tested): HEART/HRV/POWER/CADENCE 24h; SLEEP 36h; others 48h.

Athletes see copy such as “Your sleep data hasn't updated recently.” Never alarmist or diagnostic.

Readiness gate: only fused `HIGH` or `MEDIUM` within the stale window. Athlete-entered recovery still wins (V5 D6). Below-threshold evidence remains stored and visible as low quality.

## Providers

| Provider | V9 posture |
| --- | --- |
| APPLE_HEALTHKIT | Preserve V5 upload-only OS hub. Live sample certification still deferred (Apple Development signing). |
| HEALTH_CONNECT | Preserve V5. |
| GARMIN, FITBIT, WHOOP, OURA, POLAR | Adapter interface + config + DEVELOPMENT certification + mock tests. Live OAuth deferred (no credentials, no scraping). |
| BLE_DIRECT | Client owns discovery/GATT. Server revalidates normalized batches. Native BLE SDK not added in V9 (risky platform work). |

Certification: `DISABLED`, `DEVELOPMENT`, `CERTIFIED`. Compiling an adapter is not “supported.” Production exposure requires CERTIFIED **and** the existing per-provider enabled flag.

## Non-goals

Medical diagnostics, ECG/arrhythmia/glucose treatment, medical-device claims, AI-generated biometrics, AI/BLE device control, sponsor or marketplace targeting from sensors, raw sensor sales, vendor scraping, V10.

## Preservation

- V8: wearable evidence never ranks, filters, or merchandises listings.
- V7: sponsor targeting remains membership/account-scope.
- V6: Groq stays behind ModelGateway. Model output is never evidence.
- V5: HealthKit/Health Connect lifecycle and evidence uniqueness preserved.

## Schema

Flyway **V43** after V42 marketplace catalog.

## Deferred (do not block V9)

1. V7 operator allowlist configuration
2. V6 live Groq acceptance
3. V5 Apple Health real-sample acceptance (Apple Development signing)
4. Live Garmin / Fitbit / WHOOP / Oura / Polar OAuth
5. Native BLE hardware pairing on iOS/Android
