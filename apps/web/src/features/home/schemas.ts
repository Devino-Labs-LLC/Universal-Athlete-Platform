import { z } from 'zod';

import type { DateOnly } from '@/core/date/dateOnly';

export const trainingClientBootstrapSchema = z
  .object({
    clientContractVersion: z.string(),
  })
  .passthrough();

export type TrainingClientBootstrap = z.infer<typeof trainingClientBootstrapSchema>;

const bigDecimalLike = z
  .union([z.number(), z.string()])
  .transform(Number)
  .nullable()
  .optional();

const actionFlagSchema = z
  .object({
    allowed: z.boolean(),
    reasonCode: z.string().nullable().optional(),
  })
  .passthrough();

export type TrainingActionFlag = z.infer<typeof actionFlagSchema>;

const trainingDashboardAthleteSchema = z
  .object({
    athleteId: z.string(),
    displayName: z.string(),
  })
  .passthrough();

const trainingDashboardRecoverySchema = z
  .object({
    checkInPresent: z.boolean(),
    recoveryCheckInId: z.string().nullable().optional(),
    completeness: z.string().nullable().optional(),
    discomfortPresent: z.boolean().optional(),
    fatigue: z.number().nullable().optional(),
    muscleSoreness: z.number().nullable().optional(),
    stress: z.number().nullable().optional(),
    mood: z.number().nullable().optional(),
    motivation: z.number().nullable().optional(),
    sleepDurationMinutes: z.number().nullable().optional(),
    sleepQuality: z.number().nullable().optional(),
  })
  .passthrough();

const trainingDashboardAthleteStateSchema = z
  .object({
    snapshotPresent: z.boolean(),
    dailyAthleteStateSnapshotId: z.string().nullable().optional(),
    snapshotVersion: z.number().nullable().optional(),
  })
  .passthrough();

const trainingDashboardReadinessSchema = z
  .object({
    readinessPresent: z.boolean(),
    readinessAssessmentId: z.string().nullable().optional(),
    readinessScore: bigDecimalLike,
    readinessBand: z.string().nullable().optional(),
    dataSufficiency: z.string().nullable().optional(),
    limitingDimensions: z.array(z.string()).nullable().optional(),
  })
  .passthrough();

const trainingDashboardRecommendationSchema = z
  .object({
    recommendationPresent: z.boolean(),
    recommendationId: z.string().nullable().optional(),
    overallAction: z.string().nullable().optional(),
    recommendationStatus: z.string().nullable().optional(),
    adjustmentTypes: z.array(z.string()).nullable().optional(),
  })
  .passthrough();

const trainingDashboardOccurrenceSchema = z
  .object({
    occurrenceId: z.string(),
    trainingPlanId: z.string(),
    workoutDayId: z.string(),
    trainingPlanName: z.string(),
    workoutDayName: z.string(),
    status: z.string(),
    scheduledDate: z.string(),
    exerciseCount: z.number(),
    completedExerciseCount: z.number(),
    startedAt: z.string().nullable().optional(),
    completedAt: z.string().nullable().optional(),
    plannedEnvironmentId: z.string().nullable().optional(),
    plannedEnvironmentName: z.string().nullable().optional(),
    actualEnvironmentId: z.string().nullable().optional(),
    actualEnvironmentName: z.string().nullable().optional(),
    feasibilityStatus: z.string().nullable().optional(),
    activeAdaptationProposalId: z.string().nullable().optional(),
  })
  .passthrough();

export type TrainingDashboardOccurrence = z.infer<typeof trainingDashboardOccurrenceSchema>;

const trainingDashboardTrainingSchema = z
  .object({
    scheduledOccurrenceCount: z.number(),
    modifiableOccurrenceCount: z.number().optional(),
    completedOccurrenceCount: z.number().optional(),
    inProgressOccurrenceCount: z.number().optional(),
    occurrences: z.array(trainingDashboardOccurrenceSchema).optional(),
    primaryOccurrence: trainingDashboardOccurrenceSchema.nullable().optional(),
  })
  .passthrough();

