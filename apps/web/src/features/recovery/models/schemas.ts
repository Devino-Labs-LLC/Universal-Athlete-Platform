import { z } from 'zod';

const bigDecimalLike = z.union([z.number(), z.string()]).transform(Number).nullable().optional();

const ratingResponseSchema = z
  .object({
    value: z.number(),
    label: z.string(),
  })
  .passthrough();

export type TrendDays = 7 | 14 | 28;
export const TREND_DAYS_OPTIONS: TrendDays[] = [7, 14, 28];

export type BaselineWindowDays = 7 | 14 | 28;
export const BASELINE_WINDOW_OPTIONS: BaselineWindowDays[] = [7, 14, 28];

export const RECOVERY_METRIC_TYPES = [
  'SLEEP_DURATION',
  'SLEEP_QUALITY',
  'FATIGUE',
  'MUSCLE_SORENESS',
  'STRESS',
  'MOOD',
  'MOTIVATION',
] as const;
export type RecoveryMetricType = (typeof RECOVERY_METRIC_TYPES)[number];

export function isRecoveryMetricType(value: string): value is RecoveryMetricType {
  return (RECOVERY_METRIC_TYPES as readonly string[]).includes(value);
}

export function isTrendDays(value: number): value is TrendDays {
  return TREND_DAYS_OPTIONS.includes(value as TrendDays);
}

export function isBaselineWindowDays(value: number): value is BaselineWindowDays {
  return BASELINE_WINDOW_OPTIONS.includes(value as BaselineWindowDays);
}

export const bodyAreaDiscomfortSchema = z
  .object({
    bodyArea: z.string(),
    side: z.string(),
    intensity: ratingResponseSchema,
    notes: z.string().nullable().optional(),
    orderIndex: z.number().optional(),
  })
  .passthrough();
export type BodyAreaDiscomfort = z.infer<typeof bodyAreaDiscomfortSchema>;

export const dailyRecoveryCheckInSchema = z
  .object({
    id: z.string(),
    checkInDate: z.string(),
    sleepDurationMinutes: z.number().nullable().optional(),
    sleepQuality: ratingResponseSchema.nullable().optional(),
    fatigue: ratingResponseSchema,
    muscleSoreness: ratingResponseSchema,
    stress: ratingResponseSchema,
    mood: ratingResponseSchema,
    motivation: ratingResponseSchema,
    completeness: z.string(),
    discomfortAreas: z.array(bodyAreaDiscomfortSchema),
    notes: z.string().nullable().optional(),
    source: z.string().optional(),
    submittedAt: z.string().optional(),
    createdAt: z.string().optional(),
    updatedAt: z.string().optional(),
    version: z.number(),
  })
  .passthrough();
export type DailyRecoveryCheckIn = z.infer<typeof dailyRecoveryCheckInSchema>;

export const dailyRecoveryCheckInListSchema = z
  .object({
    checkIns: z.array(dailyRecoveryCheckInSchema),
    page: z.number(),
    size: z.number(),
    totalElements: z.number(),
    totalPages: z.number(),
  })
  .passthrough();
export type DailyRecoveryCheckInList = z.infer<typeof dailyRecoveryCheckInListSchema>;

export const recoveryCheckInRevisionSchema = z
  .object({
    id: z.string().optional(),
    recoveryCheckInId: z.string().optional(),
    revisionNumber: z.number().optional(),
    priorCompleteness: z.string().nullable().optional(),
    newCompleteness: z.string().nullable().optional(),
    priorNotes: z.string().nullable().optional(),
    newNotes: z.string().nullable().optional(),
    changedAt: z.string().optional(),
    createdAt: z.string().optional(),
  })
  .passthrough();
export type RecoveryCheckInRevision = z.infer<typeof recoveryCheckInRevisionSchema>;

export const recoveryCheckInRevisionListSchema = z
  .object({
    revisions: z.array(recoveryCheckInRevisionSchema),
  })
  .passthrough();

