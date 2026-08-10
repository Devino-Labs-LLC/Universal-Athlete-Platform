import { z } from 'zod';

const trainingClientBootstrapFeaturesSchema = z
  .object({
    readinessEnabled: z.boolean(),
    recommendationsEnabled: z.boolean(),
    adaptationEnabled: z.boolean(),
    recoveryEnabled: z.boolean(),
    trainingLoadEnabled: z.boolean(),
    environmentsEnabled: z.boolean(),
  })
  .passthrough();

const trainingClientBootstrapLimitsSchema = z
  .object({
    recoveryHistoryMaxDays: z.number(),
    baselineWindows: z.array(z.number()),
    readinessAlgorithmVersion: z.string(),
    recommendationAlgorithmVersion: z.string(),
    maxEnvironmentPageSize: z.number(),
    maxHistoryRangeDays: z.number(),
    recoveryCheckInMaxPastDays: z.number(),
  })
  .passthrough();

const trainingClientBootstrapUnitsSchema = z
  .object({
    canonicalWeightUnit: z.string(),
    distanceUnit: z.string(),
    durationUnit: z.string(),
    trainingLoadUnit: z.string(),
  })
  .passthrough();

const trainingClientBootstrapRatingScalesSchema = z
  .object({
    recoveryRatingMin: z.number(),
    recoveryRatingMax: z.number(),
    sessionRpeMin: z.union([z.number(), z.string()]),
    sessionRpeMax: z.union([z.number(), z.string()]),
  })
  .passthrough();

export const trainingClientBootstrapSchema = z
  .object({
    clientContractVersion: z.string(),
    features: trainingClientBootstrapFeaturesSchema,
    limits: trainingClientBootstrapLimitsSchema,
    units: trainingClientBootstrapUnitsSchema,
    ratingScales: trainingClientBootstrapRatingScalesSchema,
  })
  .passthrough();

export type TrainingClientBootstrap = z.infer<typeof trainingClientBootstrapSchema>;

const trainingDashboardRecoverySchema = z
  .object({
    checkInPresent: z.boolean(),
  })
  .passthrough();

const trainingDashboardReadinessSchema = z
  .object({
    readinessPresent: z.boolean(),
    readinessScore: z.union([z.number(), z.string()]).nullable().optional(),
    readinessBand: z.string().nullable().optional(),
  })
  .passthrough();

const trainingDashboardRecommendationSchema = z
  .object({
    recommendationPresent: z.boolean(),
    overallAction: z.string().nullable().optional(),
    recommendationStatus: z.string().nullable().optional(),
  })
  .passthrough();

const trainingDashboardTrainingSchema = z
  .object({
    scheduledOccurrenceCount: z.number(),
    modifiableOccurrenceCount: z.number().optional(),
    completedOccurrenceCount: z.number().optional(),
    inProgressOccurrenceCount: z.number().optional(),
  })
  .passthrough();

export const trainingTodayDashboardSchema = z
  .object({
    date: z.string(),
    recovery: trainingDashboardRecoverySchema,
    readiness: trainingDashboardReadinessSchema,
    recommendation: trainingDashboardRecommendationSchema,
    training: trainingDashboardTrainingSchema,
  })
  .passthrough();

export type TrainingTodayDashboard = z.infer<typeof trainingTodayDashboardSchema>;
