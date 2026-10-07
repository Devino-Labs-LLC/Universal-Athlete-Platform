import { NativeModules, Platform } from 'react-native';

import { DEFAULT_BACKFILL_DAYS } from '@/src/features/connectedApps/constants';
import {
  normalizeHealthKitSamples,
  type HealthKitRawSample,
} from '@/src/features/connectedApps/adapters/normalizeHealthKit';
import type { EvidenceBatchItem } from '@/src/features/connectedApps/models/evidenceBatch';

/**
 * Minimal surface of react-native-health used by this adapter.
 * Kept local so tests can inject mocks without loading the native module.
 */
export type HealthKitNativeModule = {
  isAvailable: (callback: (error: unknown, results: boolean) => void) => void;
  initHealthKit: (
    permissions: { permissions: { read: string[]; write: string[] } },
    callback: (error: string, result?: unknown) => void,
  ) => void;
  getSleepSamples: (
    options: { startDate: string; endDate: string },
    callback: (error: unknown, results: Record<string, unknown>[]) => void,
  ) => void;
  getRestingHeartRateSamples: (
    options: { startDate: string; endDate: string },
    callback: (error: unknown, results: Record<string, unknown>[]) => void,
  ) => void;
  getHeartRateVariabilitySamples: (
    options: { startDate: string; endDate: string; unit?: string },
    callback: (error: unknown, results: Record<string, unknown>[]) => void,
  ) => void;
  getDailyStepCountSamples: (
    options: { startDate: string; endDate: string },
    callback: (error: unknown, results: Record<string, unknown>[]) => void,
  ) => void;
  getActiveEnergyBurned: (
    options: { startDate: string; endDate: string },
    callback: (error: unknown, results: Record<string, unknown>[]) => void,
  ) => void;
  getAnchoredWorkouts: (
    options: { startDate: string; endDate: string },
    callback: (error: { message?: string } | null, results: { data?: Record<string, unknown>[] }) => void,
  ) => void;
  Constants: {
    Permissions: Record<string, string>;
    Units?: Record<string, string>;
  };
};

export type HealthKitAuthorizationResult = {
  granted: boolean;
  /** True when the OS dialog completed without a hard failure; read grants are not inspectable. */
  dialogCompleted: boolean;
  message?: string;
};

export type HealthKitReadWindow = {
  startDate: Date;
  endDate: Date;
};

export type HealthKitReadResult = {
  items: EvidenceBatchItem[];
  rawCount: number;
  partial: boolean;
  deniedOrEmptyFamilies: string[];
  message?: string;
};

let injectedModule: HealthKitNativeModule | null | undefined;

export function __setHealthKitNativeModuleForTests(
  module: HealthKitNativeModule | null | undefined,
): void {
  injectedModule = module;
}

type HealthKitPackageExport = {
  default?: Partial<HealthKitNativeModule>;
  Constants?: HealthKitNativeModule['Constants'];
} & Partial<HealthKitNativeModule>;

function readHealthKitPackage(): HealthKitPackageExport | null {
  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    return require('react-native-health') as HealthKitPackageExport;
  } catch {
    return null;
  }
}

function packageConstants(exported: HealthKitPackageExport | null): HealthKitNativeModule['Constants'] | null {
  return exported?.Constants ?? exported?.default?.Constants ?? null;
}

/**
 * react-native-health copies NativeModules.AppleHealthKit with Object.assign at import time.
 * On the New Architecture those methods are non-enumerable, so the copy only keeps Constants
 * and isAvailable is missing. Call the live native module and attach Constants from the package.
 */
function loadNativeModule(): HealthKitNativeModule | null {
  if (injectedModule !== undefined) {
    return injectedModule;
  }
  if (Platform.OS !== 'ios') {
    return null;
  }
  const exported = readHealthKitPackage();
  const native = NativeModules.AppleHealthKit as HealthKitNativeModule | null | undefined;
  if (native && typeof native.isAvailable === 'function') {
    if (native.Constants?.Permissions) {
      return native;
    }
    const constants = packageConstants(exported);
    if (!constants?.Permissions) {
      return null;
    }
    return Object.assign(native, { Constants: constants });
  }
  const fallback = exported?.default ?? exported;
  if (fallback && typeof fallback.isAvailable === 'function' && fallback.Constants?.Permissions) {
    return fallback as HealthKitNativeModule;
  }
  return null;
}

/** READ-only approved types — do not request write or unrelated health data. */
export function healthKitReadPermissions(kit: HealthKitNativeModule): string[] {
  const p = kit.Constants.Permissions;
  return [
    p.SleepAnalysis,
    p.RestingHeartRate,
    p.HeartRateVariability,
    p.StepCount ?? p.Steps,
    p.ActiveEnergyBurned,
    p.Workout,
  ].filter((value): value is string => typeof value === 'string' && value.length > 0);
}

