import { z } from 'zod';

const bigDecimalLike = z.union([z.number(), z.string()]).transform(Number).nullable().optional();

export const PERSONAL_RECORD_TYPES = [
  'HEAVIEST_WEIGHT',
  'MOST_REPETITIONS',
  'MOST_REPETITIONS_AT_WEIGHT',
  'HIGHEST_ESTIMATED_ONE_REP_MAX',
  'HIGHEST_SET_VOLUME',
  'LONGEST_DURATION',
  'LONGEST_DISTANCE',
] as const;
export type PersonalRecordType = (typeof PERSONAL_RECORD_TYPES)[number];

export function isPersonalRecordType(value: string): value is PersonalRecordType {
  return (PERSONAL_RECORD_TYPES as readonly string[]).includes(value);
}

export const performanceMeasurementSchema = z
  .object({
    normalizedValue: bigDecimalLike,
    normalizedUnit: z.string().nullable().optional(),
    measuredValue: bigDecimalLike,
    measuredUnit: z.string().nullable().optional(),
    estimated: z.boolean().optional(),
  })
  .passthrough()
  .nullable()
  .optional();

export type PerformanceMeasurement = z.infer<typeof performanceMeasurementSchema>;

export const exercisePerformanceMetricsSchema = z
  .object({
    completedSetCount: z.number(),
    totalRepetitions: z.number().nullable().optional(),
    mostRepetitionsInSet: z.number().nullable().optional(),
    heaviestWeight: performanceMeasurementSchema,
    bestEstimatedOneRepMax: performanceMeasurementSchema,
    bestSetVolume: performanceMeasurementSchema,
    totalVolume: performanceMeasurementSchema,
    longestSetDurationSeconds: z.number().nullable().optional(),
    totalDurationSeconds: z.number().nullable().optional(),
    longestSetDistance: performanceMeasurementSchema,
    totalDistance: performanceMeasurementSchema,
    averageRpe: bigDecimalLike,
  })
  .passthrough();

export type ExercisePerformanceMetrics = z.infer<typeof exercisePerformanceMetricsSchema>;

export const exerciseExecutionPerformanceSchema = z
  .object({
    executionId: z.string(),
    occurrenceId: z.string(),
    exercisePerformanceKey: z.string(),
    exerciseName: z.string(),
    category: z.string().nullable().optional(),
    type: z.string().nullable().optional(),
    displayOrder: z.number(),
    status: z.string(),
    scheduledDate: z.string(),
    completedAt: z.string().nullable().optional(),
    metrics: exercisePerformanceMetricsSchema,
  })
  .passthrough();

export type ExerciseExecutionPerformance = z.infer<typeof exerciseExecutionPerformanceSchema>;

export const personalRecordSchema = z
  .object({
    id: z.string(),
    exercisePerformanceKey: z.string(),
    exerciseDefinitionId: z.string(),
    recordType: z.string(),
    recordQualifier: z.string().nullable().optional(),
    exerciseName: z.string(),
    normalizedValue: bigDecimalLike,
    normalizedUnit: z.string().nullable().optional(),
    measuredValue: bigDecimalLike,
    measuredUnit: z.string().nullable().optional(),
    estimated: z.boolean().optional(),
    repetitions: z.number().nullable().optional(),
    weightValue: bigDecimalLike,
    weightUnit: z.string().nullable().optional(),
    achievedAt: z.string().nullable().optional(),
    scheduledDate: z.string().nullable().optional(),
    sourceSetId: z.string().nullable().optional(),
    sourceExecutionId: z.string().nullable().optional(),
    sourceOccurrenceId: z.string().nullable().optional(),
    createdAt: z.string().nullable().optional(),
    updatedAt: z.string().nullable().optional(),
  })
  .passthrough();

export type PersonalRecord = z.infer<typeof personalRecordSchema>;

export const personalRecordsSchema = z.array(personalRecordSchema);