const trainingDashboardLoadSchema = z
  .object({
    loadPresent: z.boolean(),
    occurrenceCount: z.number().optional(),
    ratedOccurrenceCount: z.number().optional(),
    unratedOccurrenceCount: z.number().optional(),
    completedExerciseCount: z.number().optional(),
    completedSetCount: z.number().optional(),
    totalVolumeKilograms: bigDecimalLike,
    totalDurationSeconds: z.number().nullable().optional(),
    totalDistanceMeters: bigDecimalLike,
    totalSessionRpeLoad: bigDecimalLike,
    averageSessionRpe: bigDecimalLike,
  })
  .passthrough();

const trainingDashboardAdaptationSchema = z
  .object({
    activeProposalPresent: z.boolean(),
    adaptationProposalId: z.string().nullable().optional(),
    status: z.string().nullable().optional(),
    origin: z.string().nullable().optional(),
    unresolvedCount: z.number().optional(),
    occurrenceId: z.string().nullable().optional(),
  })
  .passthrough();

const trainingDashboardPersonalRecordSchema = z
  .object({
    personalRecordId: z.string(),
    exercisePerformanceKey: z.string().optional(),
    exerciseName: z.string(),
    recordType: z.string(),
    recordQualifier: z.string().nullable().optional(),
    normalizedValue: bigDecimalLike,
    normalizedUnit: z.string().nullable().optional(),
    achievedAt: z.string().nullable().optional(),
    scheduledDate: z.string().nullable().optional(),
    sourceOccurrenceId: z.string().nullable().optional(),
  })
  .passthrough();

export type TrainingDashboardPersonalRecord = z.infer<
  typeof trainingDashboardPersonalRecordSchema
>;

const trainingDashboardActionsSchema = z
  .object({
    canCreateRecoveryCheckIn: actionFlagSchema.optional(),
    canUpdateRecoveryCheckIn: actionFlagSchema.optional(),
    canGenerateAthleteStateSnapshot: actionFlagSchema.optional(),
    canGenerateReadinessAssessment: actionFlagSchema.optional(),
    canGenerateTrainingRecommendation: actionFlagSchema.optional(),
    canGenerateAdaptationProposal: actionFlagSchema.optional(),
    canStartWorkout: actionFlagSchema.optional(),
    canContinueWorkout: actionFlagSchema.optional(),
    canSubmitSessionEffort: actionFlagSchema.optional(),
  })
  .passthrough();

export const todayDashboardSchema = z
  .object({
    date: z.string(),
    athlete: trainingDashboardAthleteSchema.optional(),
    recovery: trainingDashboardRecoverySchema,
    athleteState: trainingDashboardAthleteStateSchema.optional(),
    readiness: trainingDashboardReadinessSchema,
    recommendation: trainingDashboardRecommendationSchema,
    training: trainingDashboardTrainingSchema,
    trainingLoad: trainingDashboardLoadSchema.optional(),
    adaptation: trainingDashboardAdaptationSchema.optional(),
    recentPerformance: z.array(trainingDashboardPersonalRecordSchema).optional(),
    actions: trainingDashboardActionsSchema.optional(),
  })
  .passthrough();

export type TodayDashboard = z.infer<typeof todayDashboardSchema>;

export const EXPECTED_CLIENT_CONTRACT_VERSION = 'V1';

export const BOOTSTRAP_PATH = '/api/v1/training/client/bootstrap';
export const TODAY_PATH = '/api/v1/training/client/today';

export function deriveTrainingOccurrenceCount(data: TodayDashboard): number {
  return data.training?.scheduledOccurrenceCount ?? 0;
}

export function deriveReadinessBand(data: TodayDashboard): string | null {
  if (!data.readiness?.readinessPresent) {
    return null;
  }
  return data.readiness.readinessBand ?? null;
}

export function deriveRecommendationAction(data: TodayDashboard): string | null {
  if (!data.recommendation?.recommendationPresent) {
    return null;
  }
  return data.recommendation.overallAction ?? null;
}

export function todayQueryDate(date?: DateOnly): string {
  return date ?? 'current';
}

/** @deprecated Use todayDashboardSchema — kept for W1 test compatibility */
export { todayDashboardSchema as trainingTodayDashboardSchema };
