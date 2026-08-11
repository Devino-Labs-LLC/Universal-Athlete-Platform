import {
  athleteExercisePerformanceHistorySchema,
  getNextHistoryPage,
  personalRecordSchema,
  trainingLoadHistorySchema,
} from '@/src/features/performance/models/performanceSchemas';

import {
  estimatedOneRmFixture,
  exerciseHistoryPageFixture,
  heaviestWeightRecordFixture,
  trainingLoadHistoryWeeklyFixture,
} from './fixtures/performanceFixtures';

describe('performanceSchemas', () => {
  it('parses personal record fixture for each major type', () => {
    expect(personalRecordSchema.parse(heaviestWeightRecordFixture).recordType).toBe(
      'HEAVIEST_WEIGHT',
    );
    expect(personalRecordSchema.parse(estimatedOneRmFixture).estimated).toBe(true);
  });

  it('parses exercise history page fixture', () => {
    const parsed = athleteExercisePerformanceHistorySchema.parse(exerciseHistoryPageFixture);
    expect(parsed.entries).toHaveLength(1);
    expect(parsed.entries[0].metrics.completedSetCount).toBe(4);
  });

  it('parses weekly training load history fixture', () => {
    const parsed = trainingLoadHistorySchema.parse(trainingLoadHistoryWeeklyFixture);
    expect(parsed.granularity).toBe('WEEKLY');
    expect(parsed.weeklySummaries?.[0].ratedOccurrenceCount).toBe(3);
  });

  it('parses daily summaries with null session RPE load', () => {
    const parsed = trainingLoadHistorySchema.parse({
      ...trainingLoadHistoryWeeklyFixture,
      granularity: 'DAILY',
      dailySummaries: [
        {
          date: '2026-08-10',
          occurrenceCount: 1,
          ratedOccurrenceCount: 0,
          unratedOccurrenceCount: 1,
          completedExerciseCount: 6,
          completedSetCount: 18,
          completedRepetitionCount: 120,
          totalVolumeKilograms: 3200,
          totalDurationSeconds: 2400,
          totalDistanceMeters: 0,
          totalSessionRpeLoad: null,
          averageSessionRpe: null,
          totalSessionDurationMinutes: 60,
          noImpactExerciseCount: 0,
          lowImpactExerciseCount: 2,
          moderateImpactExerciseCount: 3,
          highImpactExerciseCount: 1,
        },
      ],
    });
    expect(parsed.dailySummaries?.[0].totalSessionRpeLoad).toBeNull();
  });

  it('computes next history page', () => {
    expect(getNextHistoryPage(0, 3)).toBe(1);
    expect(getNextHistoryPage(2, 3)).toBeUndefined();
  });
});