export const recoveryMetricBaselineSchema = z
  .object({
    metricType: z.string(),
    scaleDirection: z.string(),
    windowDays: z.number(),
    windowStartDate: z.string(),
    windowEndDate: z.string(),
    observationCount: z.number(),
    dataSufficiency: z.string(),
    mean: bigDecimalLike,
    median: bigDecimalLike,
    minimum: bigDecimalLike,
    maximum: bigDecimalLike,
    standardDeviation: bigDecimalLike,
    firstObservationDate: z.string().nullable().optional(),
    lastObservationDate: z.string().nullable().optional(),
    calculatedAt: z.string().optional(),
  })
  .passthrough();
export type RecoveryMetricBaseline = z.infer<typeof recoveryMetricBaselineSchema>;

export const recoveryMetricDeviationSchema = z
  .object({
    metricType: z.string(),
    scaleDirection: z.string(),
    targetValue: bigDecimalLike,
    baseline: recoveryMetricBaselineSchema.optional(),
    absoluteDifference: bigDecimalLike,
    percentageDifference: bigDecimalLike,
    standardizedDeviation: bigDecimalLike,
    comparisonBand: z.string(),
    dataSufficiency: z.string(),
    reasonCode: z.string().nullable().optional(),
  })
  .passthrough();
export type RecoveryMetricDeviation = z.infer<typeof recoveryMetricDeviationSchema>;

const recoveryOverviewCheckInSchema = z
  .object({
    recoveryCheckInId: z.string(),
    completeness: z.string(),
    fatigue: z.number().nullable().optional(),
    muscleSoreness: z.number().nullable().optional(),
    stress: z.number().nullable().optional(),
    mood: z.number().nullable().optional(),
    motivation: z.number().nullable().optional(),
    sleepDurationMinutes: z.number().nullable().optional(),
    sleepQuality: z.number().nullable().optional(),
    discomfortPresent: z.boolean(),
  })
  .passthrough();

const recoveryOverviewReadinessSchema = z
  .object({
    readinessAssessmentId: z.string(),
    readinessScore: bigDecimalLike,
    readinessBand: z.string(),
    dataSufficiency: z.string(),
    limitingDimensions: z.array(z.string()),
  })
  .passthrough();

const recoveryOverviewRecommendationSchema = z
  .object({
    recommendationId: z.string(),
    overallAction: z.string(),
    recommendationStatus: z.string(),
    adjustmentTypes: z.array(z.string()),
  })
  .passthrough();

const recoveryOverviewTrendSchema = z
  .object({
    metricType: z.string(),
    trendDirection: z.string(),
    observationCount: z.number(),
  })
  .passthrough();

const recoveryOverviewDiscomfortSchema = z
  .object({
    bodyArea: z.string(),
    bodySide: z.string(),
    intensity: z.number(),
    notes: z.string().nullable().optional(),
    orderIndex: z.number(),
  })
  .passthrough();

export const recoveryTrainingLoadContextSchema = z
  .object({
    date: z.string(),
    occurrenceCount: z.number(),
    ratedOccurrenceCount: z.number(),
    unratedOccurrenceCount: z.number(),
    completedExerciseCount: z.number(),
    completedSetCount: z.number(),
    totalVolumeKilograms: bigDecimalLike,
    totalDurationSeconds: z.number(),
    totalDistanceMeters: bigDecimalLike,
    totalSessionRpeLoad: bigDecimalLike,
  })
  .passthrough()
  .nullable()
  .optional();
export type RecoveryTrainingLoadContext = z.infer<typeof recoveryTrainingLoadContextSchema>;

export const recoveryOverviewSchema = z
  .object({
    date: z.string(),
    trendDays: z.number(),
    checkInPresent: z.boolean(),
    checkIn: recoveryOverviewCheckInSchema.nullable().optional(),
    baselines: z.array(recoveryMetricBaselineSchema),
    deviations: z.array(recoveryMetricDeviationSchema),
    readinessPresent: z.boolean(),
    readiness: recoveryOverviewReadinessSchema.nullable().optional(),
    recommendationPresent: z.boolean(),
    recommendation: recoveryOverviewRecommendationSchema.nullable().optional(),
    trends: z.array(recoveryOverviewTrendSchema),
    discomfort: z.array(recoveryOverviewDiscomfortSchema),
    trainingLoadContext: recoveryTrainingLoadContextSchema,
  })
  .passthrough();
