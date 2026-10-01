import { Linking, Platform } from 'react-native';

import {
  normalizeHealthConnectSamples,
  type HealthConnectRawSample,
} from '@/src/features/connectedApps/adapters/normalizeHealthConnect';
import {
  DEFAULT_BACKFILL_DAYS,
  HEALTH_CONNECT_MAX_HISTORY_DAYS,
  HEALTH_CONNECT_PROVIDER_PACKAGE,
} from '@/src/features/connectedApps/constants';
import type { EvidenceBatchItem } from '@/src/features/connectedApps/models/evidenceBatch';

/** Matches react-native-health-connect SdkAvailabilityStatus. */
export const HealthConnectSdkStatus = {
  SDK_UNAVAILABLE: 1,
  SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED: 2,
  SDK_AVAILABLE: 3,
} as const;

export type HealthConnectPermission = {
  accessType: 'read' | 'write';
  recordType: string;
};

export type HealthConnectReadOptions = {
  timeRangeFilter: {
    operator: 'between';
    startTime: string;
    endTime: string;
  };
};

/**
 * Minimal surface of react-native-health-connect used by this adapter.
 * Kept local so tests can inject mocks without loading the native module.
 */
export type HealthConnectNativeModule = {
  getSdkStatus: (providerPackageName?: string) => Promise<number>;
  initialize: (providerPackageName?: string) => Promise<boolean>;
  requestPermission: (permissions: HealthConnectPermission[]) => Promise<HealthConnectPermission[]>;
  getGrantedPermissions: () => Promise<HealthConnectPermission[]>;
  readRecords: (
    recordType: string,
    options: HealthConnectReadOptions,
  ) => Promise<{ records: Record<string, unknown>[]; pageToken?: string }>;
  openHealthConnectSettings: () => void;
};

export type HealthConnectAvailability =
  | { status: 'available' }
  | { status: 'unavailable'; reason: 'not_android' | 'module_missing' | 'sdk_unavailable' | 'update_required' | 'init_failed'; message: string };

export type HealthConnectAuthorizationResult = {
  granted: boolean;
  /** True when at least one requested read permission was granted. */
  partial: boolean;
  grantedRecordTypes: string[];
  deniedRecordTypes: string[];
  dialogCompleted: boolean;
  message?: string;
};

export type HealthConnectReadWindow = {
  startDate: Date;
  endDate: Date;
};

export type HealthConnectReadResult = {
  items: EvidenceBatchItem[];
  rawCount: number;
  partial: boolean;
  deniedOrEmptyFamilies: string[];
  message?: string;
};

/** READ-only approved record types — do not request write or unrelated health data. */
export const HEALTH_CONNECT_READ_RECORD_TYPES = [
  'SleepSession',
  'RestingHeartRate',
  'HeartRateVariabilityRmssd',
  'Steps',
  'ActiveCaloriesBurned',
  'ExerciseSession',
] as const;

export type HealthConnectReadRecordType = (typeof HEALTH_CONNECT_READ_RECORD_TYPES)[number];

const RECORD_TYPE_TO_FAMILY: Record<HealthConnectReadRecordType, string> = {
  SleepSession: 'SLEEP',
  RestingHeartRate: 'HEART',
  HeartRateVariabilityRmssd: 'HRV',
  Steps: 'ACTIVITY',
  ActiveCaloriesBurned: 'ACTIVITY',
  ExerciseSession: 'WORKOUT',
};

/** Common ExerciseType constants → display names (subset; unknown falls back to WORKOUT). */
const EXERCISE_TYPE_NAMES: Record<number, string> = {
  0: 'OTHER_WORKOUT',
  8: 'BIKING',
  9: 'BIKING_STATIONARY',
  16: 'DANCING',
  25: 'ELLIPTICAL',
  36: 'HIGH_INTENSITY_INTERVAL_TRAINING',
  37: 'HIKING',
  48: 'PILATES',
  53: 'ROWING',
  54: 'ROWING_MACHINE',
  56: 'RUNNING',
  57: 'RUNNING_TREADMILL',
  70: 'STRENGTH_TRAINING',
  71: 'STRETCHING',
  73: 'SWIMMING_OPEN_WATER',
  74: 'SWIMMING_POOL',
  79: 'WALKING',
  81: 'WEIGHTLIFTING',
  83: 'YOGA',
};

let injectedModule: HealthConnectNativeModule | null | undefined;

export function __setHealthConnectNativeModuleForTests(
  module: HealthConnectNativeModule | null,
): void {
  injectedModule = module;
}

function loadNativeModule(): HealthConnectNativeModule | null {
  if (injectedModule !== undefined) {
    return injectedModule;
  }
  if (Platform.OS !== 'android') {
    return null;
  }
  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const mod = require('react-native-health-connect') as HealthConnectNativeModule;
    return mod;
  } catch {
    return null;
  }
}

