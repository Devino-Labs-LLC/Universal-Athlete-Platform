# Connected Apps (V5)

Athlete-facing health provider connections. Separate from Premium billing.

## C1 — Apple HealthKit library choice

**Library:** [`react-native-health`](https://github.com/agencyenterprise/react-native-health) (`^1.19`).

**Why:** Maintained HealthKit bridge with an Expo config plugin (`NSHealthShareUsageDescription`, HealthKit entitlement). Works with Expo SDK 57 **development builds** / EAS — not Expo Go. Read-only permissions for sleep, resting HR, HRV (SDNN), steps / active energy, and workouts.

Native HealthKit APIs require an iOS device or simulator rebuild after `expo prebuild`. On Windows CI / non-iOS hosts the JS adapter returns `isAvailable() === false` and tests mock the native module.

## C2 — Android Health Connect library choice

**Library:** [`react-native-health-connect`](https://github.com/matinzd/react-native-health-connect) (`^4.1`) + [`expo-build-properties`](https://docs.expo.dev/versions/v57.0.0/sdk/build-properties/) (`minSdkVersion: 26`).

**Why:** Maintained Health Connect bridge with a built-in Expo config plugin (v4+) that registers the permission delegate. Works with Expo SDK 57 **development builds** / EAS — not Expo Go. Read-only permissions for SleepSession, RestingHeartRate, HeartRateVariabilityRmssd, Steps, ActiveCaloriesBurned, and ExerciseSession.

Native Health Connect APIs require an Android device/emulator rebuild after `expo prebuild`. On Windows / non-Android hosts the JS adapter reports unavailable and tests mock the native module. Native compile may be blocked on Windows without an Android SDK.

### Honest differences vs HealthKit (do not fake metric-for-metric parity)

| Concern | Apple HealthKit (C1) | Health Connect (C2) |
| --- | --- | --- |
| HRV | SDNN (often seconds → ms) | **RMSSD** milliseconds (`signalType: RMSSD`) |
| Sleep | Asleep stages only (skips INBED/AWAKE) | **SleepSession** interval duration |
| Activity steps | Daily step samples | Interval `Steps` records (may be finer-grained) |
| Workouts | HealthKit workouts | `ExerciseSession` (+ exercise type / title) |
| Permission visibility | Apple does not expose reliable per-type read status | HC returns granted permissions → honest partial grants |
| Availability | Device HealthKit capability | SDK may be missing / need Play install or update |
| History window | Client default 30d | Same ~30d default; longer history needs `READ_HEALTH_DATA_HISTORY` (not requested in C2) |

Both paths normalize to the same evidence-batch contract and reuse `evidenceUploadQueue`. Backend ingest is `OS_HUB_UPLOAD_ONLY` for both OS hubs.