export type RecoveryOverview = z.infer<typeof recoveryOverviewSchema>;

export const athleteRecoveryHistoryDaySchema = z
  .object({
    date: z.string(),
    checkIn: dailyRecoveryCheckInSchema.nullable().optional(),
    trainingLoad: recoveryTrainingLoadContextSchema,
    revisionCount: z.number().optional(),
    lastUpdatedAt: z.string().optional(),
  })
  .passthrough();
export type AthleteRecoveryHistoryDay = z.infer<typeof athleteRecoveryHistoryDaySchema>;

export const athleteRecoveryHistorySchema = z
  .object({
    days: z.array(athleteRecoveryHistoryDaySchema),
  })
  .passthrough();
export type AthleteRecoveryHistory = z.infer<typeof athleteRecoveryHistorySchema>;

const recoveryDashboardTrendSummarySchema = z
  .object({
    metricType: z.string(),
    scaleDirection: z.string(),
    trendDirection: z.string(),
    observationCount: z.number(),
  })
  .passthrough();
export type RecoveryDashboardTrendSummary = z.infer<typeof recoveryDashboardTrendSummarySchema>;

export const recoveryBaselineDashboardSchema = z
  .object({
    targetDate: z.string(),
    checkInPresent: z.boolean(),
    checkIn: dailyRecoveryCheckInSchema.nullable().optional(),
    baselineWindowDays: z.number(),
    baselines: z.array(recoveryMetricBaselineSchema),
    metricDeviations: z.array(recoveryMetricDeviationSchema),
    metricTrends: z.array(recoveryDashboardTrendSummarySchema),
    trainingLoadContext: recoveryTrainingLoadContextSchema,
    calculatedAt: z.string().optional(),
  })
  .passthrough();
export type RecoveryBaselineDashboard = z.infer<typeof recoveryBaselineDashboardSchema>;

const trendPointValueSchema = z.union([ratingResponseSchema, bigDecimalLike]).nullable().optional();

const recoveryMetricTrendPointSchema = z
  .object({
    date: z.string(),
    checkInId: z.string().nullable().optional(),
    value: trendPointValueSchema,
    rollingAverage3: bigDecimalLike,
    rollingAverage7: bigDecimalLike,
    trainingLoadContext: recoveryTrainingLoadContextSchema,
  })
  .passthrough();
export type RecoveryMetricTrendPoint = z.infer<typeof recoveryMetricTrendPointSchema>;

export const recoveryMetricTrendSchema = z
  .object({
    metricType: z.string(),
    scaleDirection: z.string(),
    startDate: z.string(),
    endDate: z.string(),
    observationCount: z.number(),
    trendDirection: z.string(),
    trendReasonCode: z.string().nullable().optional(),
    points: z.array(recoveryMetricTrendPointSchema),
  })
  .passthrough();
export type RecoveryMetricTrend = z.infer<typeof recoveryMetricTrendSchema>;

/** Extracts a numeric point value whether the backend sent a rating object or a bare number (SLEEP_DURATION). */
export function trendPointNumericValue(value: RecoveryMetricTrendPoint['value']): number | null {
  if (value == null) {
    return null;
  }
  if (typeof value === 'object' && 'value' in value) {
    return value.value;
  }
  return typeof value === 'number' ? value : Number(value);
}

const bodyAreaDiscomfortHistoryEntrySchema = z
  .object({
    date: z.string(),
    checkInId: z.string().nullable().optional(),
    bodyArea: z.string(),
    side: z.string(),
    intensity: ratingResponseSchema.nullable().optional(),
    notes: z.string().nullable().optional(),
    checkInVersion: z.number().optional(),
  })
  .passthrough();
export type BodyAreaDiscomfortHistoryEntry = z.infer<typeof bodyAreaDiscomfortHistoryEntrySchema>;

export const bodyAreaDiscomfortHistorySchema = z
  .object({
    startDate: z.string(),
    endDate: z.string(),
    observationCount: z.number(),
    datesObserved: z.number().optional(),
    averageIntensity: bigDecimalLike,
    maximumIntensity: bigDecimalLike,
    latestObservationDate: z.string().nullable().optional(),
    entries: z.array(bodyAreaDiscomfortHistoryEntrySchema),
  })
  .passthrough();
