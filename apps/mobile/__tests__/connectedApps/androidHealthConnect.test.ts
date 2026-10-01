import {
  HealthConnectSdkStatus,
  __setHealthConnectNativeModuleForTests,
  checkAvailability,
  isAvailable,
  openHealthConnectInstallOrSettings,
  requestAuthorization,
  readSamplesForLastNDays,
  windowForBackfillDays,
  type HealthConnectNativeModule,
} from '@/src/features/connectedApps/adapters/androidHealthConnect';
import { HEALTH_CONNECT_MAX_HISTORY_DAYS } from '@/src/features/connectedApps/constants';
import { Linking, Platform } from 'react-native';

function mockModule(
  overrides: Partial<HealthConnectNativeModule> = {},
): HealthConnectNativeModule {
  return {
    getSdkStatus: jest.fn().mockResolvedValue(HealthConnectSdkStatus.SDK_AVAILABLE),
    initialize: jest.fn().mockResolvedValue(true),
    requestPermission: jest.fn().mockResolvedValue([
      { accessType: 'read', recordType: 'SleepSession' },
      { accessType: 'read', recordType: 'Steps' },
    ]),
    getGrantedPermissions: jest.fn().mockResolvedValue([]),
    readRecords: jest.fn().mockResolvedValue({ records: [] }),
    openHealthConnectSettings: jest.fn(),
    ...overrides,
  };
}

