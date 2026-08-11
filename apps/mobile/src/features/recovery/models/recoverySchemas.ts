import { z } from 'zod';

const bigDecimalLike = z
  .union([z.number(), z.string()])
  .transform(Number)
  .nullable()
  .optional();

const ratingResponseSchema = z
  .object({
    value: z.number(),
    label: z.string(),
  })
  .passthrough();

export type TrendDays = 7 | 14 | 28;

export const bodyAreaDiscomfortSchema = z
  .object({
    bodyArea: z.string(),
    side: z.string(),
    intensity: ratingResponseSchema,
    notes: z.string().nullable().optional(),
    orderIndex: z.number().optional(),
  })
  .passthrough();

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

export const discomfortInputSchema = z.object({
  bodyArea: z.string().min(1),
  side: z.string().min(1),
  intensity: z.number().int().min(1).max(5),
  notes: z.string().max(250).optional(),
});

export const createCheckInFormSchema = z.object({
  checkInDate: z.string(),
  fatigue: z.number().int().min(1).max(5),
  muscleSoreness: z.number().int().min(1).max(5),
  stress: z.number().int().min(1).max(5),
  mood: z.number().int().min(1).max(5),
  motivation: z.number().int().min(1).max(5),
  sleepDurationMinutes: z.number().int().min(0).max(1440).optional(),
  sleepQuality: z.number().int().min(1).max(5).optional(),
  discomfortAreas: z.array(discomfortInputSchema).max(20),
  notes: z.string().max(2000).optional(),
});

export type CreateCheckInFormValues = z.infer<typeof createCheckInFormSchema>;

export const createDailyRecoveryCheckInRequestSchema = z.object({
  checkInDate: z.string(),
  fatigue: z.number().int().min(1).max(5),
  muscleSoreness: z.number().int().min(1).max(5),
  stress: z.number().int().min(1).max(5),
  mood: z.number().int().min(1).max(5),
  motivation: z.number().int().min(1).max(5),
  sleepDurationMinutes: z.number().int().min(0).max(1440).optional(),
  sleepQuality: z.number().int().min(1).max(5).optional(),
  discomfortAreas: z.array(discomfortInputSchema).optional(),
  notes: z.string().max(2000).optional(),
});

export type CreateDailyRecoveryCheckInRequest = z.infer<
  typeof createDailyRecoveryCheckInRequestSchema
>;

const recoveryMetricBaselineSchema = z
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

const recoveryMetricDeviationSchema = z
  .object({
    metricType: z.string(),
    scaleDirection: z.string(),
    targetValue: z.unknown().optional(),
    baseline: recoveryMetricBaselineSchema.optional(),
    absoluteDifference: bigDecimalLike,
    percentageDifference: bigDecimalLike,
    standardizedDeviation: bigDecimalLike,
    comparisonBand: z.string(),
    dataSufficiency: z.string(),
    reasonCode: z.string().nullable().optional(),
  })
  .passthrough();

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

const recoveryTrainingLoadContextSchema = z
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
    checkIn: dailyRecoveryCheckInSchema,
    trainingLoad: recoveryTrainingLoadContextSchema,
    revisionCount: z.number(),
    lastUpdatedAt: z.string(),
  })
  .passthrough();

export const athleteRecoveryHistorySchema = z
  .object({
    days: z.array(athleteRecoveryHistoryDaySchema),
  })
  .passthrough();

export type AthleteRecoveryHistory = z.infer<typeof athleteRecoveryHistorySchema>;

const readinessContributionSchema = z
  .object({
    dimensionType: z.string(),
    sourceMetricType: z.string().optional(),
    available: z.boolean(),
    baselineSufficiency: z.string().optional(),
    targetValue: bigDecimalLike,
    baselineMean: bigDecimalLike,
    standardizedDeviation: bigDecimalLike,
    comparisonBand: z.string().optional(),
    normalizedScore: bigDecimalLike,
    configuredWeight: bigDecimalLike,
    effectiveWeight: bigDecimalLike,
    weightedContribution: bigDecimalLike,
    reasonCode: z.string().nullable().optional(),
    rankAsLimiting: z.number().nullable().optional(),
    rankAsStrongest: z.number().nullable().optional(),
  })
  .passthrough();

export const dailyReadinessAssessmentSchema = z
  .object({
    assessmentId: z.string(),
    stateDate: z.string(),
    readinessScore: bigDecimalLike,
    readinessBand: z.string(),
    dataSufficiency: z.string(),
    limitingDimensions: z.array(z.string()),
    strongestDimensions: z.array(z.string()),
    contributions: z.array(readinessContributionSchema),
    assessedAt: z.string().optional(),
    createdAt: z.string().optional(),
    newlyCreated: z.boolean().optional(),
  })
  .passthrough();

export type DailyReadinessAssessment = z.infer<typeof dailyReadinessAssessmentSchema>;

const trainingRecommendationAdjustmentSchema = z
  .object({
    adjustmentId: z.string(),
    type: z.string(),
    priority: z.number(),
    reasonCodes: z.array(z.string()).optional(),
    sourceDimensions: z.array(z.string()).optional(),
    explanationKey: z.string().optional(),
    orderIndex: z.number(),
  })
  .passthrough();

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

export const dailyTrainingRecommendationSchema = z
  .object({
    recommendationId: z.string(),
    stateDate: z.string(),
    overallAction: z.string(),
    recommendationStatus: z.string(),
    readinessBand: z.string().nullable().optional(),
    readinessScore: bigDecimalLike,
    limitingDimensions: z.array(z.string()).optional(),
    adjustments: z.array(trainingRecommendationAdjustmentSchema),
    scheduledOccurrenceCount: z.number().optional(),
    modifiableScheduledOccurrenceCount: z.number().optional(),
    scheduledOccurrences: z.array(trainingRecommendationOccurrenceSchema).optional(),
    generatedAt: z.string().optional(),
    createdAt: z.string().optional(),
    newlyCreated: z.boolean().optional(),
  })
  .passthrough();

export type DailyTrainingRecommendation = z.infer<typeof dailyTrainingRecommendationSchema>;

export function discomfortKey(area: string, side: string): string {
  return `${area}:${side}`;
}

export function validateDiscomfortUniqueness(
  areas: Array<{ bodyArea: string; side: string }>,
): boolean {
  const keys = new Set<string>();
  for (const item of areas) {
    const key = discomfortKey(item.bodyArea, item.side);
    if (keys.has(key)) {
      return false;
    }
    keys.add(key);
  }
  return true;
}

export function normalizeDiscomfortSide(bodyArea: string, side: string): string {
  if (bodyArea === 'GENERAL_FULL_BODY') {
    return 'NOT_APPLICABLE';
  }
  return side;
}