export type BodyAreaDiscomfortHistory = z.infer<typeof bodyAreaDiscomfortHistorySchema>;

const readinessContributionSchema = z
  .object({
    dimensionType: z.string(),
    sourceMetricType: z.string().nullable().optional(),
    available: z.boolean(),
    baselineSufficiency: z.string().nullable().optional(),
    targetValue: bigDecimalLike,
    baselineMean: bigDecimalLike,
    standardizedDeviation: bigDecimalLike,
    comparisonBand: z.string().nullable().optional(),
    normalizedScore: bigDecimalLike,
    configuredWeight: bigDecimalLike,
    effectiveWeight: bigDecimalLike,
    weightedContribution: bigDecimalLike,
    reasonCode: z.string().nullable().optional(),
    rankAsLimiting: z.number().nullable().optional(),
    rankAsStrongest: z.number().nullable().optional(),
  })
  .passthrough();
export type ReadinessContribution = z.infer<typeof readinessContributionSchema>;

const readinessContextSchema = z
  .object({
    discomfortPresent: z.boolean(),
    discomfortObservations: z.array(bodyAreaDiscomfortSchema).optional(),
    totalVolumeKilograms: bigDecimalLike,
    totalDistanceMeters: bigDecimalLike,
    totalDurationSeconds: z.number().optional(),
    totalSessionRpeLoad: bigDecimalLike,
    scheduledOccurrenceCount: z.number().optional(),
    completedScheduledCount: z.number().optional(),
    inProgressScheduledCount: z.number().optional(),
    skippedScheduledCount: z.number().optional(),
    cancelledScheduledCount: z.number().optional(),
  })
  .passthrough()
  .optional();

export const dailyReadinessAssessmentSchema = z
  .object({
    assessmentId: z.string(),
    stateDate: z.string(),
    dailyAthleteStateSnapshotId: z.string().optional(),
    dailyAthleteStateSnapshotVersion: z.number().optional(),
    algorithmVersion: z.string().optional(),
    readinessScore: bigDecimalLike,
    readinessBand: z.string(),
    dataSufficiency: z.string(),
    summaryReasonCode: z.string().nullable().optional(),
    limitingDimensionCount: z.number().optional(),
    contributingDimensionCount: z.number().optional(),
    limitingDimensions: z.array(z.string()),
    strongestDimensions: z.array(z.string()),
    contributions: z.array(readinessContributionSchema),
    context: readinessContextSchema,
    assessedAt: z.string().optional(),
    createdAt: z.string().optional(),
    newlyCreated: z.boolean().optional(),
  })
  .passthrough();
export type DailyReadinessAssessment = z.infer<typeof dailyReadinessAssessmentSchema>;

export const dailyReadinessHistoryItemSchema = z
  .object({
    assessmentId: z.string(),
    stateDate: z.string(),
    dailyAthleteStateSnapshotId: z.string().optional(),
    dailyAthleteStateSnapshotVersion: z.number().optional(),
    currentSnapshot: z.boolean().optional(),
    algorithmVersion: z.string().optional(),
    readinessScore: bigDecimalLike,
    readinessBand: z.string(),
    dataSufficiency: z.string(),
    summaryReasonCode: z.string().nullable().optional(),
    assessedAt: z.string().optional(),
  })
  .passthrough();
export type DailyReadinessHistoryItem = z.infer<typeof dailyReadinessHistoryItemSchema>;

export const dailyReadinessHistorySchema = z
  .object({
    content: z.array(dailyReadinessHistoryItemSchema),
    page: z.number(),
    size: z.number(),
    totalElements: z.number(),
  })
  .passthrough();
export type DailyReadinessHistory = z.infer<typeof dailyReadinessHistorySchema>;

const readinessDimensionDifferenceSchema = z
  .object({
    dimensionType: z.string(),
    olderNormalizedScore: bigDecimalLike,
    newerNormalizedScore: bigDecimalLike,
    olderReasonCode: z.string().nullable().optional(),
    newerReasonCode: z.string().nullable().optional(),
    olderComparisonBand: z.string().nullable().optional(),
    newerComparisonBand: z.string().nullable().optional(),
  })
  .passthrough();