function promisifyAvailable(kit: HealthKitNativeModule): Promise<boolean> {
  return new Promise((resolve) => {
    try {
      kit.isAvailable((error, results) => {
        if (error) {
          resolve(false);
          return;
        }
        resolve(Boolean(results));
      });
    } catch {
      resolve(false);
    }
  });
}

function promisifyInit(
  kit: HealthKitNativeModule,
  permissions: { permissions: { read: string[]; write: string[] } },
): Promise<{ ok: boolean; error?: string }> {
  return new Promise((resolve) => {
    try {
      kit.initHealthKit(permissions, (error) => {
        if (error) {
          resolve({ ok: false, error: String(error) });
          return;
        }
        resolve({ ok: true });
      });
    } catch (error) {
      resolve({ ok: false, error: error instanceof Error ? error.message : String(error) });
    }
  });
}

function querySamples(
  runner: (callback: (error: unknown, results: Record<string, unknown>[]) => void) => void,
): Promise<{ samples: Record<string, unknown>[]; error?: string }> {
  return new Promise((resolve) => {
    try {
      runner((error, results) => {
        if (error) {
          resolve({ samples: [], error: typeof error === 'string' ? error : 'query_failed' });
          return;
        }
        resolve({ samples: Array.isArray(results) ? results : [] });
      });
    } catch (error) {
      resolve({ samples: [], error: error instanceof Error ? error.message : String(error) });
    }
  });
}

function mapQuantitySamples(
  kind: HealthKitRawSample['kind'],
  samples: Record<string, unknown>[],
): HealthKitRawSample[] {
  return samples.map((sample) => ({
    kind,
    id: typeof sample.id === 'string' ? sample.id : null,
    startDate: String(sample.startDate ?? ''),
    endDate: String(sample.endDate ?? sample.startDate ?? ''),
    value: sample.value as number | string | null | undefined,
    unit: typeof sample.unit === 'string' ? sample.unit : null,
    sourceName: typeof sample.sourceName === 'string' ? sample.sourceName : null,
  }));
}

function mapSleepSamples(samples: Record<string, unknown>[]): HealthKitRawSample[] {
  return samples.map((sample) => ({
    kind: 'sleep' as const,
    id: typeof sample.id === 'string' ? sample.id : null,
    startDate: String(sample.startDate ?? ''),
    endDate: String(sample.endDate ?? ''),
    value: sample.value as number | string | null | undefined,
    sourceName: typeof sample.sourceName === 'string' ? sample.sourceName : null,
  }));
}

function mapWorkouts(samples: Record<string, unknown>[]): HealthKitRawSample[] {
  return samples.map((sample) => ({
    kind: 'workout' as const,
    id: typeof sample.id === 'string' ? sample.id : null,
    startDate: String(sample.start ?? sample.startDate ?? ''),
    endDate: String(sample.end ?? sample.endDate ?? ''),
    activityName: typeof sample.activityName === 'string' ? sample.activityName : null,
    calories: typeof sample.calories === 'number' ? sample.calories : null,
    durationSeconds: typeof sample.duration === 'number' ? sample.duration : null,
    sourceName: typeof sample.sourceName === 'string' ? sample.sourceName : null,
  }));
}

export function windowForBackfillDays(
  days: number = DEFAULT_BACKFILL_DAYS,
  now: Date = new Date(),
): HealthKitReadWindow {
  const safeDays = Math.max(1, Math.floor(days));
  const endDate = now;
  const startDate = new Date(now.getTime() - safeDays * 24 * 60 * 60 * 1000);
  return { startDate, endDate };
}

export async function isAvailable(): Promise<boolean> {
  if (Platform.OS !== 'ios') {
    return false;
  }
  const kit = loadNativeModule();
  if (!kit) {
    return false;
  }
  return promisifyAvailable(kit);
}

/**
 * Requests READ-only authorization for the approved type set.
 * Apple does not expose reliable per-type read status; callers must treat empty reads honestly.
 */
export async function requestAuthorization(): Promise<HealthKitAuthorizationResult> {
  if (Platform.OS !== 'ios') {
    return {
      granted: false,
      dialogCompleted: false,
      message: 'Apple Health is only available on iPhone.',
    };
  }
  const kit = loadNativeModule();
  if (!kit) {
    return {
      granted: false,
      dialogCompleted: false,
      message:
        'Apple HealthKit native module is not available in this build. Use an iOS development build with HealthKit enabled.',
    };
  }

  const available = await promisifyAvailable(kit);
  if (!available) {
    return {
      granted: false,
      dialogCompleted: false,
      message: 'Apple Health is not available on this device.',
    };
  }

  const read = healthKitReadPermissions(kit);
  const result = await promisifyInit(kit, { permissions: { read, write: [] } });
  if (!result.ok) {
    return {
      granted: false,
      dialogCompleted: true,
      message:
        result.error ||
        'Apple Health permission was not granted. You can enable types in Settings → Health → Sharing.',
    };
  }
  return { granted: true, dialogCompleted: true };
}

