/**
 * Jest stub — CI has no HealthKit native module.
 * Production uses react-native-health via the iosHealthKit adapter (lazy require).
 */
module.exports = {
  __esModule: true,
  default: {
    isAvailable: (cb) => cb(null, false),
    initHealthKit: (_perms, cb) => cb('HealthKit unavailable in Jest'),
    getSleepSamples: (_opts, cb) => cb(null, []),
    getRestingHeartRateSamples: (_opts, cb) => cb(null, []),
    getHeartRateVariabilitySamples: (_opts, cb) => cb(null, []),
    getDailyStepCountSamples: (_opts, cb) => cb(null, []),
    getActiveEnergyBurned: (_opts, cb) => cb(null, []),
    getAnchoredWorkouts: (_opts, cb) => cb(null, { data: [] }),
    Constants: {
      Permissions: {
        SleepAnalysis: 'SleepAnalysis',
        RestingHeartRate: 'RestingHeartRate',
        HeartRateVariability: 'HeartRateVariability',
        StepCount: 'StepCount',
        Steps: 'Steps',
        ActiveEnergyBurned: 'ActiveEnergyBurned',
        Workout: 'Workout',
      },
      Units: { Second: 's' },
    },
  },
};