export function healthConnectReadPermissions(): HealthConnectPermission[] {
  return HEALTH_CONNECT_READ_RECORD_TYPES.map((recordType) => ({
    accessType: 'read' as const,
    recordType,
  }));
}

/**
 * Health Connect history beyond ~30 days needs READ_HEALTH_DATA_HISTORY.
 * C2 stays within that limit — clamp even if callers pass a larger window.
 */
export function windowForBackfillDays(
  days: number = DEFAULT_BACKFILL_DAYS,
  now: Date = new Date(),
): HealthConnectReadWindow {
  const safeDays = Math.min(
    HEALTH_CONNECT_MAX_HISTORY_DAYS,
    Math.max(1, Math.floor(days)),
  );
  const endDate = now;
  const startDate = new Date(now.getTime() - safeDays * 24 * 60 * 60 * 1000);
  return { startDate, endDate };
}

function metadataId(record: Record<string, unknown>): string | null {
  const metadata = record.metadata as { id?: unknown } | undefined;
  return typeof metadata?.id === 'string' ? metadata.id : null;
}

function dataOrigin(record: Record<string, unknown>): string | null {
  const metadata = record.metadata as { dataOrigin?: unknown } | undefined;
  return typeof metadata?.dataOrigin === 'string' ? metadata.dataOrigin : null;
}

function exerciseActivityName(record: Record<string, unknown>): string {
  if (typeof record.title === 'string' && record.title.trim().length > 0) {
    return record.title.trim();
  }
  const type = typeof record.exerciseType === 'number' ? record.exerciseType : 0;
  return EXERCISE_TYPE_NAMES[type] ?? 'WORKOUT';
}

function mapSleepSessions(records: Record<string, unknown>[]): HealthConnectRawSample[] {
  return records.map((record) => ({
    kind: 'sleepSession' as const,
    id: metadataId(record),
    startDate: String(record.startTime ?? ''),
    endDate: String(record.endTime ?? record.startTime ?? ''),
    sourceName: dataOrigin(record),
  }));
}

function mapRestingHeartRate(records: Record<string, unknown>[]): HealthConnectRawSample[] {
  return records.map((record) => {
    const time = String(record.time ?? '');
    return {
      kind: 'restingHeartRate' as const,
      id: metadataId(record),
      startDate: time,
      endDate: time,
      value: typeof record.beatsPerMinute === 'number' ? record.beatsPerMinute : null,
      sourceName: dataOrigin(record),
    };
  });
}

function mapHrvRmssd(records: Record<string, unknown>[]): HealthConnectRawSample[] {
  return records.map((record) => {
    const time = String(record.time ?? '');
    return {
      kind: 'hrvRmssd' as const,
      id: metadataId(record),
      startDate: time,
      endDate: time,
      value:
        typeof record.heartRateVariabilityMillis === 'number'
          ? record.heartRateVariabilityMillis
          : null,
      sourceName: dataOrigin(record),
    };
  });
}

function mapSteps(records: Record<string, unknown>[]): HealthConnectRawSample[] {
  return records.map((record) => ({
    kind: 'steps' as const,
    id: metadataId(record),
    startDate: String(record.startTime ?? ''),
    endDate: String(record.endTime ?? record.startTime ?? ''),
    value: typeof record.count === 'number' ? record.count : null,
    sourceName: dataOrigin(record),
  }));
}

function mapActiveEnergy(records: Record<string, unknown>[]): HealthConnectRawSample[] {
  return records.map((record) => {
    const energy = record.energy as { inKilocalories?: number } | undefined;
    return {
      kind: 'activeEnergy' as const,
      id: metadataId(record),
      startDate: String(record.startTime ?? ''),
      endDate: String(record.endTime ?? record.startTime ?? ''),
      value: typeof energy?.inKilocalories === 'number' ? energy.inKilocalories : null,
      sourceName: dataOrigin(record),
    };
  });
}

function mapExercises(records: Record<string, unknown>[]): HealthConnectRawSample[] {
  return records.map((record) => ({
    kind: 'exerciseSession' as const,
    id: metadataId(record),
    startDate: String(record.startTime ?? ''),
    endDate: String(record.endTime ?? record.startTime ?? ''),
    activityName: exerciseActivityName(record),
    sourceName: dataOrigin(record),
  }));
}