export async function readSamplesForWindow(
  window: HealthKitReadWindow = windowForBackfillDays(),
): Promise<HealthKitReadResult> {
  const kit = loadNativeModule();
  if (!kit || Platform.OS !== 'ios') {
    return {
      items: [],
      rawCount: 0,
      partial: true,
      deniedOrEmptyFamilies: ['SLEEP', 'HEART', 'HRV', 'ACTIVITY', 'WORKOUT'],
      message: 'HealthKit is not available on this platform/build.',
    };
  }

  const options = {
    startDate: window.startDate.toISOString(),
    endDate: window.endDate.toISOString(),
  };

  const raw: HealthKitRawSample[] = [];
  const deniedOrEmptyFamilies: string[] = [];
  let queryFailures = 0;

  const sleep = await querySamples((cb) => kit.getSleepSamples(options, cb));
  if (sleep.error) {
    queryFailures += 1;
    deniedOrEmptyFamilies.push('SLEEP');
  } else {
    raw.push(...mapSleepSamples(sleep.samples));
    if (sleep.samples.length === 0) {
      deniedOrEmptyFamilies.push('SLEEP');
    }
  }

  const rhr = await querySamples((cb) => kit.getRestingHeartRateSamples(options, cb));
  if (rhr.error) {
    queryFailures += 1;
    deniedOrEmptyFamilies.push('HEART');
  } else {
    raw.push(...mapQuantitySamples('restingHeartRate', rhr.samples));
    if (rhr.samples.length === 0) {
      deniedOrEmptyFamilies.push('HEART');
    }
  }

  const hrv = await querySamples((cb) =>
    kit.getHeartRateVariabilitySamples(
      { ...options, unit: kit.Constants.Units?.Second ?? 's' },
      cb,
    ),
  );
  if (hrv.error) {
    queryFailures += 1;
    deniedOrEmptyFamilies.push('HRV');
  } else {
    raw.push(...mapQuantitySamples('hrvSdnn', hrv.samples));
    if (hrv.samples.length === 0) {
      deniedOrEmptyFamilies.push('HRV');
    }
  }

  const steps = await querySamples((cb) => kit.getDailyStepCountSamples(options, cb));
  const energy = await querySamples((cb) => kit.getActiveEnergyBurned(options, cb));
  if (steps.error && energy.error) {
    queryFailures += 1;
    deniedOrEmptyFamilies.push('ACTIVITY');
  } else {
    if (!steps.error) {
      raw.push(...mapQuantitySamples('stepsDaily', steps.samples));
    }
    if (!energy.error) {
      raw.push(...mapQuantitySamples('activeEnergy', energy.samples));
    }
    if ((steps.samples?.length ?? 0) + (energy.samples?.length ?? 0) === 0) {
      deniedOrEmptyFamilies.push('ACTIVITY');
    }
  }

  const workouts = await new Promise<{
    samples: Record<string, unknown>[];
    error?: string;
  }>((resolve) => {
    try {
      kit.getAnchoredWorkouts(options, (error, results) => {
        if (error?.message) {
          resolve({ samples: [], error: error.message });
          return;
        }
        resolve({ samples: Array.isArray(results?.data) ? results.data : [] });
      });
    } catch (error) {
      resolve({ samples: [], error: error instanceof Error ? error.message : String(error) });
    }
  });
  if (workouts.error) {
    queryFailures += 1;
    deniedOrEmptyFamilies.push('WORKOUT');
  } else {
    raw.push(...mapWorkouts(workouts.samples));
    if (workouts.samples.length === 0) {
      deniedOrEmptyFamilies.push('WORKOUT');
    }
  }

  const items = normalizeHealthKitSamples(raw);
  const uniqueDenied = [...new Set(deniedOrEmptyFamilies)];
  const partial = queryFailures > 0 || uniqueDenied.length > 0;
  let message: string | undefined;
  if (items.length === 0) {
    message =
      'No readable Apple Health samples were returned for the selected types. If you expected data, enable Sleep, Heart, HRV, Activity, and Workouts for Athlete Readiness in Settings → Health → Sharing.';
  } else if (partial) {
    message =
      'Some Apple Health types returned no data. Enable missing types in Settings → Health → Sharing if you expected them.';
  }

  return {
    items,
    rawCount: raw.length,
    partial,
    deniedOrEmptyFamilies: uniqueDenied,
    message,
  };
}

export async function readSamplesForLastNDays(days: number = DEFAULT_BACKFILL_DAYS): Promise<HealthKitReadResult> {
  return readSamplesForWindow(windowForBackfillDays(days));
}