export const athleteExercisePerformanceHistorySchema = z
  .object({
    exercisePerformanceKey: z.string(),
    exerciseDefinitionId: z.string(),
    exerciseName: z.string(),
    entries: z.array(exerciseExecutionPerformanceSchema),
    page: z.number(),
    size: z.number(),
    totalElements: z.number(),
    totalPages: z.number(),
  })
  .passthrough();

export type AthleteExercisePerformanceHistory = z.infer<typeof athleteExercisePerformanceHistorySchema>;

export const workoutOccurrencePerformanceTotalsSchema = z
  .object({
    completedExerciseCount: z.number(),
    completedSetCount: z.number(),
    totalRepetitions: z.number().nullable().optional(),
    totalVolumeKilogramRepetitions: bigDecimalLike,
    totalDurationSeconds: z.number().nullable().optional(),
    totalDistanceMeters: bigDecimalLike,
    averageRpe: bigDecimalLike,
  })
  .passthrough();

export type WorkoutOccurrencePerformanceTotals = z.infer<typeof workoutOccurrencePerformanceTotalsSchema>;

export const workoutOccurrencePerformanceSchema = z
  .object({
    occurrenceId: z.string(),
    scheduledDate: z.string(),
    status: z.string(),
    startedAt: z.string().nullable().optional(),
    completedAt: z.string().nullable().optional(),
    totals: workoutOccurrencePerformanceTotalsSchema,
    exercises: z.array(exerciseExecutionPerformanceSchema),
  })
  .passthrough();

export type WorkoutOccurrencePerformance = z.infer<typeof workoutOccurrencePerformanceSchema>;

export const workoutLoadCategorySummarySchema = z
  .object({
    category: z.string(),
    completedExerciseCount: z.number(),
    completedSetCount: z.number(),
    volumeKilograms: bigDecimalLike,
    volumeUnit: z.string().nullable().optional(),
    durationSeconds: z.number(),
    durationUnit: z.string().nullable().optional(),
    distanceMeters: bigDecimalLike,
    distanceUnit: z.string().nullable().optional(),
  })
  .passthrough();

export type WorkoutLoadCategorySummary = z.infer<typeof workoutLoadCategorySummarySchema>;

export const workoutLoadMovementSummarySchema = z
  .object({
    primaryMovementPattern: z.string(),
    completedExerciseCount: z.number(),
    completedSetCount: z.number(),
    completedRepetitionCount: z.number(),
    volumeKilograms: bigDecimalLike,
    volumeUnit: z.string().nullable().optional(),
    durationSeconds: z.number(),
    durationUnit: z.string().nullable().optional(),
    distanceMeters: bigDecimalLike,
    distanceUnit: z.string().nullable().optional(),
  })
  .passthrough();

export type WorkoutLoadMovementSummary = z.infer<typeof workoutLoadMovementSummarySchema>;

export const workoutOccurrenceLoadSummarySchema = z
  .object({
    id: z.string(),
    trainingPlanId: z.string(),
    workoutDayId: z.string(),
    workoutOccurrenceId: z.string(),
    scheduledDate: z.string(),
    sessionRpe: bigDecimalLike,
    sessionDurationMinutes: z.number().nullable().optional(),
    sessionRpeLoad: bigDecimalLike,
    sessionRpeLoadUnit: z.string().nullable().optional(),
    prescribedExerciseCount: z.number().optional(),
    completedExerciseCount: z.number().optional(),
    substitutedExerciseCount: z.number().optional(),
    completedSetCount: z.number().optional(),
    skippedSetCount: z.number().optional(),
    completedRepetitionCount: z.number().optional(),
    totalVolumeKilograms: bigDecimalLike,
    totalVolumeUnit: z.string().nullable().optional(),
    totalDurationSeconds: z.number().optional(),
    totalDurationUnit: z.string().nullable().optional(),
    totalDistanceMeters: bigDecimalLike,
    totalDistanceUnit: z.string().nullable().optional(),
    noImpactExerciseCount: z.number().optional(),
    lowImpactExerciseCount: z.number().optional(),
    moderateImpactExerciseCount: z.number().optional(),
    highImpactExerciseCount: z.number().optional(),
    categorySummaries: z.array(workoutLoadCategorySummarySchema).optional(),
    movementSummaries: z.array(workoutLoadMovementSummarySchema).optional(),
    calculatedAt: z.string().nullable().optional(),
    sourceUpdatedAt: z.string().nullable().optional(),
    calculationVersion: z.string().nullable().optional(),
  })
  .passthrough();

