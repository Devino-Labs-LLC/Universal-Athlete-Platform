import { formatPersonalRecord } from '@/src/features/performance/utils/formatPersonalRecord';

import {
  estimatedOneRmFixture,
  heaviestWeightRecordFixture,
  mostRepsAtWeightFixture,
} from './fixtures/performanceFixtures';

describe('formatPersonalRecord', () => {
  it('formats heaviest weight with measured pounds', () => {
    expect(formatPersonalRecord(heaviestWeightRecordFixture)).toBe('225 lb');
  });

  it('formats estimated 1RM with Estimated prefix', () => {
    expect(formatPersonalRecord(estimatedOneRmFixture)).toBe('Estimated 1RM: 118.4 kg');
  });

  it('formats most reps at weight', () => {
    expect(formatPersonalRecord(mostRepsAtWeightFixture)).toBe('12 reps @ 100 kg');
  });

  it('formats most repetitions', () => {
    expect(
      formatPersonalRecord({
        ...heaviestWeightRecordFixture,
        recordType: 'MOST_REPETITIONS',
        repetitions: 12,
      }),
    ).toBe('12 reps');
  });

  it('formats longest duration from seconds', () => {
    expect(
      formatPersonalRecord({
        ...heaviestWeightRecordFixture,
        recordType: 'LONGEST_DURATION',
        measuredValue: 3600,
        measuredUnit: 'SECOND',
      }),
    ).toBe('1h');
  });

  it('formats highest set volume', () => {
    expect(
      formatPersonalRecord({
        ...heaviestWeightRecordFixture,
        recordType: 'HIGHEST_SET_VOLUME',
        measuredValue: 800,
        measuredUnit: 'KILOGRAM_REPETITION',
      }),
    ).toBe('800 kg');
  });

  it('formats longest distance', () => {
    expect(
      formatPersonalRecord({
        ...heaviestWeightRecordFixture,
        recordType: 'LONGEST_DISTANCE',
        measuredValue: 5000,
        measuredUnit: 'METER',
      }),
    ).toBe('5.0 km');
  });

  it('trims trailing decimal zeros in display', () => {
    expect(
      formatPersonalRecord({
        ...estimatedOneRmFixture,
        normalizedValue: 120,
      }),
    ).toBe('Estimated 1RM: 120 kg');
  });
});
