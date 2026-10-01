import { HEALTHKIT_SOURCE_APP } from '@/src/features/connectedApps/constants';
import type { EvidenceBatchItem } from '@/src/features/connectedApps/models/evidenceBatch';

/** Provider-agnostic raw sample shapes produced by the HealthKit adapter (not SDK types). */
export type HealthKitRawSampleKind =
  | 'sleep'
  | 'restingHeartRate'
  | 'hrvSdnn'
  | 'stepsDaily'
  | 'activeEnergy'
  | 'workout';

export type HealthKitRawSample = {
  kind: HealthKitRawSampleKind;
  /** HealthKit UUID when present; otherwise a stable synthetic key. */
  id?: string | null;
  startDate: string;
  endDate: string;
  value?: number | string | null;
  unit?: string | null;
  sourceName?: string | null;
  activityName?: string | null;
  calories?: number | null;
  durationSeconds?: number | null;
};

export type NormalizeHealthKitOptions = {
  sourceDeviceOrApp?: string;
};

function toIso(value: string): string {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toISOString();
}

function durationMinutes(startDate: string, endDate: string): number {
  const start = new Date(startDate).getTime();
  const end = new Date(endDate).getTime();
  if (Number.isNaN(start) || Number.isNaN(end) || end <= start) {
    return 0;
  }
  return Math.round(((end - start) / 60_000) * 1000) / 1000;
}

function fallbackId(kind: string, startDate: string, endDate: string, extra = ''): string {
  const base = `hk:${kind}:${toIso(startDate)}:${toIso(endDate)}${extra ? `:${extra}` : ''}`;
  return base.slice(0, 191);
}

function sourceOf(sample: HealthKitRawSample, fallback: string): string {
  const name = sample.sourceName?.trim();
  return (name && name.length > 0 ? name : fallback).slice(0, 128);
}

function isAsleepStage(value: string | number | null | undefined): boolean {
  if (value == null) {
    return false;
  }
  const normalized = String(value).trim().toUpperCase();
  return (
    normalized === 'ASLEEP' ||
    normalized === 'DEEP' ||
    normalized === 'CORE' ||
    normalized === 'REM' ||
    normalized === 'SLEEPANALYSISASLEEP' ||
    normalized === 'SLEEPANALYSISASLEEPDEEP' ||
    normalized === 'SLEEPANALYSISASLEEPCORE' ||
    normalized === 'SLEEPANALYSISASLEEPREM'
  );
}

/**
 * Maps HealthKit adapter samples → EvidenceBatchRequest items (ADR-047).
 * Pure function — unit-tested without a native module.
 */