export type WorkoutOccurrenceLoadSummary = z.infer<typeof workoutOccurrenceLoadSummarySchema>;

const loadSummaryBaseFields = {
  occurrenceCount: z.number(),
  ratedOccurrenceCount: z.number(),
  unratedOccurrenceCount: z.number(),
  completedExerciseCount: z.number(),
  completedSetCount: z.number(),
  completedRepetitionCount: z.number(),
  totalVolumeKilograms: bigDecimalLike,
  totalVolumeUnit: z.string().nullable().optional(),
  totalDurationSeconds: z.number(),
  totalDurationUnit: z.string().nullable().optional(),
  totalDistanceMeters: bigDecimalLike,
  totalDistanceUnit: z.string().nullable().optional(),
  totalSessionRpeLoad: bigDecimalLike,
  totalSessionRpeLoadUnit: z.string().nullable().optional(),
  averageSessionRpe: bigDecimalLike,
  totalSessionDurationMinutes: z.number(),
  noImpactExerciseCount: z.number(),
  lowImpactExerciseCount: z.number(),
  moderateImpactExerciseCount: z.number(),
  highImpactExerciseCount: z.number(),
  categorySummaries: z.array(workoutLoadCategorySummarySchema).optional(),
  movementSummaries: z.array(workoutLoadMovementSummarySchema).optional(),
};

export const dailyTrainingLoadSummarySchema = z
  .object({
    date: z.string(),
    ...loadSummaryBaseFields,
  })
  .passthrough();

export type DailyTrainingLoadSummary = z.infer<typeof dailyTrainingLoadSummarySchema>;

export const weeklyTrainingLoadSummarySchema = z
  .object({
    weekStartDate: z.string(),
    weekEndDate: z.string(),
    trainingDays: z.number(),
    highestSessionRpe: bigDecimalLike,
    ...loadSummaryBaseFields,
  })
  .passthrough();

export type WeeklyTrainingLoadSummary = z.infer<typeof weeklyTrainingLoadSummarySchema>;

export const occurrenceTrainingLoadHistoryItemSchema = z
  .object({
    summary: workoutOccurrenceLoadSummarySchema,
  })
  .passthrough();

export type OccurrenceTrainingLoadHistoryItem = z.infer<typeof occurrenceTrainingLoadHistoryItemSchema>;

export const TRAINING_LOAD_GRANULARITIES = ['OCCURRENCE', 'DAILY', 'WEEKLY'] as const;
export const trainingLoadGranularitySchema = z.enum(TRAINING_LOAD_GRANULARITIES);

export type TrainingLoadGranularity = z.infer<typeof trainingLoadGranularitySchema>;

export function isTrainingLoadGranularity(value: string): value is TrainingLoadGranularity {
  return (TRAINING_LOAD_GRANULARITIES as readonly string[]).includes(value);
}

export const trainingLoadHistorySchema = z
  .object({
    granularity: trainingLoadGranularitySchema,
    occurrences: z.array(occurrenceTrainingLoadHistoryItemSchema).optional(),
    dailySummaries: z.array(dailyTrainingLoadSummarySchema).optional(),
    weeklySummaries: z.array(weeklyTrainingLoadSummarySchema).optional(),
    page: z.number(),
    size: z.number(),
    totalElements: z.number(),
    totalPages: z.number(),
  })
  .passthrough();

export type TrainingLoadHistory = z.infer<typeof trainingLoadHistorySchema>;

export function getNextHistoryPage(page: number, totalPages: number): number | undefined {
  if (page + 1 >= totalPages) {
    return undefined;
  }
  return page + 1;
}