export const dailyReadinessAssessmentComparisonSchema = z
  .object({
    olderAssessmentId: z.string(),
    newerAssessmentId: z.string(),
    olderStateDate: z.string(),
    newerStateDate: z.string(),
    olderScore: bigDecimalLike,
    newerScore: bigDecimalLike,
    scoreDelta: bigDecimalLike,
    scoreDirection: z.string().optional(),
    bandChanged: z.boolean(),
    olderBand: z.string(),
    newerBand: z.string(),
    dataSufficiencyChanged: z.boolean(),
    olderDataSufficiency: z.string(),
    newerDataSufficiency: z.string(),
    limitingDimensionsChanged: z.boolean(),
    olderLimitingDimensions: z.array(z.string()),
    newerLimitingDimensions: z.array(z.string()),
    dimensionChanges: z.array(readinessDimensionDifferenceSchema),
  })
  .passthrough();
export type DailyReadinessAssessmentComparison = z.infer<typeof dailyReadinessAssessmentComparisonSchema>;

const trainingRecommendationAdjustmentSchema = z
  .object({
    adjustmentId: z.string(),
    type: z.string(),
    priority: z.number(),
    reasonCodes: z.array(z.string()).optional(),
    sourceDimensions: z.array(z.string()).optional(),
    explanationKey: z.string().nullable().optional(),
    orderIndex: z.number(),
  })
  .passthrough();
export type TrainingRecommendationAdjustment = z.infer<typeof trainingRecommendationAdjustmentSchema>;

const trainingRecommendationOccurrenceSchema = z
  .object({
    occurrenceId: z.string(),
    trainingPlanId: z.string(),
    workoutDayId: z.string(),
    occurrenceStatus: z.string(),
    modifiable: z.boolean(),
    plannedEnvironmentNameSnapshot: z.string().nullable().optional(),
    actualEnvironmentNameSnapshot: z.string().nullable().optional(),
    orderIndex: z.number(),
  })
  .passthrough();
export type TrainingRecommendationOccurrence = z.infer<typeof trainingRecommendationOccurrenceSchema>;

export const dailyTrainingRecommendationSchema = z
  .object({
    recommendationId: z.string(),
    stateDate: z.string(),
    dailyReadinessAssessmentId: z.string().optional(),
    dailyAthleteStateSnapshotId: z.string().optional(),
    dailyAthleteStateSnapshotVersion: z.number().optional(),
    recommendationAlgorithmVersion: z.string().optional(),
    overallAction: z.string(),
    recommendationStatus: z.string(),
    primaryReasonCode: z.string().nullable().optional(),
    readinessBand: z.string().nullable().optional(),
    readinessScore: bigDecimalLike,
    scheduledTrainingPresent: z.boolean().optional(),
    scheduledOccurrenceCount: z.number().optional(),
    modifiableScheduledOccurrenceCount: z.number().optional(),
    adjustmentCount: z.number().optional(),
    limitingDimensionCount: z.number().optional(),
    limitingDimensions: z.array(z.string()).optional(),
    adjustments: z.array(trainingRecommendationAdjustmentSchema),
    scheduledOccurrences: z.array(trainingRecommendationOccurrenceSchema).optional(),
    generatedAt: z.string().optional(),
    createdAt: z.string().optional(),
    newlyCreated: z.boolean().optional(),
  })
  .passthrough();
export type DailyTrainingRecommendation = z.infer<typeof dailyTrainingRecommendationSchema>;

export const dailyTrainingRecommendationHistoryItemSchema = z
  .object({
    recommendationId: z.string(),
    stateDate: z.string(),
    dailyReadinessAssessmentId: z.string().optional(),
    dailyAthleteStateSnapshotId: z.string().optional(),
    dailyAthleteStateSnapshotVersion: z.number().optional(),
    currentSnapshot: z.boolean().optional(),
    recommendationAlgorithmVersion: z.string().optional(),
    overallAction: z.string(),
    recommendationStatus: z.string(),
    primaryReasonCode: z.string().nullable().optional(),
    adjustmentCount: z.number().optional(),
    generatedAt: z.string().optional(),
  })
  .passthrough();
