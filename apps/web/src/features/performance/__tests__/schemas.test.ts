import { describe, expect, it } from 'vitest';

import {
  athleteExercisePerformanceHistorySchema,
  getNextHistoryPage,
  isPersonalRecordType,
  isTrainingLoadGranularity,
  PERSONAL_RECORD_TYPES,
  personalRecordSchema,
  trainingLoadHistorySchema,
  workoutOccurrencePerformanceSchema,
} from '@/features/performance/models/schemas';

describe('PersonalRecordType guard', () => {
  it('recognizes all seven backend record types', () => {
    expect(PERSONAL_RECORD_TYPES).toHaveLength(7);
    for (const type of PERSONAL_RECORD_TYPES) {
      expect(isPersonalRecordType(type)).toBe(true);
    }
  });

  it('rejects an unknown record type', () => {
    expect(isPersonalRecordType('FASTEST_TIME')).toBe(false);
  });
});

describe('TrainingLoadGranularity guard', () => {
  it('accepts OCCURRENCE, DAILY, and WEEKLY', () => {
    expect(isTrainingLoadGranularity('OCCURRENCE')).toBe(true);
    expect(isTrainingLoadGranularity('DAILY')).toBe(true);
    expect(isTrainingLoadGranularity('WEEKLY')).toBe(true);
    expect(isTrainingLoadGranularity('MONTHLY')).toBe(false);
  });
});

describe('personalRecordSchema', () => {
  it('parses a record grouped by exercisePerformanceKey (not by name)', () => {
    const record = personalRecordSchema.parse({
      id: 'pr-1',
      exercisePerformanceKey: 'key-abc',
      exerciseDefinitionId: 'def-1',
      recordType: 'HEAVIEST_WEIGHT',
      exerciseName: 'Back Squat',
      measuredValue: 140,
      measuredUnit: 'KILOGRAM',
    });
    expect(record.exercisePerformanceKey).toBe('key-abc');
  });

  it('marks HIGHEST_ESTIMATED_ONE_REP_MAX records as estimated when flagged', () => {
    const record = personalRecordSchema.parse({
      id: 'pr-2',
      exercisePerformanceKey: 'key-abc',
      exerciseDefinitionId: 'def-1',
      recordType: 'HIGHEST_ESTIMATED_ONE_REP_MAX',
      exerciseName: 'Back Squat',
      measuredValue: 150,
      measuredUnit: 'KILOGRAM',
      estimated: true,
    });
    expect(record.estimated).toBe(true);
  });
});

describe('athleteExercisePerformanceHistorySchema', () => {
  it('parses a paginated history envelope', () => {
    const parsed = athleteExercisePerformanceHistorySchema.parse({
      exercisePerformanceKey: 'key-abc',
      exerciseDefinitionId: 'def-1',
      exerciseName: 'Back Squat',
      entries: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
    expect(parsed.entries).toEqual([]);
  });
});

describe('getNextHistoryPage', () => {
  it('returns the next page number while more pages remain', () => {
    expect(getNextHistoryPage(0, 3)).toBe(1);
    expect(getNextHistoryPage(1, 3)).toBe(2);
  });

  it('returns undefined once the last page has been reached', () => {
    expect(getNextHistoryPage(2, 3)).toBeUndefined();
    expect(getNextHistoryPage(0, 1)).toBeUndefined();
  });
});

describe('workoutOccurrencePerformanceSchema', () => {
  it('parses totals with a null averageRpe (unrated), not zero', () => {
    const parsed = workoutOccurrencePerformanceSchema.parse({
      occurrenceId: 'occ-1',
      scheduledDate: '2026-02-01',
      status: 'COMPLETED',
      totals: {
        completedExerciseCount: 3,
        completedSetCount: 9,
        totalRepetitions: 90,
        totalVolumeKilogramRepetitions: 4500,
        totalDurationSeconds: 2400,
        totalDistanceMeters: null,
        averageRpe: null,
      },
      exercises: [],
    });
    expect(parsed.totals.averageRpe).toBeNull();
  });
});

describe('trainingLoadHistorySchema', () => {
  it('parses an OCCURRENCE-granularity envelope', () => {
    const parsed = trainingLoadHistorySchema.parse({
      granularity: 'OCCURRENCE',
      occurrences: [],
      page: 0,
      size: 50,
      totalElements: 0,
      totalPages: 0,
    });
    expect(parsed.granularity).toBe('OCCURRENCE');
  });

  it('parses a DAILY-granularity envelope with page fields zeroed (full-range aggregate)', () => {
    const parsed = trainingLoadHistorySchema.parse({
      granularity: 'DAILY',
      dailySummaries: [],
      page: 0,
      size: 0,
      totalElements: 0,
      totalPages: 0,
    });
    expect(parsed.granularity).toBe('DAILY');
    expect(parsed.page).toBe(0);
    expect(parsed.totalPages).toBe(0);
  });
});
