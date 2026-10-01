import {
  __setHealthKitNativeModuleForTests,
  healthKitReadPermissions,
  isAvailable,
  readSamplesForLastNDays,
  requestAuthorization,
  windowForBackfillDays,
  type HealthKitNativeModule,
} from '@/src/features/connectedApps/adapters/iosHealthKit';
import { Platform } from 'react-native';

function mockKit(overrides: Partial<HealthKitNativeModule> = {}): HealthKitNativeModule {
  return {
    isAvailable: jest.fn((cb) => cb(null, true)),
    initHealthKit: jest.fn((_perms, cb) => cb(null as unknown as string)),
    getSleepSamples: jest.fn((_opts, cb) => cb(null, [])),
    getRestingHeartRateSamples: jest.fn((_opts, cb) => cb(null, [])),
    getHeartRateVariabilitySamples: jest.fn((_opts, cb) => cb(null, [])),
    getDailyStepCountSamples: jest.fn((_opts, cb) => cb(null, [])),
    getActiveEnergyBurned: jest.fn((_opts, cb) => cb(null, [])),
    getAnchoredWorkouts: jest.fn((_opts, cb) => cb(null, { data: [] })),
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
    ...overrides,
  };
}

describe('iosHealthKit adapter', () => {
  const originalOs = Platform.OS;

  beforeEach(() => {
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => 'ios' });
    __setHealthKitNativeModuleForTests(null);
  });

  afterEach(() => {
    __setHealthKitNativeModuleForTests(null);
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => originalOs });
  });

  it('builds a backfill window of N days', () => {
    const now = new Date('2026-10-01T12:00:00.000Z');
    const window = windowForBackfillDays(7, now);
    expect(window.endDate).toBe(now);
    expect(window.startDate.toISOString()).toBe('2026-09-24T12:00:00.000Z');
  });

  it('returns false for isAvailable on non-iOS', async () => {
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => 'android' });
    expect(await isAvailable()).toBe(false);
  });

  it('returns false when native module is missing', async () => {
    __setHealthKitNativeModuleForTests(null);
    expect(await isAvailable()).toBe(false);
  });

  it('reports available when HealthKit isAvailable succeeds', async () => {
    __setHealthKitNativeModuleForTests(mockKit());
    expect(await isAvailable()).toBe(true);
  });

  it('treats isAvailable callback errors as unavailable', async () => {
    __setHealthKitNativeModuleForTests(
      mockKit({
        isAvailable: jest.fn((cb) => cb(new Error('unavailable'), false)),
      }),
    );
    expect(await isAvailable()).toBe(false);
  });

  it('rejects authorization on non-iOS', async () => {
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => 'android' });
    const result = await requestAuthorization();
    expect(result.granted).toBe(false);
    expect(result.message).toMatch(/iPhone/i);
  });

  it('rejects authorization when module missing', async () => {
    __setHealthKitNativeModuleForTests(null);
    const result = await requestAuthorization();
    expect(result.granted).toBe(false);
    expect(result.dialogCompleted).toBe(false);
    expect(result.message).toMatch(/native module/i);
  });

  it('rejects authorization when HealthKit unavailable on device', async () => {
    __setHealthKitNativeModuleForTests(
      mockKit({
        isAvailable: jest.fn((cb) => cb(null, false)),
      }),
    );
    const result = await requestAuthorization();
    expect(result.granted).toBe(false);
    expect(result.message).toMatch(/not available/i);
  });

  it('grants authorization after successful initHealthKit', async () => {
    const kit = mockKit();
    __setHealthKitNativeModuleForTests(kit);
    const result = await requestAuthorization();
    expect(result).toEqual({ granted: true, dialogCompleted: true });
    expect(kit.initHealthKit).toHaveBeenCalledWith(
      {
        permissions: {
          read: expect.arrayContaining(['SleepAnalysis', 'Workout']),
          write: [],
        },
      },
      expect.any(Function),
    );
  });

  it('surfaces initHealthKit errors as denied auth', async () => {
    __setHealthKitNativeModuleForTests(
      mockKit({
        initHealthKit: jest.fn((_perms, cb) => cb('permission denied')),
      }),
    );
    const result = await requestAuthorization();
    expect(result.granted).toBe(false);
    expect(result.dialogCompleted).toBe(true);
    expect(result.message).toMatch(/permission denied/i);
  });

  it('lists READ permissions from Constants', () => {
    const kit = mockKit();
    expect(healthKitReadPermissions(kit)).toEqual(
      expect.arrayContaining(['SleepAnalysis', 'StepCount', 'Workout']),
    );
  });

  it('returns unavailable read result when not on iOS', async () => {
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => 'android' });
    const result = await readSamplesForLastNDays(7);
    expect(result.items).toEqual([]);
    expect(result.partial).toBe(true);
    expect(result.message).toMatch(/not available/i);
  });

  it('reads and normalizes samples across families', async () => {
    __setHealthKitNativeModuleForTests(
      mockKit({
        getSleepSamples: jest.fn((_opts, cb) =>
          cb(null, [
            {
              id: 'sleep-1',
              startDate: '2026-09-29T02:00:00.000Z',
              endDate: '2026-09-29T08:00:00.000Z',
              value: 'ASLEEP',
              sourceName: 'Watch',
            },
          ]),
        ),
        getRestingHeartRateSamples: jest.fn((_opts, cb) =>
          cb(null, [
            {
              id: 'rhr-1',
              startDate: '2026-09-29T06:00:00.000Z',
              endDate: '2026-09-29T06:00:00.000Z',
              value: 52,
              unit: 'count/min',
            },
          ]),
        ),
        getHeartRateVariabilitySamples: jest.fn((_opts, cb) =>
          cb(null, [
            {
              id: 'hrv-1',
              startDate: '2026-09-29T06:05:00.000Z',
              endDate: '2026-09-29T06:05:00.000Z',
              value: 0.042,
              unit: 's',
            },
          ]),
        ),
        getDailyStepCountSamples: jest.fn((_opts, cb) =>
          cb(null, [
            {
              id: 'steps-1',
              startDate: '2026-09-29T00:00:00.000Z',
              endDate: '2026-09-30T00:00:00.000Z',
              value: 8000,
            },
          ]),
        ),
        getActiveEnergyBurned: jest.fn((_opts, cb) =>
          cb(null, [
            {
              id: 'kcal-1',
              startDate: '2026-09-29T00:00:00.000Z',
              endDate: '2026-09-30T00:00:00.000Z',
              value: 450,
            },
          ]),
        ),
        getAnchoredWorkouts: jest.fn((_opts, cb) =>
          cb(null, {
            data: [
              {
                id: 'wo-1',
                start: '2026-09-29T10:00:00.000Z',
                end: '2026-09-29T11:00:00.000Z',
                activityName: 'Running',
                calories: 500,
                duration: 3600,
              },
            ],
          }),
        ),
      }),
    );

    const result = await readSamplesForLastNDays(7);
    expect(result.rawCount).toBeGreaterThanOrEqual(6);
    expect(result.items.length).toBeGreaterThan(0);
    expect(result.items).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ externalRecordId: 'sleep-1', signalFamily: 'SLEEP' }),
        expect.objectContaining({ externalRecordId: 'rhr-1', signalFamily: 'HEART' }),
        expect.objectContaining({ externalRecordId: 'wo-1', signalFamily: 'WORKOUT' }),
      ]),
    );
  });

  it('marks families denied when queries fail', async () => {
    __setHealthKitNativeModuleForTests(
      mockKit({
        getSleepSamples: jest.fn((_opts, cb) => cb('denied', [])),
        getRestingHeartRateSamples: jest.fn((_opts, cb) => cb('denied', [])),
        getHeartRateVariabilitySamples: jest.fn((_opts, cb) => cb('denied', [])),
        getDailyStepCountSamples: jest.fn((_opts, cb) => cb('denied', [])),
        getActiveEnergyBurned: jest.fn((_opts, cb) => cb('denied', [])),
        getAnchoredWorkouts: jest.fn((_opts, cb) => cb({ message: 'denied' }, { data: [] })),
      }),
    );

    const result = await readSamplesForLastNDays(7);
    expect(result.items).toEqual([]);
    expect(result.partial).toBe(true);
    expect(result.deniedOrEmptyFamilies).toEqual(
      expect.arrayContaining(['SLEEP', 'HEART', 'HRV', 'ACTIVITY', 'WORKOUT']),
    );
    expect(result.message).toMatch(/No readable Apple Health samples/i);
  });

  it('handles thrown query runners without crashing', async () => {
    __setHealthKitNativeModuleForTests(
      mockKit({
        getSleepSamples: jest.fn(() => {
          throw new Error('native crash');
        }),
        isAvailable: jest.fn((cb) => cb(null, true)),
      }),
    );
    const result = await readSamplesForLastNDays(3);
    expect(result.deniedOrEmptyFamilies).toContain('SLEEP');
    expect(result.partial).toBe(true);
  });
});
