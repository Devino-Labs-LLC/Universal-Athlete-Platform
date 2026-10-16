# ADR-055 — Advanced wearable provider boundary, devices, and sensor fusion

- **Status:** Accepted
- **Date:** 2026-10-12
- **Product:** Athlete Readiness V9 Advanced Wearables & Sensor Fusion V1

## Context

V5 stores OS-hub evidence with a free-text `sourceDeviceOrApp` and string `qualityCode`. V9 must distinguish a provider **connection** from a physical/logical **device**, normalize more sensor families, fuse overlapping sources without destroying originals, and keep vendors out of the State Engine.

Live Garmin/Fitbit/WHOOP/Oura/Polar credentials and Apple Development signing are not available. Native BLE pairing would pull a new mobile BLE stack.

## Decision

1. **Stay in `integrations`.** Do not add a second evidence module. Publish `devices` and fused evidence APIs.
2. **Device identity** is a first-class row keyed by `(athlete_id, provider, external_device_id)`. It is not the connection id. One connection may own many devices.
3. **Capabilities** are server-persisted from adapter-known or provider-reported metadata. Clients may display them. Clients must not be treated as authoritative.
4. **Normalized evidence** remains `ConnectedEvidence`. Add optional `device_id`, bounded quality (`HIGH`/`MEDIUM`/`LOW`/`UNKNOWN`), additional families/units, and provenance `DIRECT_SENSOR`. Raw vendor payloads stay behind adapters.
5. **Fusion** is a deterministic read model (`FusedEvidenceView`). Precedence is documented in `docs/V9_IMPLEMENTATION_PLAN.md` (specificity, freshness, quality, capability). Originals stay `ACTIVE`.
6. **Readiness** keeps READINESS_V1. Training may consume fused views only through `WearableReadinessPolicy` (HIGH/MEDIUM, not stale, no double-count, athlete-entered recovery wins, missing wearable is not a penalty).
7. **Coach** receives fused family counts, freshness, quality, and missing/unavailable — never raw samples, credentials, or BLE MAC addresses.
8. **Adapters** for OAuth wearables compile with DEVELOPMENT certification and fail live pull with `PROVIDER_LIVE_UNAVAILABLE` unless an explicit mock flag is on. No portal scraping.
9. **BLE:** mobile owns hardware; backend revalidates. V9 ships contracts and mock validation, not a native BLE SDK.
10. **Certification** `DISABLED` / `DEVELOPMENT` / `CERTIFIED`. Production requires CERTIFIED plus the existing enabled flag.
11. **Privacy:** athlete ownership on every device/evidence API (404 for foreign ids). No wearable data in sponsor or marketplace reporting. Ops metrics are counts and error categories only.

## Consequences

- V5 evidence uniqueness `(provider, athlete_id, external_record_id)` is unchanged.
- Enabling a wearable flag in DEVELOPMENT does not claim a certified live integration.
- Native BLE pairing and live OAuth remain later acceptance items.

## References

- [`docs/V9_IMPLEMENTATION_PLAN.md`](../V9_IMPLEMENTATION_PLAN.md)
- ADR-046 integrations module, ADR-047 evidence store, ADR-048 connection vs sync, ADR-052 Coach gateway, ADR-053/054 commercial isolation
