import { describe, expect, it } from 'vitest';

import { formatPerformanceMeasurement, formatPersonalRecord } from '@/features/performance/utils/formatPersonalRecord';
import type { PersonalRecord } from '@/features/performance/models/schemas';

function baseRecord(overrides: Partial<PersonalRecord>): PersonalRecord {
  return {
    id: 'pr-1',
    exercisePerformanceKey: 'key-1',
    exerciseDefinitionId: 'def-1',
    recordType: 'HEAVIEST_WEIGHT',
    exerciseName: 'Back Squat',
    ...overrides,
  } as PersonalRecord;
}

describe('formatPersonalRecord — HEAVIEST_WEIGHT', () => {
  it('formats the measured weight with its unit', () => {
    const record = baseRecord({ recordType: 'HEAVIEST_WEIGHT', measuredValue: 140, measuredUnit: 'KILOGRAM' });
    expect(formatPersonalRecord(record)).toBe('140 kg');
  });

  it('falls back to a dash when no value is present', () => {
    const record = baseRecord({ recordType: 'HEAVIEST_WEIGHT' });
    expect(formatPersonalRecord(record)).toBe('—');
  });
});

describe('formatPersonalRecord — MOST_REPETITIONS', () => {
  it('formats the repetition count', () => {
    const record = baseRecord({ recordType: 'MOST_REPETITIONS', repetitions: 25 });
    expect(formatPersonalRecord(record)).toBe('25 reps');
  });

  it('falls back to measuredValue when repetitions is absent', () => {
    const record = baseRecord({ recordType: 'MOST_REPETITIONS', measuredValue: 20 });
    expect(formatPersonalRecord(record)).toBe('20 reps');
  });
});

describe('formatPersonalRecord — MOST_REPETITIONS_AT_WEIGHT', () => {
  it('formats reps at a specific weight', () => {
    const record = baseRecord({
      recordType: 'MOST_REPETITIONS_AT_WEIGHT',
      repetitions: 8,
      weightValue: 100,
      weightUnit: 'KILOGRAM',
    });
    expect(formatPersonalRecord(record)).toBe('8 reps @ 100 kg');
  });

  it('falls back to reps alone when no weight is present', () => {
    const record = baseRecord({ recordType: 'MOST_REPETITIONS_AT_WEIGHT', repetitions: 8 });
    expect(formatPersonalRecord(record)).toBe('8 reps');
  });
});

describe('formatPersonalRecord — HIGHEST_ESTIMATED_ONE_REP_MAX', () => {
  it('labels the value with "Estimated 1RM" prefix', () => {
    const record = baseRecord({
      recordType: 'HIGHEST_ESTIMATED_ONE_REP_MAX',
      measuredValue: 155,
      measuredUnit: 'KILOGRAM',
      estimated: true,
    });
    expect(formatPersonalRecord(record)).toBe('Estimated 1RM: 155 kg');
  });

  it('falls back to a bare "Estimated 1RM" label with no value', () => {
    const record = baseRecord({ recordType: 'HIGHEST_ESTIMATED_ONE_REP_MAX' });
    expect(formatPersonalRecord(record)).toBe('Estimated 1RM');
  });
});

describe('formatPersonalRecord — HIGHEST_SET_VOLUME', () => {
  it('formats the volume with its unit', () => {
    const record = baseRecord({
      recordType: 'HIGHEST_SET_VOLUME',
      measuredValue: 800,
      measuredUnit: 'KILOGRAM_REPETITION',
    });
    expect(formatPersonalRecord(record)).toBe('800 kg');
  });
});

describe('formatPersonalRecord — LONGEST_DURATION', () => {
  it('formats duration in minutes/hours from seconds', () => {
    const record = baseRecord({ recordType: 'LONGEST_DURATION', measuredValue: 5400 });
    expect(formatPersonalRecord(record)).toBe('1h 30m');
  });

  it('falls back to a dash when no duration value is present', () => {
    const record = baseRecord({ recordType: 'LONGEST_DURATION' });
    expect(formatPersonalRecord(record)).toBe('—');
  });
});

describe('formatPersonalRecord — LONGEST_DISTANCE', () => {
  it('formats the measured distance', () => {
    const record = baseRecord({ recordType: 'LONGEST_DISTANCE', measuredValue: 5000, measuredUnit: 'METER' });
    expect(formatPersonalRecord(record)).toBe('5.0 km');
  });
});

describe('formatPersonalRecord — unknown record type fallback', () => {
  it('falls back to the formatted normalized value when the record type is unrecognized', () => {
    const record = baseRecord({ recordType: 'SOME_NEW_TYPE' as PersonalRecord['recordType'], normalizedValue: 3 });
    expect(formatPersonalRecord(record)).toBe('3');
  });

  it('falls back to a decimal "0.0" when neither measured nor normalized values exist', () => {
    const record = baseRecord({ recordType: 'SOME_NEW_TYPE' as PersonalRecord['recordType'] });
    expect(formatPersonalRecord(record)).toBe('0.0');
  });
});

describe('formatPerformanceMeasurement', () => {
  it('returns null for a missing measurement', () => {
    expect(formatPerformanceMeasurement(null)).toBeNull();
    expect(formatPerformanceMeasurement(undefined)).toBeNull();
  });

  it('prefixes "Estimated 1RM" when the measurement is flagged as estimated', () => {
    expect(
      formatPerformanceMeasurement({ measuredValue: 150, measuredUnit: 'KILOGRAM', estimated: true }),
    ).toBe('Estimated 1RM: 150 kg');
  });

  it('formats reps and pounds-based weights', () => {
    expect(formatPerformanceMeasurement({ measuredValue: 10, measuredUnit: 'REPETITION' })).toBe('10 reps');
    expect(formatPerformanceMeasurement({ measuredValue: 225, measuredUnit: 'POUND' })).toBe('225 lb');
  });
});