export async function checkAvailability(): Promise<HealthConnectAvailability> {
  if (Platform.OS !== 'android') {
    return {
      status: 'unavailable',
      reason: 'not_android',
      message: 'Health Connect is only available on Android.',
    };
  }
  const hc = loadNativeModule();
  if (!hc) {
    return {
      status: 'unavailable',
      reason: 'module_missing',
      message:
        'Health Connect native module is not available in this build. Use an Android development build with Health Connect enabled.',
    };
  }

  try {
    const sdkStatus = await hc.getSdkStatus(HEALTH_CONNECT_PROVIDER_PACKAGE);
    if (sdkStatus === HealthConnectSdkStatus.SDK_UNAVAILABLE) {
      return {
        status: 'unavailable',
        reason: 'sdk_unavailable',
        message:
          'Health Connect is not installed on this device. Install Health Connect from the Play Store, then try again.',
      };
    }
    if (sdkStatus === HealthConnectSdkStatus.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
      return {
        status: 'unavailable',
        reason: 'update_required',
        message:
          'Health Connect needs an update before Athlete Readiness can connect. Open Health Connect or the Play Store to update, then try again.',
      };
    }
    if (sdkStatus !== HealthConnectSdkStatus.SDK_AVAILABLE) {
      return {
        status: 'unavailable',
        reason: 'sdk_unavailable',
        message: 'Health Connect is not available on this device.',
      };
    }

    const initialized = await hc.initialize(HEALTH_CONNECT_PROVIDER_PACKAGE);
    if (!initialized) {
      return {
        status: 'unavailable',
        reason: 'init_failed',
        message: 'Health Connect could not be initialized on this device.',
      };
    }
    return { status: 'available' };
  } catch {
    return {
      status: 'unavailable',
      reason: 'init_failed',
      message: 'Health Connect could not be reached on this device.',
    };
  }
}

export async function isAvailable(): Promise<boolean> {
  const availability = await checkAvailability();
  return availability.status === 'available';
}

/**
 * Opens Play Store listing (install) or Health Connect settings (update / manage).
 * Best-effort — failures are non-fatal.
 */
export async function openHealthConnectInstallOrSettings(
  reason: 'sdk_unavailable' | 'update_required' | 'permissions' = 'permissions',
): Promise<void> {
  const hc = loadNativeModule();
  if (reason === 'sdk_unavailable') {
    try {
      await Linking.openURL(`market://details?id=${HEALTH_CONNECT_PROVIDER_PACKAGE}`);
      return;
    } catch {
      try {
        await Linking.openURL(
          `https://play.google.com/store/apps/details?id=${HEALTH_CONNECT_PROVIDER_PACKAGE}`,
        );
        return;
      } catch {
        // fall through to settings if available
      }
    }
  }
  try {
    hc?.openHealthConnectSettings();
  } catch {
    // ignore — UX already explained the action
  }
}

/**
 * Requests READ-only authorization for the approved type set.
 * Unlike HealthKit, Health Connect returns granted permissions so partial grants are honest.
 */
export async function requestAuthorization(): Promise<HealthConnectAuthorizationResult> {
  const availability = await checkAvailability();
  if (availability.status !== 'available') {
    return {
      granted: false,
      partial: false,
      grantedRecordTypes: [],
      deniedRecordTypes: [...HEALTH_CONNECT_READ_RECORD_TYPES],
      dialogCompleted: false,
      message: availability.message,
    };
  }

  const hc = loadNativeModule();
  if (!hc) {
    return {
      granted: false,
      partial: false,
      grantedRecordTypes: [],
      deniedRecordTypes: [...HEALTH_CONNECT_READ_RECORD_TYPES],
      dialogCompleted: false,
      message:
        'Health Connect native module is not available in this build. Use an Android development build with Health Connect enabled.',
    };
  }

  try {
    const requested = healthConnectReadPermissions();
    const granted = await hc.requestPermission(requested);
    const grantedReadTypes = new Set(
      granted
        .filter((p) => p.accessType === 'read' && typeof p.recordType === 'string')
        .map((p) => p.recordType),
    );
    const grantedRecordTypes = HEALTH_CONNECT_READ_RECORD_TYPES.filter((t) =>
      grantedReadTypes.has(t),
    );
    const deniedRecordTypes = HEALTH_CONNECT_READ_RECORD_TYPES.filter(
      (t) => !grantedReadTypes.has(t),
    );
    const partial =
      grantedRecordTypes.length > 0 && deniedRecordTypes.length > 0;

    if (grantedRecordTypes.length === 0) {
      return {
        granted: false,
        partial: false,
        grantedRecordTypes,
        deniedRecordTypes,
        dialogCompleted: true,
        message:
          'Health Connect permission was not granted. Enable Sleep, Resting heart rate, HRV, Steps, Active calories, and Exercise for Athlete Readiness in Health Connect, then try again.',
      };
    }

    return {
      granted: true,
      partial,
      grantedRecordTypes: [...grantedRecordTypes],
      deniedRecordTypes: [...deniedRecordTypes],
      dialogCompleted: true,
      message: partial
        ? 'Some Health Connect types were not granted. You can enable missing types in Health Connect settings.'
        : undefined,
    };
  } catch (error) {
    return {
      granted: false,
      partial: false,
      grantedRecordTypes: [],
      deniedRecordTypes: [...HEALTH_CONNECT_READ_RECORD_TYPES],
      dialogCompleted: true,
      message:
        error instanceof Error
          ? error.message
          : 'Health Connect permission request failed.',
    };
  }
}