describe('androidHealthConnect adapter', () => {
  const originalOs = Platform.OS;

  beforeEach(() => {
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => 'android' });
    __setHealthConnectNativeModuleForTests(null);
    jest.spyOn(Linking, 'openURL').mockResolvedValue(undefined as never);
  });

  afterEach(() => {
    __setHealthConnectNativeModuleForTests(null);
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => originalOs });
    jest.restoreAllMocks();
  });

  it('clamps backfill window to Health Connect history limit', () => {
    const now = new Date('2026-10-01T12:00:00.000Z');
    const window = windowForBackfillDays(90, now);
    const days =
      (window.endDate.getTime() - window.startDate.getTime()) / (24 * 60 * 60 * 1000);
    expect(days).toBe(HEALTH_CONNECT_MAX_HISTORY_DAYS);
  });

  it('reports not_android when Platform is not Android', async () => {
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => 'ios' });
    const availability = await checkAvailability();
    expect(availability).toMatchObject({ status: 'unavailable', reason: 'not_android' });
    expect(await isAvailable()).toBe(false);
  });

  it('reports module_missing when native module is null', async () => {
    __setHealthConnectNativeModuleForTests(null);
    const availability = await checkAvailability();
    expect(availability).toMatchObject({ status: 'unavailable', reason: 'module_missing' });
  });

  it('reports sdk_unavailable when Health Connect is not installed', async () => {
    __setHealthConnectNativeModuleForTests(
      mockModule({
        getSdkStatus: jest
          .fn()
          .mockResolvedValue(HealthConnectSdkStatus.SDK_UNAVAILABLE),
      }),
    );
    const availability = await checkAvailability();
    expect(availability).toMatchObject({
      status: 'unavailable',
      reason: 'sdk_unavailable',
    });
  });

  it('reports update_required when provider update is needed', async () => {
    __setHealthConnectNativeModuleForTests(
      mockModule({
        getSdkStatus: jest
          .fn()
          .mockResolvedValue(HealthConnectSdkStatus.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED),
      }),
    );
    const availability = await checkAvailability();
    expect(availability).toMatchObject({
      status: 'unavailable',
      reason: 'update_required',
    });
  });

  it('reports init_failed when initialize returns false', async () => {
    __setHealthConnectNativeModuleForTests(
      mockModule({
        initialize: jest.fn().mockResolvedValue(false),
      }),
    );
    const availability = await checkAvailability();
    expect(availability).toMatchObject({ status: 'unavailable', reason: 'init_failed' });
  });

  it('reports available when SDK is ready', async () => {
    __setHealthConnectNativeModuleForTests(mockModule());
    expect(await checkAvailability()).toEqual({ status: 'available' });
    expect(await isAvailable()).toBe(true);
  });

  it('detects partial permission grants', async () => {
    __setHealthConnectNativeModuleForTests(mockModule());
    const auth = await requestAuthorization();
    expect(auth.granted).toBe(true);
    expect(auth.partial).toBe(true);
    expect(auth.grantedRecordTypes).toEqual(['SleepSession', 'Steps']);
    expect(auth.deniedRecordTypes).toEqual(
      expect.arrayContaining(['RestingHeartRate', 'ExerciseSession']),
    );
  });

  it('rejects authorization when no read permissions granted', async () => {
    __setHealthConnectNativeModuleForTests(
      mockModule({
        requestPermission: jest.fn().mockResolvedValue([]),
      }),
    );
    const auth = await requestAuthorization();
    expect(auth.granted).toBe(false);
    expect(auth.dialogCompleted).toBe(true);
    expect(auth.message).toMatch(/permission was not granted/i);
  });

  it('surfaces requestPermission exceptions', async () => {
    __setHealthConnectNativeModuleForTests(
      mockModule({
        requestPermission: jest.fn().mockRejectedValue(new Error('permission dialog failed')),
      }),
    );
    const auth = await requestAuthorization();
    expect(auth.granted).toBe(false);
    expect(auth.message).toMatch(/permission dialog failed/i);
  });

  it('opens Play Store for sdk_unavailable and settings otherwise', async () => {
    const openHealthConnectSettings = jest.fn();
    __setHealthConnectNativeModuleForTests(mockModule({ openHealthConnectSettings }));

    await openHealthConnectInstallOrSettings('sdk_unavailable');
    expect(Linking.openURL).toHaveBeenCalledWith(
      expect.stringContaining('market://details'),
    );

    await openHealthConnectInstallOrSettings('permissions');
    expect(openHealthConnectSettings).toHaveBeenCalled();
  });

  it('reads and normalizes granted families only', async () => {
    const readRecords = jest.fn(async (recordType: string) => {
      if (recordType === 'SleepSession') {
        return {
          records: [
            {
              startTime: '2026-09-29T02:00:00.000Z',
              endTime: '2026-09-29T08:00:00.000Z',
              metadata: { id: 'sleep-1', dataOrigin: 'com.example.sleep' },
            },
          ],
        };
      }
      if (recordType === 'HeartRateVariabilityRmssd') {
        return {
          records: [
            {
              time: '2026-09-29T06:00:00.000Z',
              heartRateVariabilityMillis: 42,
              metadata: { id: 'hrv-1' },
            },
          ],
        };
      }
      return { records: [] };
    });

    __setHealthConnectNativeModuleForTests(mockModule({ readRecords }));

    const result = await readSamplesForLastNDays(7, [
      'SleepSession',
      'HeartRateVariabilityRmssd',
    ]);

    expect(result.items).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          externalRecordId: 'sleep-1',
          signalFamily: 'SLEEP',
          valueNumeric: 360,
        }),
        expect.objectContaining({
          externalRecordId: 'hrv-1',
          signalFamily: 'HRV',
          signalType: 'RMSSD',
          valueNumeric: 42,
        }),
      ]),
    );
    expect(readRecords).not.toHaveBeenCalledWith('Steps', expect.anything());
  });

  it('marks families when readRecords throws', async () => {
    __setHealthConnectNativeModuleForTests(
      mockModule({
        readRecords: jest.fn().mockRejectedValue(new Error('read failed')),
      }),
    );
    const result = await readSamplesForLastNDays(7);
    expect(result.items).toEqual([]);
    expect(result.partial).toBe(true);
    expect(result.deniedOrEmptyFamilies.length).toBeGreaterThan(0);
    expect(result.message).toMatch(/No readable Health Connect samples/i);
  });

  it('returns unavailable read result off Android', async () => {
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => 'ios' });
    const result = await readSamplesForLastNDays(7);
    expect(result.partial).toBe(true);
    expect(result.message).toMatch(/not available/i);
  });
});
