import { describe, expect, it } from 'vitest';

import { aggregateCategorySummaries, aggregateMovementSummaries } from '@/features/performance/utils/aggregateDistributions';
import type { TrainingLoadHistory } from '@/features/performance/models/schemas';

describe('aggregateCategorySummaries', () => {
  it('sums category volumes across OCCURRENCE-granularity sessions', () => {
    const history: TrainingLoadHistory = {
      granularity: 'OCCURRENCE',
      occurrences: [
        {
          summary: {
            id: 'occ-1',
            trainingPlanId: 'p1',
            workoutDayId: 'd1',
            workoutOccurrenceId: 'o1',
            scheduledDate: '2026-01-01',
            categorySummaries: [
              { category: 'STRENGTH', completedExerciseCount: 3, completedSetCount: 9, volumeKilograms: 500, durationSeconds: 1200, distanceMeters: 0 },
            ],
          },
        },
        {
          summary: {
            id: 'occ-2',
            trainingPlanId: 'p1',
            workoutDayId: 'd1',
            workoutOccurrenceId: 'o2',
            scheduledDate: '2026-01-02',
            categorySummaries: [
              { category: 'STRENGTH', completedExerciseCount: 2, completedSetCount: 6, volumeKilograms: 300, durationSeconds: 900, distanceMeters: 0 },
              { category: 'CARDIO', completedExerciseCount: 1, completedSetCount: 1, volumeKilograms: 0, durationSeconds: 1800, distanceMeters: 5000 },
            ],
          },
        },
      ],
      page: 0,
      size: 50,
      totalElements: 2,
      totalPages: 1,
    };

    const summaries = aggregateCategorySummaries(history);
    const strength = summaries.find((s) => s.category === 'STRENGTH')!;
    expect(strength.completedSetCount).toBe(15);
    expect(Number(strength.volumeKilograms)).toBe(800);
  });

  it('sorts categories by descending total volume', () => {
    const history: TrainingLoadHistory = {
      granularity: 'DAILY',
      dailySummaries: [
        {
          date: '2026-01-01',
          occurrenceCount: 1,
          ratedOccurrenceCount: 1,
          unratedOccurrenceCount: 0,
          completedExerciseCount: 3,
          completedSetCount: 9,
          completedRepetitionCount: 90,
          totalDurationSeconds: 1200,
          totalSessionRpeLoad: 200,
          totalSessionDurationMinutes: 20,
          noImpactExerciseCount: 0,
          lowImpactExerciseCount: 3,
          moderateImpactExerciseCount: 0,
          highImpactExerciseCount: 0,
          categorySummaries: [
            { category: 'CARDIO', completedExerciseCount: 1, completedSetCount: 1, volumeKilograms: 100, durationSeconds: 600, distanceMeters: 2000 },
            { category: 'STRENGTH', completedExerciseCount: 2, completedSetCount: 8, volumeKilograms: 900, durationSeconds: 600, distanceMeters: 0 },
          ],
        },
      ],
      page: 0,
      size: 0,
      totalElements: 0,
      totalPages: 0,
    };

    const summaries = aggregateCategorySummaries(history);
    expect(summaries[0]!.category).toBe('STRENGTH');
    expect(summaries[1]!.category).toBe('CARDIO');
  });

  it('returns an empty array when there are no category summaries at all', () => {
    const history: TrainingLoadHistory = {
      granularity: 'WEEKLY',
      weeklySummaries: [],
      page: 0,
      size: 0,
      totalElements: 0,
      totalPages: 0,
    };
    expect(aggregateCategorySummaries(history)).toEqual([]);
  });
});

describe('aggregateMovementSummaries', () => {
  it('sums repetition and volume totals across buckets for the same movement pattern', () => {
    const history: TrainingLoadHistory = {
      granularity: 'WEEKLY',
      weeklySummaries: [
        {
          weekStartDate: '2026-01-01',
          weekEndDate: '2026-01-07',
          trainingDays: 3,
          occurrenceCount: 3,
          ratedOccurrenceCount: 3,
          unratedOccurrenceCount: 0,
          completedExerciseCount: 9,
          completedSetCount: 27,
          completedRepetitionCount: 270,
          totalDurationSeconds: 3600,
          totalSessionRpeLoad: 600,
          totalSessionDurationMinutes: 60,
          noImpactExerciseCount: 0,
          lowImpactExerciseCount: 9,
          moderateImpactExerciseCount: 0,
          highImpactExerciseCount: 0,
          movementSummaries: [
            {
              primaryMovementPattern: 'SQUAT',
              completedExerciseCount: 3,
              completedSetCount: 9,
              completedRepetitionCount: 90,
              volumeKilograms: 1000,
              durationSeconds: 1200,
              distanceMeters: 0,
            },
          ],
        },
        {
          weekStartDate: '2026-01-08',
          weekEndDate: '2026-01-14',
          trainingDays: 3,
          occurrenceCount: 3,
          ratedOccurrenceCount: 3,
          unratedOccurrenceCount: 0,
          completedExerciseCount: 9,
          completedSetCount: 27,
          completedRepetitionCount: 270,
          totalDurationSeconds: 3600,
          totalSessionRpeLoad: 600,
          totalSessionDurationMinutes: 60,
          noImpactExerciseCount: 0,
          lowImpactExerciseCount: 9,
          moderateImpactExerciseCount: 0,
          highImpactExerciseCount: 0,
          movementSummaries: [
            {
              primaryMovementPattern: 'SQUAT',
              completedExerciseCount: 3,
              completedSetCount: 9,
              completedRepetitionCount: 90,
              volumeKilograms: 500,
              durationSeconds: 1200,
              distanceMeters: 0,
            },
          ],
        },
      ],
      page: 0,
      size: 0,
      totalElements: 0,
      totalPages: 0,
    };

    const summaries = aggregateMovementSummaries(history);
    expect(summaries).toHaveLength(1);
    expect(summaries[0]!.completedRepetitionCount).toBe(180);
    expect(Number(summaries[0]!.volumeKilograms)).toBe(1500);
  });
});