export type DailyTrainingRecommendationHistoryItem = z.infer<
  typeof dailyTrainingRecommendationHistoryItemSchema
>;

export const dailyTrainingRecommendationHistorySchema = z
  .object({
    content: z.array(dailyTrainingRecommendationHistoryItemSchema),
    page: z.number(),
    size: z.number(),
    totalElements: z.number(),
  })
  .passthrough();
export type DailyTrainingRecommendationHistory = z.infer<
  typeof dailyTrainingRecommendationHistorySchema
>;

export const dailyTrainingRecommendationComparisonSchema = z
  .object({
    olderRecommendationId: z.string(),
    newerRecommendationId: z.string(),
    olderStateDate: z.string(),
    newerStateDate: z.string(),
    actionChanged: z.boolean(),
    priorAction: z.string(),
    newAction: z.string(),
    adjustmentsAdded: z.array(z.string()),
    adjustmentsRemoved: z.array(z.string()),
    limitingDimensionsChanged: z.boolean(),
    olderLimitingDimensions: z.array(z.string()),
    newerLimitingDimensions: z.array(z.string()),
  })
  .passthrough();
export type DailyTrainingRecommendationComparison = z.infer<
  typeof dailyTrainingRecommendationComparisonSchema
>;

const discomfortObservationSchema = z
  .object({
    bodyArea: z.string(),
    bodySide: z.string(),
    intensity: z.number(),
    notes: z.string().nullable().optional(),
    orderIndex: z.number().optional(),
  })
  .passthrough();

const recoveryMetricSnapshotSchema = z
  .object({
    metricType: z.string(),
    targetValue: bigDecimalLike,
    metricDirection: z.string().optional(),
    observationCount: z.number(),
    dataSufficiency: z.string(),
    baselineMean: bigDecimalLike,
    baselineMedian: bigDecimalLike,
    baselineMinimum: bigDecimalLike,
    baselineMaximum: bigDecimalLike,
    baselineStandardDeviation: bigDecimalLike,
    absoluteDifference: bigDecimalLike,
    percentageDifference: bigDecimalLike,
    standardizedDeviation: bigDecimalLike,
    comparisonBand: z.string(),
    reasonCode: z.string().nullable().optional(),
  })
  .passthrough();
export type RecoveryMetricSnapshot = z.infer<typeof recoveryMetricSnapshotSchema>;

const athleteStateRecoverySchema = z
  .object({
    checkInPresent: z.boolean(),
    recoveryCheckInId: z.string().nullable().optional(),
    recoveryCheckInVersion: z.number().nullable().optional(),
    sleepDurationMinutes: z.number().nullable().optional(),
    sleepQuality: z.number().nullable().optional(),
    fatigue: z.number().nullable().optional(),
    muscleSoreness: z.number().nullable().optional(),
    stress: z.number().nullable().optional(),
    mood: z.number().nullable().optional(),
    motivation: z.number().nullable().optional(),
    checkInSubmittedAt: z.string().nullable().optional(),
    checkInLastUpdatedAt: z.string().nullable().optional(),
    discomfortObservations: z.array(discomfortObservationSchema),
  })
  .passthrough();

const categorySummarySchema = z
  .object({
    category: z.string(),
    completedExerciseCount: z.number(),
    completedSetCount: z.number(),
    volumeKilograms: bigDecimalLike,
    durationSeconds: z.number(),
    distanceMeters: bigDecimalLike,
  })
  .passthrough();

const movementSummarySchema = z
  .object({
    movementPattern: z.string(),
    completedExerciseCount: z.number(),
    completedSetCount: z.number(),
    completedRepetitionCount: z.number(),
    volumeKilograms: bigDecimalLike,
    durationSeconds: z.number(),
    distanceMeters: bigDecimalLike,
  })
  .passthrough();

