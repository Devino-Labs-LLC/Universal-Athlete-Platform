import { z } from 'zod';

import { parseDateOnly } from '@/core/date/dateOnly';

export const coachOverviewSectionStatusSchema = z.enum(['NOT_SHARED', 'NO_DATA', 'AVAILABLE']);

export type CoachOverviewSectionStatus = z.infer<typeof coachOverviewSectionStatusSchema>;

export const teamRosterEntrySchema = z.object({
  athleteId: z.string().min(1),
  membershipId: z.string().min(1),
  displayName: z.string().min(1),
  role: z.literal('ATHLETE'),
  status: z.literal('ACTIVE'),
});

export type TeamRosterEntry = z.infer<typeof teamRosterEntrySchema>;

export const teamRosterSchema = z.array(teamRosterEntrySchema);

function sectionSchema<T extends z.ZodTypeAny>(dataSchema: T) {
  return z.object({
    status: coachOverviewSectionStatusSchema,
    data: dataSchema.nullable(),
  });
}

export const ratingValueSchema = z.object({
  value: z.number().int(),
  label: z.string().min(1),
});

export type RatingValue = z.infer<typeof ratingValueSchema>;

export const discomfortAreaSchema = z.object({
  bodyArea: z.string().min(1),
  side: z.string().nullable().optional(),
  intensity: ratingValueSchema.nullable().optional(),
  notes: z.string().nullable().optional(),
  orderIndex: z.number().int(),
});

export type DiscomfortArea = z.infer<typeof discomfortAreaSchema>;

const readinessCategoryDataSchema = z.object({
  readinessBand: z.string().min(1),
  dataSufficiency: z.string().min(1),
});

/** Score projection is scope-independent from band — readinessBand is intentionally absent. */
const readinessScoreDataSchema = z.object({
  readinessScore: z.union([z.number(), z.string()]),
  dataSufficiency: z.string().min(1),
  summaryReasonCode: z.string().nullable().optional(),
});

const limitingDimensionsDataSchema = z.object({
  limitingDimensions: z.array(z.string()),
});

const recoveryCheckInDataSchema = z.object({
  checkInDate: z.string().min(1).transform(parseDateOnly),
  sleepQuality: ratingValueSchema.nullable().optional(),
  mood: ratingValueSchema.nullable().optional(),
  fatigue: ratingValueSchema.nullable().optional(),
  muscleSoreness: ratingValueSchema.nullable().optional(),
  stress: ratingValueSchema.nullable().optional(),
  notes: z.string().nullable().optional(),
  completeness: z.string().nullable().optional(),
  discomfortAreas: z.array(discomfortAreaSchema).default([]),
});

const trainingAdherenceDataSchema = z.object({
  scheduledCount: z.number().int(),
  completedCount: z.number().int(),
  skippedCount: z.number().int(),
  inProgressCount: z.number().int(),
  cancelledCount: z.number().int(),
});

const performanceHistoryEntrySchema = z.object({
  exerciseName: z.string().min(1),
  recordType: z.string().min(1),
  recordQualifier: z.string().nullable().optional(),
  achievedAt: z.string().min(1),
  scheduledDate: z.string().nullable().optional(),
});

const performanceHistoryDataSchema = z.object({
  recentRecords: z.array(performanceHistoryEntrySchema),
});

export const coachAthleteOverviewSchema = z.object({
  teamId: z.string().min(1),
  organizationId: z.string().min(1),
  athleteId: z.string().min(1),
  membershipId: z.string().min(1),
  displayName: z.string().min(1),
  role: z.string().min(1),
  viewDate: z.string().min(1).transform(parseDateOnly),
  effectiveScopes: z.array(z.string()),
  availability: sectionSchema(z.null()),
  readinessCategory: sectionSchema(readinessCategoryDataSchema),
  readinessScore: sectionSchema(readinessScoreDataSchema),
  limitingDimensions: sectionSchema(limitingDimensionsDataSchema),
  recoveryCheckIn: sectionSchema(recoveryCheckInDataSchema),
  trainingAdherence: sectionSchema(trainingAdherenceDataSchema),
  performanceHistory: sectionSchema(performanceHistoryDataSchema),
});

export type CoachAthleteOverview = z.infer<typeof coachAthleteOverviewSchema>;

export const trainingAssignmentSchema = z.object({
  id: z.string().min(1),
  teamId: z.string().min(1),
  athleteId: z.string().min(1),
  title: z.string().min(1),
  description: z.string().nullable(),
  scheduledDate: z.string().min(1),
  status: z.enum(['ASSIGNED', 'DECLINED', 'UNABLE']),
  athleteResponseNote: z.string().nullable(),
  respondedAt: z.string().nullable(),
  provenance: z.literal('COACH_ASSIGNMENT'),
  assignedByRole: z.enum(['COACH', 'HEAD_COACH']),
  version: z.number().int(),
});

export type TrainingAssignment = z.infer<typeof trainingAssignmentSchema>;

export const trainingAssignmentListSchema = z.array(trainingAssignmentSchema);