async function readRecordFamily(
  hc: HealthConnectNativeModule,
  recordType: HealthConnectReadRecordType,
  options: HealthConnectReadOptions,
  mapper: (records: Record<string, unknown>[]) => HealthConnectRawSample[],
): Promise<{ samples: HealthConnectRawSample[]; error?: string }> {
  try {
    const result = await hc.readRecords(recordType, options);
    const records = Array.isArray(result?.records) ? result.records : [];
    return { samples: mapper(records) };
  } catch (error) {
    return {
      samples: [],
      error: error instanceof Error ? error.message : 'query_failed',
    };
  }
}

export async function readSamplesForWindow(
  window: HealthConnectReadWindow = windowForBackfillDays(),
  grantedRecordTypes?: readonly string[],
): Promise<HealthConnectReadResult> {
  const hc = loadNativeModule();
  if (!hc || Platform.OS !== 'android') {
    return {
      items: [],
      rawCount: 0,
      partial: true,
      deniedOrEmptyFamilies: ['SLEEP', 'HEART', 'HRV', 'ACTIVITY', 'WORKOUT'],
      message: 'Health Connect is not available on this platform/build.',
    };
  }

  const allowed = new Set(
    grantedRecordTypes && grantedRecordTypes.length > 0
      ? grantedRecordTypes
      : HEALTH_CONNECT_READ_RECORD_TYPES,
  );

  const options: HealthConnectReadOptions = {
    timeRangeFilter: {
      operator: 'between',
      startTime: window.startDate.toISOString(),
      endTime: window.endDate.toISOString(),
    },
  };

  const raw: HealthConnectRawSample[] = [];
  const deniedOrEmptyFamilies: string[] = [];
  let queryFailures = 0;

  const readers: {
    recordType: HealthConnectReadRecordType;
    mapper: (records: Record<string, unknown>[]) => HealthConnectRawSample[];
  }[] = [
    { recordType: 'SleepSession', mapper: mapSleepSessions },
    { recordType: 'RestingHeartRate', mapper: mapRestingHeartRate },
    { recordType: 'HeartRateVariabilityRmssd', mapper: mapHrvRmssd },
    { recordType: 'Steps', mapper: mapSteps },
    { recordType: 'ActiveCaloriesBurned', mapper: mapActiveEnergy },
    { recordType: 'ExerciseSession', mapper: mapExercises },
  ];

  const familySampleCounts = new Map<string, number>();

  for (const reader of readers) {
    const family = RECORD_TYPE_TO_FAMILY[reader.recordType];
    if (!allowed.has(reader.recordType)) {
      deniedOrEmptyFamilies.push(family);
      continue;
    }
    const result = await readRecordFamily(hc, reader.recordType, options, reader.mapper);
    if (result.error) {
      queryFailures += 1;
      deniedOrEmptyFamilies.push(family);
      continue;
    }
    raw.push(...result.samples);
    familySampleCounts.set(family, (familySampleCounts.get(family) ?? 0) + result.samples.length);
  }

  for (const [family, count] of familySampleCounts) {
    if (count === 0) {
      deniedOrEmptyFamilies.push(family);
    }
  }

  // ACTIVITY is empty only when both Steps and ActiveCaloriesBurned contributed nothing
  // and neither failed as a hard deny already covered above.
  const uniqueDenied = [...new Set(deniedOrEmptyFamilies)];
  const items = normalizeHealthConnectSamples(raw);
  const partial = queryFailures > 0 || uniqueDenied.length > 0;

  let message: string | undefined;
  if (items.length === 0) {
    message =
      'No readable Health Connect samples were returned for the selected types. If you expected data, enable Sleep, Resting heart rate, HRV, Steps, Active calories, and Exercise for Athlete Readiness in Health Connect.';
  } else if (partial) {
    message =
      'Some Health Connect types returned no data. Enable missing types in Health Connect settings if you expected them.';
  }

  return {
    items,
    rawCount: raw.length,
    partial,
    deniedOrEmptyFamilies: uniqueDenied,
    message,
  };
}

export async function readSamplesForLastNDays(
  days: number = DEFAULT_BACKFILL_DAYS,
  grantedRecordTypes?: readonly string[],
): Promise<HealthConnectReadResult> {
  return readSamplesForWindow(windowForBackfillDays(days), grantedRecordTypes);
}