const athleteStateTrainingLoadSchema = z
  .object({
    occurrenceCount: z.number(),
    completedOccurrenceCount: z.number(),
    ratedOccurrenceCount: z.number(),
    unratedOccurrenceCount: z.number(),
    completedExerciseCount: z.number(),
    completedSetCount: z.number(),
    completedRepetitionCount: z.number(),
    totalVolumeKilograms: bigDecimalLike,
    totalDurationSeconds: z.number(),
    totalDistanceMeters: bigDecimalLike,
    totalSessionRpeLoad: bigDecimalLike,
    averageSessionRpe: bigDecimalLike,
    totalSessionDurationMinutes: z.number(),
    noImpactExerciseCount: z.number(),
    lowImpactExerciseCount: z.number(),
    moderateImpactExerciseCount: z.number(),
    highImpactExerciseCount: z.number(),
    categorySummaries: z.array(categorySummarySchema),
    movementSummaries: z.array(movementSummarySchema),
  })
  .passthrough();

const scheduledOccurrenceSchema = z
  .object({
    occurrenceId: z.string(),
    trainingPlanId: z.string(),
    workoutDayId: z.string(),
    scheduledDate: z.string(),
    occurrenceStatus: z.string(),
    plannedEnvironmentNameSnapshot: z.string().nullable().optional(),
    actualEnvironmentNameSnapshot: z.string().nullable().optional(),
    orderIndex: z.number(),
  })
  .passthrough();

const athleteStateScheduleSchema = z
  .object({
    scheduledOccurrenceCount: z.number(),
    scheduledWorkoutCount: z.number(),
    completedScheduledCount: z.number(),
    skippedScheduledCount: z.number(),
    cancelledScheduledCount: z.number(),
    inProgressScheduledCount: z.number(),
    scheduledOccurrences: z.array(scheduledOccurrenceSchema),
  })
  .passthrough();

export const dailyAthleteStateSnapshotSchema = z
  .object({
    snapshotId: z.string(),
    stateDate: z.string(),
    snapshotVersion: z.number(),
    current: z.boolean(),
    changed: z.boolean().optional(),
    generationReason: z.string().optional(),
    generatedAt: z.string().optional(),
    completeness: z.string().optional(),
    baselineWindowDays: z.number().optional(),
    recoveryAnalyticsCalculationVersion: z.string().optional(),
    recovery: athleteStateRecoverySchema,
    recoveryMetrics: z.array(recoveryMetricSnapshotSchema),
    trainingLoad: athleteStateTrainingLoadSchema,
    schedule: athleteStateScheduleSchema,
    createdAt: z.string().optional(),
  })
  .passthrough();
export type DailyAthleteStateSnapshot = z.infer<typeof dailyAthleteStateSnapshotSchema>;

export const dailyAthleteStateSnapshotVersionSchema = z
  .object({
    snapshotId: z.string(),
    stateDate: z.string(),
    snapshotVersion: z.number(),
    current: z.boolean(),
    generatedAt: z.string().optional(),
    generationReason: z.string().optional(),
    baselineWindowDays: z.number().optional(),
    completeness: z.string().optional(),
  })
  .passthrough();
export type DailyAthleteStateSnapshotVersion = z.infer<typeof dailyAthleteStateSnapshotVersionSchema>;

export const dailyAthleteStateHistorySchema = z
  .object({
    content: z.array(dailyAthleteStateSnapshotVersionSchema),
    page: z.number(),
    size: z.number(),
    totalElements: z.number(),
  })
  .passthrough();
export type DailyAthleteStateHistory = z.infer<typeof dailyAthleteStateHistorySchema>;

const fieldDifferenceSchema = z
  .object({
    field: z.string(),
    previousValue: z.string().nullable().optional(),
    newValue: z.string().nullable().optional(),
  })
  .passthrough();

export const dailyAthleteStateSnapshotComparisonSchema = z
  .object({
    olderSnapshotId: z.string(),
    newerSnapshotId: z.string(),
    olderStateDate: z.string(),
    newerStateDate: z.string(),
    olderVersion: z.number(),
    newerVersion: z.number(),
    recoveryChanged: z.boolean(),
    baselineChanged: z.boolean(),
    trainingLoadChanged: z.boolean(),
    scheduleChanged: z.boolean(),
    discomfortChanged: z.boolean(),
    fieldDifferences: z.array(fieldDifferenceSchema),
  })
  .passthrough();
export type DailyAthleteStateSnapshotComparison = z.infer<
  typeof dailyAthleteStateSnapshotComparisonSchema
>;

export function discomfortKey(area: string, side: string): string {
  return `${area}:${side}`;
}
