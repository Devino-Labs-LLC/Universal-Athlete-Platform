import {
  formatDecimal,
  formatDistance,
  formatDurationSeconds,
  formatVolumeKg,
} from '@/features/performance/utils/formatMetrics';
import type { PerformanceMeasurement, PersonalRecord } from '@/features/performance/models/schemas';

function trimDisplayNumber(value: number | string): string {
  const numeric = Number(value);
  if (Number.isNaN(numeric)) {
    return String(value);
  }
  if (Number.isInteger(numeric)) {
    return String(numeric);
  }
  return numeric.toFixed(1).replace(/\.0$/, '');
}

function formatWeight(value: number | string, unit?: string | null): string {
  const trimmed = trimDisplayNumber(value);
  const normalizedUnit = unit?.toLowerCase() ?? 'kg';
  if (normalizedUnit === 'pound' || normalizedUnit === 'lb' || normalizedUnit === 'pounds') {
    return `${trimmed} lb`;
  }
  if (normalizedUnit === 'kilogram' || normalizedUnit === 'kg' || normalizedUnit === 'kilograms') {
    return `${trimmed} kg`;
  }
  return unit ? `${trimmed} ${unit}` : trimmed;
}

function formatMeasurementValue(
  measuredValue: number | string | null | undefined,
  measuredUnit: string | null | undefined,
  normalizedValue: number | string | null | undefined,
  normalizedUnit: string | null | undefined,
): string | null {
  if (measuredValue != null) {
    const unit = measuredUnit?.toUpperCase();
    if (unit === 'KILOGRAM' || unit === 'POUND') {
      return formatWeight(measuredValue, measuredUnit);
    }
    if (unit === 'SECOND') {
      return formatDurationSeconds(measuredValue);
    }
    if (unit === 'METER') {
      return formatDistance(measuredValue);
    }
    if (unit === 'REPETITION') {
      return `${trimDisplayNumber(measuredValue)} reps`;
    }
    if (unit === 'KILOGRAM_REPETITION') {
      return formatVolumeKg(measuredValue);
    }
    return measuredUnit
      ? `${trimDisplayNumber(measuredValue)} ${measuredUnit.toLowerCase()}`
      : trimDisplayNumber(measuredValue);
  }

  if (normalizedValue != null) {
    const unit = normalizedUnit?.toUpperCase();
    if (unit === 'KILOGRAM') {
      return formatWeight(normalizedValue, 'kg');
    }
    if (unit === 'SECOND') {
      return formatDurationSeconds(normalizedValue);
    }
    if (unit === 'METER') {
      return formatDistance(normalizedValue);
    }
    if (unit === 'REPETITION') {
      return `${trimDisplayNumber(normalizedValue)} reps`;
    }
    if (unit === 'KILOGRAM_REPETITION') {
      return formatVolumeKg(normalizedValue);
    }
    return normalizedUnit
      ? `${trimDisplayNumber(normalizedValue)} ${normalizedUnit.toLowerCase()}`
      : trimDisplayNumber(normalizedValue);
  }

  return null;
}

export function formatPerformanceMeasurement(measurement: PerformanceMeasurement | null | undefined): string | null {
  if (!measurement) {
    return null;
  }
  const value = formatMeasurementValue(
    measurement.measuredValue,
    measurement.measuredUnit,
    measurement.normalizedValue,
    measurement.normalizedUnit,
  );
  if (!value) {
    return null;
  }
  if (measurement.estimated) {
    return `Estimated 1RM: ${value}`;
  }
  return value;
}

export function formatPersonalRecord(record: PersonalRecord): string {
  switch (record.recordType) {
    case 'HEAVIEST_WEIGHT': {
      const value = formatMeasurementValue(
        record.measuredValue,
        record.measuredUnit,
        record.normalizedValue,
        record.normalizedUnit,
      );
      return value ?? '—';
    }
    case 'MOST_REPETITIONS': {
      const reps = record.repetitions ?? record.measuredValue ?? record.normalizedValue;
      return reps != null ? `${trimDisplayNumber(reps)} reps` : '—';
    }
    case 'MOST_REPETITIONS_AT_WEIGHT': {
      const reps = record.repetitions ?? record.measuredValue ?? record.normalizedValue;
      const weight =
        record.weightValue != null ? formatWeight(record.weightValue, record.weightUnit) : record.recordQualifier;
      if (reps != null && weight) {
        return `${trimDisplayNumber(reps)} reps @ ${weight}`;
      }
      if (reps != null) {
        return `${trimDisplayNumber(reps)} reps`;
      }
      return record.recordQualifier ?? '—';
    }
    case 'HIGHEST_ESTIMATED_ONE_REP_MAX': {
      const value = formatMeasurementValue(
        record.measuredValue,
        record.measuredUnit,
        record.normalizedValue,
        record.normalizedUnit,
      );
      return value ? `Estimated 1RM: ${value}` : 'Estimated 1RM';
    }
    case 'HIGHEST_SET_VOLUME': {
      const value = formatMeasurementValue(
        record.measuredValue,
        record.measuredUnit,
        record.normalizedValue,
        record.normalizedUnit,
      );
      return value ?? '—';
    }
    case 'LONGEST_DURATION': {
      const seconds = record.measuredValue ?? record.normalizedValue ?? record.repetitions ?? null;
      return seconds != null ? formatDurationSeconds(seconds) : '—';
    }
    case 'LONGEST_DISTANCE': {
      const value = formatMeasurementValue(
        record.measuredValue,
        record.measuredUnit,
        record.normalizedValue,
        record.normalizedUnit,
      );
      return value ?? '—';
    }
    default: {
      const fallback = formatMeasurementValue(
        record.measuredValue,
        record.measuredUnit,
        record.normalizedValue,
        record.normalizedUnit,
      );
      return fallback ?? formatDecimal(record.normalizedValue ?? 0);
    }
  }
}