export function normalizeHealthKitSamples(
  samples: HealthKitRawSample[],
  options: NormalizeHealthKitOptions = {},
): EvidenceBatchItem[] {
  const sourceFallback = options.sourceDeviceOrApp ?? HEALTHKIT_SOURCE_APP;
  const items: EvidenceBatchItem[] = [];

  for (const sample of samples) {
    const observedAt = toIso(sample.endDate || sample.startDate);
    const periodStart = toIso(sample.startDate);
    const periodEnd = toIso(sample.endDate);
    const sourceDeviceOrApp = sourceOf(sample, sourceFallback);

    switch (sample.kind) {
      case 'sleep': {
        if (!isAsleepStage(sample.value)) {
          // Skip INBED / AWAKE / unknown to avoid double-counting sleep duration.
          break;
        }
        const minutes = durationMinutes(sample.startDate, sample.endDate);
        if (minutes <= 0) {
          break;
        }
        items.push({
          externalRecordId: (sample.id?.trim() || fallbackId('sleep', sample.startDate, sample.endDate)).slice(
            0,
            191,
          ),
          signalFamily: 'SLEEP',
          signalType: 'DURATION',
          valueNumeric: minutes,
          unitCode: 'MINUTE',
          periodStart,
          periodEnd,
          observedAt,
          sourceDeviceOrApp,
          provenanceClass: 'CLIENT_DEVICE',
        });
        break;
      }
      case 'restingHeartRate': {
        const bpm = typeof sample.value === 'number' ? sample.value : Number(sample.value);
        if (!Number.isFinite(bpm) || bpm <= 0) {
          break;
        }
        items.push({
          externalRecordId: (sample.id?.trim() || fallbackId('rhr', sample.startDate, sample.endDate)).slice(
            0,
            191,
          ),
          signalFamily: 'HEART',
          signalType: 'RHR',
          valueNumeric: bpm,
          unitCode: 'BPM',
          periodStart,
          periodEnd,
          observedAt,
          sourceDeviceOrApp,
          provenanceClass: 'CLIENT_DEVICE',
        });
        break;
      }
      case 'hrvSdnn': {
        let ms = typeof sample.value === 'number' ? sample.value : Number(sample.value);
        if (!Number.isFinite(ms) || ms <= 0) {
          break;
        }
        // react-native-health typically returns SDNN in seconds; convert when clearly sub-second.
        const unit = sample.unit?.toLowerCase() ?? '';
        if (unit.includes('s') && !unit.includes('ms') && ms < 10) {
          ms = ms * 1000;
        } else if (!unit && ms > 0 && ms < 10) {
          ms = ms * 1000;
        }
        items.push({
          externalRecordId: (sample.id?.trim() || fallbackId('hrv', sample.startDate, sample.endDate)).slice(
            0,
            191,
          ),
          signalFamily: 'HRV',
          signalType: 'SDNN',
          valueNumeric: Math.round(ms * 1000) / 1000,
          unitCode: 'MS',
          periodStart,
          periodEnd,
          observedAt,
          sourceDeviceOrApp,
          provenanceClass: 'CLIENT_DEVICE',
        });
        break;
      }
      case 'stepsDaily': {
        const count = typeof sample.value === 'number' ? sample.value : Number(sample.value);
        if (!Number.isFinite(count) || count < 0) {
          break;
        }
        const dayKey = periodStart.slice(0, 10);
        items.push({
          externalRecordId: (
            sample.id?.trim() || fallbackId('steps', sample.startDate, sample.endDate, dayKey)
          ).slice(0, 191),
          signalFamily: 'ACTIVITY',
          signalType: 'STEPS',
          valueNumeric: count,
          unitCode: 'COUNT',
          periodStart,
          periodEnd,
          observedAt,
          sourceDeviceOrApp,
          provenanceClass: 'CLIENT_DEVICE',
        });
        break;
      }
      case 'activeEnergy': {
        const kcal = typeof sample.value === 'number' ? sample.value : Number(sample.value);
        if (!Number.isFinite(kcal) || kcal < 0) {
          break;
        }
        items.push({
          externalRecordId: (
            sample.id?.trim() || fallbackId('activeEnergy', sample.startDate, sample.endDate)
          ).slice(0, 191),
          signalFamily: 'ACTIVITY',
          signalType: 'ACTIVE_ENERGY',
          valueNumeric: Math.round(kcal * 1000) / 1000,
          unitCode: 'KCAL',
          periodStart,
          periodEnd,
          observedAt,
          sourceDeviceOrApp,
          provenanceClass: 'CLIENT_DEVICE',
        });
        break;
      }
      case 'workout': {
        const minutes =
          typeof sample.durationSeconds === 'number' && sample.durationSeconds > 0
            ? Math.round((sample.durationSeconds / 60) * 1000) / 1000
            : durationMinutes(sample.startDate, sample.endDate);
        if (minutes <= 0) {
          break;
        }
        const activity = (sample.activityName?.trim() || 'WORKOUT').toUpperCase().replace(/\s+/g, '_');
        items.push({
          externalRecordId: (sample.id?.trim() || fallbackId('workout', sample.startDate, sample.endDate)).slice(
            0,
            191,
          ),
          signalFamily: 'WORKOUT',
          signalType: activity.slice(0, 80),
          valueNumeric: minutes,
          valueText: sample.activityName?.trim() || null,
          unitCode: 'MINUTE',
          periodStart,
          periodEnd,
          observedAt,
          sourceDeviceOrApp,
          provenanceClass: 'CLIENT_DEVICE',
        });
        break;
      }
      default: {
        const _exhaustive: never = sample.kind;
        void _exhaustive;
      }
    }
  }

  return items;
}
