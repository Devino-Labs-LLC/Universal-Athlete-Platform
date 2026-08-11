import {
  DailyTrainingLoadSummary,
  PersonalRecord,
  TrainingLoadHistory,
  WeeklyTrainingLoadSummary,
} from '@/src/features/performance/models/performanceSchemas';

export const heaviestWeightRecordFixture: PersonalRecord = {
  id: 'pr-1',
  exercisePerformanceKey: '00000000-0000-4000-8000-000000000001',
  exerciseDefinitionId: '00000000-0000-4000-8000-000000000001',
  recordType: 'HEAVIEST_WEIGHT',
  exerciseName: 'Back Squat',
  measuredValue: 225,
  measuredUnit: 'POUND',
  normalizedValue: 102.1,
  normalizedUnit: 'KILOGRAM',
  estimated: false,
  scheduledDate: '2026-08-08',
};

export const estimatedOneRmFixture: PersonalRecord = {
  id: 'pr-2',
  exercisePerformanceKey: '00000000-0000-4000-8000-000000000001',
  exerciseDefinitionId: '00000000-0000-4000-8000-000000000001',
  recordType: 'HIGHEST_ESTIMATED_ONE_REP_MAX',
  exerciseName: 'Back Squat',
  measuredValue: null,
  measuredUnit: null,
  normalizedValue: 118.4,
  normalizedUnit: 'KILOGRAM',
  estimated: true,
  scheduledDate: '2026-08-08',
};

export const mostRepsAtWeightFixture: PersonalRecord = {
  id: 'pr-3',
  exercisePerformanceKey: '00000000-0000-4000-8000-000000000002',
  exerciseDefinitionId: '00000000-0000-4000-8000-000000000002',
  recordType: 'MOST_REPETITIONS_AT_WEIGHT',
  exerciseName: 'Bench Press',
  repetitions: 12,
  weightValue: 100,
  weightUnit: 'KILOGRAM',
  normalizedValue: 12,
  normalizedUnit: 'REPETITION',
  estimated: false,
  scheduledDate: '2026-08-07',
};

export const weeklyLoadSummaryFixture: WeeklyTrainingLoadSummary = {
  weekStartDate: '2026-08-04',
  weekEndDate: '2026-08-10',
  trainingDays: 3,
  occurrenceCount: 4,
  ratedOccurrenceCount: 3,
  unratedOccurrenceCount: 1,
  completedExerciseCount: 24,
  completedSetCount: 72,
  completedRepetitionCount: 540,
  totalVolumeKilograms: 12500,
  totalDurationSeconds: 7200,
  totalDistanceMeters: 0,
  totalSessionRpeLoad: 1850,
  averageSessionRpe: 7.2,
  totalSessionDurationMinutes: 180,
  highestSessionRpe: 9,
  noImpactExerciseCount: 2,
  lowImpactExerciseCount: 8,
  moderateImpactExerciseCount: 10,
  highImpactExerciseCount: 4,
  categorySummaries: [
    {
      category: 'STRENGTH',
      completedExerciseCount: 12,
      completedSetCount: 36,
      volumeKilograms: 10000,
      durationSeconds: 3600,
      distanceMeters: 0,
    },
  ],
};

export const dailyLoadSummaryFixture: DailyTrainingLoadSummary = {
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
  categorySummaries: [],
};

export const trainingLoadHistoryWeeklyFixture: TrainingLoadHistory = {
  granularity: 'WEEKLY',
  weeklySummaries: [weeklyLoadSummaryFixture],
  page: 0,
  size: 4,
  totalElements: 1,
  totalPages: 1,
};

export const exerciseHistoryPageFixture = {
  exercisePerformanceKey: '00000000-0000-4000-8000-000000000001',
  exerciseDefinitionId: '00000000-0000-4000-8000-000000000001',
  exerciseName: 'Back Squat',
  entries: [
    {
      executionId: 'exec-1',
      occurrenceId: 'occ-1',
      exercisePerformanceKey: '00000000-0000-4000-8000-000000000001',
      exerciseName: 'Back Squat',
      category: 'STRENGTH',
      type: 'COMPOUND',
      displayOrder: 1,
      status: 'COMPLETED',
      scheduledDate: '2026-08-08',
      completedAt: '2026-08-08T18:00:00Z',
      metrics: {
        completedSetCount: 4,
        totalRepetitions: 16,
        mostRepetitionsInSet: 5,
        heaviestWeight: {
          normalizedValue: 102.1,
          normalizedUnit: 'KILOGRAM',
          measuredValue: 225,
          measuredUnit: 'POUND',
          estimated: false,
        },
        bestEstimatedOneRepMax: {
          normalizedValue: 118.4,
          normalizedUnit: 'KILOGRAM',
          estimated: true,
        },
        averageRpe: 8,
      },
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};
