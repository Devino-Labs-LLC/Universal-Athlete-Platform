# Connected Apps (V5)

Athlete-facing health provider connections. Separate from Premium billing.

## C1 — Apple HealthKit library choice

**Library:** [`react-native-health`](https://github.com/agencyenterprise/react-native-health) (`^1.19`).

**Why:** Maintained HealthKit bridge with an Expo config plugin (`NSHealthShareUsageDescription`, HealthKit entitlement). Works with Expo SDK 57 **development builds** / EAS — not Expo Go. Read-only permissions for sleep, resting HR, HRV (SDNN), steps / active energy, and workouts.

**Android Health Connect** is Slice C2 — not implemented here.

Native HealthKit APIs require an iOS device or simulator rebuild after `expo prebuild`. On Windows CI / non-iOS hosts the JS adapter returns `isAvailable() === false` and tests mock the native module.
