import { HEALTH_CONNECT_SOURCE_APP } from '@/src/features/connectedApps/constants';
import type { EvidenceBatchItem } from '@/src/features/connectedApps/models/evidenceBatch';

/**
 * Provider-agnostic raw sample shapes produced by the Health Connect adapter (not SDK types).
 * Honestly different from HealthKit: HC HRV is RMSSD; sleep uses SleepSession duration.
 */
export type HealthConnectRawSampleKind =
  | 'sleepSession'
  | 'restingHeartRate'
  | 'hrvRmssd'
  | 'steps'
  | 'activeEnergy'
  | 'exerciseSession';

export type HealthConnectRawSample = {
  kind: HealthConnectRawSampleKind;
  /** Health Connect metadata.id when present. */
  id?: string | null;
  startDate: string;
  endDate: string;
  value?: number | null;
  sourceName?: string | null;
  activityName?: string | null;
  durationSeconds?: number | null;
};

export type NormalizeHealthConnectOptions = {
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
  const base = `hc:${kind}:${toIso(startDate)}:${toIso(endDate)}${extra ? `:${extra}` : ''}`;
  return base.slice(0, 191);
}

function sourceOf(sample: HealthConnectRawSample, fallback: string): string {
  const name = sample.sourceName?.trim();
  return (name && name.length > 0 ? name : fallback).slice(0, 128);
}

/**
 * Maps Health Connect adapter samples → EvidenceBatchRequest items (ADR-047).
 * Pure function — unit-tested without a native module.
 */
export function normalizeHealthConnectSamples(
  samples: HealthConnectRawSample[],
  options: NormalizeHealthConnectOptions = {},
): EvidenceBatchItem[] {
  const sourceFallback = options.sourceDeviceOrApp ?? HEALTH_CONNECT_SOURCE_APP;
  const items: EvidenceBatchItem[] = [];

  for (const sample of samples) {
    const observedAt = toIso(sample.endDate || sample.startDate);
    const periodStart = toIso(sample.startDate);
    const periodEnd = toIso(sample.endDate);
    const sourceDeviceOrApp = sourceOf(sample, sourceFallback);

    switch (sample.kind) {
      case 'sleepSession': {
        const minutes =
          typeof sample.durationSeconds === 'number' && sample.durationSeconds > 0
            ? Math.round((sample.durationSeconds / 60) * 1000) / 1000
            : durationMinutes(sample.startDate, sample.endDate);
        if (minutes <= 0) {
          break;
        }
        items.push({
          externalRecordId: (
            sample.id?.trim() || fallbackId('sleep', sample.startDate, sample.endDate)
          ).slice(0, 191),
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
          externalRecordId: (
            sample.id?.trim() || fallbackId('rhr', sample.startDate, sample.endDate)
          ).slice(0, 191),
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
      case 'hrvRmssd': {
        const ms = typeof sample.value === 'number' ? sample.value : Number(sample.value);
        if (!Number.isFinite(ms) || ms <= 0) {
          break;
        }
        items.push({
          externalRecordId: (
            sample.id?.trim() || fallbackId('hrvRmssd', sample.startDate, sample.endDate)
          ).slice(0, 191),
          signalFamily: 'HRV',
          // Honest difference vs HealthKit SDNN — Health Connect exposes RMSSD.
          signalType: 'RMSSD',
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
      case 'steps': {
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
      case 'exerciseSession': {
        const minutes =
          typeof sample.durationSeconds === 'number' && sample.durationSeconds > 0
            ? Math.round((sample.durationSeconds / 60) * 1000) / 1000
            : durationMinutes(sample.startDate, sample.endDate);
        if (minutes <= 0) {
          break;
        }
        const activity = (sample.activityName?.trim() || 'WORKOUT').toUpperCase().replace(/\s+/g, '_');
        items.push({
          externalRecordId: (
            sample.id?.trim() || fallbackId('exercise', sample.startDate, sample.endDate)
          ).slice(0, 191),
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
