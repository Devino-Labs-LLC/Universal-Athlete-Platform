import { describe, expect, it } from 'vitest';

import {
  exerciseCategoryLabel,
  loadRangeLabel,
  movementPatternLabel,
  PERSONAL_RECORD_TYPE_LABELS,
  personalRecordTypeLabel,
  trainingLoadGranularityLabel,
} from '@/features/performance/models/labels';
import { PERSONAL_RECORD_TYPES } from '@/features/performance/models/schemas';

describe('personalRecordTypeLabel', () => {
  it('labels all seven PR types with human-readable copy', () => {
    for (const type of PERSONAL_RECORD_TYPES) {
      expect(personalRecordTypeLabel(type)).toBe(PERSONAL_RECORD_TYPE_LABELS[type]);
    }
  });

  it('labels the estimated-1RM record type distinctly, calling out "Estimated"', () => {
    expect(personalRecordTypeLabel('HIGHEST_ESTIMATED_ONE_REP_MAX')).toBe('Estimated 1RM');
  });

  it('falls back to a generic label when the type is missing', () => {
    expect(personalRecordTypeLabel(null)).toBe('Personal record');
    expect(personalRecordTypeLabel(undefined)).toBe('Personal record');
  });
});

describe('trainingLoadGranularityLabel', () => {
  it('labels OCCURRENCE, DAILY, WEEKLY', () => {
    expect(trainingLoadGranularityLabel('OCCURRENCE')).toBe('Sessions');
    expect(trainingLoadGranularityLabel('DAILY')).toBe('Daily');
    expect(trainingLoadGranularityLabel('WEEKLY')).toBe('Weekly');
  });
});

describe('loadRangeLabel', () => {
  it('labels the three supported ranges', () => {
    expect(loadRangeLabel(7)).toBe('7 days');
    expect(loadRangeLabel(28)).toBe('28 days');
    expect(loadRangeLabel(90)).toBe('90 days');
  });

  it('falls back to a generic "N days" label for an unmapped range', () => {
    expect(loadRangeLabel(14)).toBe('14 days');
  });
});

describe('movementPatternLabel / exerciseCategoryLabel', () => {
  it('formats enum-style values into title case', () => {
    expect(movementPatternLabel('HIP_HINGE')).toBe('Hip Hinge');
    expect(exerciseCategoryLabel('STRENGTH')).toBe('Strength');
  });
});
