import { z } from 'zod';

import {
  type ExerciseDefinition,
  exerciseDefinitionPageSchema,
  type ExerciseDefinitionPage,
  exerciseDefinitionSchema,
  type MetricMode,
  metricModeSchema,
} from '@/features/exercises/models/schemas';

export { exerciseDefinitionPageSchema, exerciseDefinitionSchema, metricModeSchema };
export type { ExerciseDefinition, ExerciseDefinitionPage, MetricMode };

const bigDecimalLike = z
  .union([z.number(), z.string()])
  .transform(Number)
  .nullable()
  .optional();

export const planTypeSchema = z.enum([
  'GENERAL',
  'STRENGTH',
  'POWER',
  'HYPERTROPHY',
  'ENDURANCE',
  'SPEED',
  'AGILITY',
  'VERTICAL',
  'SPORT_SPECIFIC',
  'RETURN_TO_PLAY',
  'OTHER',
]);

export const planContentStatusSchema = z.enum(['DRAFT', 'ACTIVE', 'COMPLETED', 'ARCHIVED']);
export const scheduleStatusSchema = z.enum(['DRAFT', 'ACTIVE', 'PAUSED', 'COMPLETED']);
export const recurrenceModeSchema = z.enum(['FINITE', 'REPEATING']);
export const dayOfWeekSchema = z.enum([
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
]);

export const exerciseCategorySchema = z.enum([
  'STRENGTH',
  'POWER',
  'PLYOMETRICS',
  'CONDITIONING',
  'CARDIO',
  'MOBILITY',
  'FLEXIBILITY',
  'SPORT_SKILL',
  'RECOVERY',
  'OTHER',
]);

export const exerciseTypeSchema = z.enum([
  'BARBELL',
  'DUMBBELL',
  'BODYWEIGHT',
  'MACHINE',
  'CABLE',
  'KETTLEBELL',
  'RESISTANCE_BAND',
  'SPRINT',
  'RUN',
  'JUMP',
  'SWIM',
  'CYCLING',
  'ROWING',
  'SPORT',
  'OTHER',
]);

export const weightUnitSchema = z.enum(['KILOGRAM', 'POUND']);
export const distanceUnitSchema = z.enum(['METER', 'KILOMETER', 'MILE']);

export const trainingPlanSchema = z
  .object({
    id: z.string(),
    type: planTypeSchema,
    customTypeName: z.string().nullable().optional(),
    name: z.string(),
    description: z.string().nullable().optional(),
    status: planContentStatusSchema,
    startDate: z.string(),
    endDate: z.string().nullable().optional(),
    athleteSportId: z.string().nullable().optional(),
    athleteGoalId: z.string().nullable().optional(),
    defaultTrainingEnvironmentId: z.string().nullable().optional(),
    scheduleStartDate: z.string().nullable().optional(),
    scheduleEndDate: z.string().nullable().optional(),
    scheduleTimezone: z.string().nullable().optional(),
    scheduleStatus: scheduleStatusSchema.nullable().optional(),
    recurrenceMode: recurrenceModeSchema.nullable().optional(),
    scheduleGeneratedThrough: z.string().nullable().optional(),
    scheduleActivatedAt: z.string().nullable().optional(),
    schedulePausedAt: z.string().nullable().optional(),
    createdAt: z.string().nullable().optional(),
    updatedAt: z.string().nullable().optional(),
  })
  .passthrough();

export type TrainingPlan = z.infer<typeof trainingPlanSchema>;
export const trainingPlansSchema = z.array(trainingPlanSchema);

export const createTrainingPlanSchema = z
  .object({
    type: planTypeSchema,
    customTypeName: z.string().max(120).optional(),
    name: z.string().min(1).max(160),
    description: z.string().max(2000).optional(),
    startDate: z.string().min(1),
    endDate: z.string().min(1),
    athleteSportId: z.string().uuid().optional(),
    athleteGoalId: z.string().uuid().optional(),
    defaultTrainingEnvironmentId: z.string().uuid().optional(),
  })
  .refine(
    (data) => data.type !== 'OTHER' || Boolean(data.customTypeName?.trim()),
    { message: 'Custom type name is required when type is OTHER', path: ['customTypeName'] },
  );

export type CreateTrainingPlanRequest = z.infer<typeof createTrainingPlanSchema>;

export const updateTrainingPlanSchema = z.object({
  name: z.string().min(1).max(160).optional(),
  description: z.string().max(2000).nullable().optional(),
  startDate: z.string().optional(),
  endDate: z.string().nullable().optional(),
  athleteSportId: z.string().uuid().nullable().optional(),
  athleteGoalId: z.string().uuid().nullable().optional(),
  defaultTrainingEnvironmentId: z.string().uuid().nullable().optional(),
});

export type UpdateTrainingPlanRequest = z.infer<typeof updateTrainingPlanSchema>;

export const planStatusActionSchema = z.object({
  action: z.enum(['ACTIVATE', 'COMPLETE', 'ARCHIVE']),
});

export const workoutDaySchema = z
  .object({
    id: z.string(),
    displayOrder: z.number(),
    title: z.string(),
    description: z.string().nullable().optional(),
    planWeekNumber: z.number().nullable().optional(),
    scheduledDayOfWeek: dayOfWeekSchema.nullable().optional(),
    plannedStartTime: z.string().nullable().optional(),
    expectedDurationMinutes: z.number().nullable().optional(),
    status: z.string(),
    trainingEnvironmentOverrideId: z.string().nullable().optional(),
    createdAt: z.string().nullable().optional(),
    updatedAt: z.string().nullable().optional(),
  })
  .passthrough();

export type WorkoutDay = z.infer<typeof workoutDaySchema>;
export const workoutDaysSchema = z.array(workoutDaySchema);

export const createWorkoutDaySchema = z.object({
  title: z.string().min(1).max(160),
  description: z.string().max(2000).optional(),
  planWeekNumber: z.number().int().min(1),
  scheduledDayOfWeek: dayOfWeekSchema,
  plannedStartTime: z.string().optional(),
  expectedDurationMinutes: z.number().int().positive().optional(),
  displayOrder: z.number().int().optional(),
  trainingEnvironmentOverrideId: z.string().uuid().optional(),
});

export type CreateWorkoutDayRequest = z.infer<typeof createWorkoutDaySchema>;

export const updateWorkoutDaySchema = z.object({
  title: z.string().min(1).max(160).optional(),
  description: z.string().max(2000).nullable().optional(),
  planWeekNumber: z.number().int().min(1).optional(),
  scheduledDayOfWeek: dayOfWeekSchema.optional(),
  plannedStartTime: z.string().nullable().optional(),
  expectedDurationMinutes: z.number().int().positive().nullable().optional(),
  displayOrder: z.number().int().optional(),
  trainingEnvironmentOverrideId: z.string().uuid().nullable().optional(),
});

export type UpdateWorkoutDayRequest = z.infer<typeof updateWorkoutDaySchema>;

export const reorderIdsSchema = z.object({
  dayIds: z.array(z.string().uuid()).optional(),
  exerciseIds: z.array(z.string().uuid()).optional(),
});

export const workoutExerciseSchema = z
  .object({
    id: z.string(),
    exerciseDefinitionId: z.string().nullable().optional(),
    displayOrder: z.number(),
    exerciseName: z.string(),
    category: exerciseCategorySchema.nullable().optional(),
    type: exerciseTypeSchema.nullable().optional(),
    sets: z.number().nullable().optional(),
    minimumReps: z.number().nullable().optional(),
    maximumReps: z.number().nullable().optional(),
    targetWeight: bigDecimalLike,
    weightUnit: weightUnitSchema.nullable().optional(),
    targetDurationSeconds: z.number().nullable().optional(),
    targetDistance: bigDecimalLike,
    distanceUnit: distanceUnitSchema.nullable().optional(),
    targetRestSeconds: z.number().nullable().optional(),
    targetRpe: z.number().min(0).max(10).nullable().optional(),
    tempo: z.string().nullable().optional(),
    coachingNotes: z.string().nullable().optional(),
    status: z.string().nullable().optional(),
  })
  .passthrough();

export type WorkoutExercise = z.infer<typeof workoutExerciseSchema>;
export const workoutExercisesSchema = z.array(workoutExerciseSchema);

export const createWorkoutExerciseSchema = z.object({
  exerciseDefinitionId: z.string().uuid(),
  exerciseName: z.string().optional(),
  category: exerciseCategorySchema,
  type: exerciseTypeSchema,
  sets: z.number().int().positive(),
  minimumReps: z.number().int().positive().optional(),
  maximumReps: z.number().int().positive().optional(),
  targetWeight: z.number().positive().optional(),
  weightUnit: weightUnitSchema.optional(),
  targetDurationSeconds: z.number().int().positive().optional(),
  targetDistance: z.number().positive().optional(),
  distanceUnit: distanceUnitSchema.optional(),
  targetRestSeconds: z.number().int().positive().optional(),
  targetRpe: z.number().min(0).max(10).optional(),
  tempo: z.string().optional(),
  coachingNotes: z.string().optional(),
  displayOrder: z.number().int().optional(),
});

export type CreateWorkoutExerciseRequest = z.infer<typeof createWorkoutExerciseSchema>;

export const updateWorkoutExerciseSchema = z.object({
  exerciseName: z.string().optional(),
  category: exerciseCategorySchema.optional(),
  type: exerciseTypeSchema.optional(),
  sets: z.number().int().positive().optional(),
  minimumReps: z.number().int().positive().nullable().optional(),
  maximumReps: z.number().int().positive().nullable().optional(),
  targetWeight: z.number().positive().nullable().optional(),
  weightUnit: weightUnitSchema.nullable().optional(),
  targetDurationSeconds: z.number().int().positive().nullable().optional(),
  targetDistance: z.number().positive().nullable().optional(),
  distanceUnit: distanceUnitSchema.nullable().optional(),
  targetRestSeconds: z.number().int().positive().nullable().optional(),
  targetRpe: z.number().min(0).max(10).nullable().optional(),
  tempo: z.string().nullable().optional(),
  coachingNotes: z.string().nullable().optional(),
  displayOrder: z.number().int().optional(),
});

export type UpdateWorkoutExerciseRequest = z.infer<typeof updateWorkoutExerciseSchema>;

export const activateScheduleSchema = z.object({
  scheduleStartDate: z.string().min(1),
  scheduleEndDate: z.string().optional(),
  timezone: z.string().min(1),
  recurrenceMode: recurrenceModeSchema,
  generateThrough: z.string().optional(),
});

export type ActivateScheduleRequest = z.infer<typeof activateScheduleSchema>;

export const generateOccurrencesSchema = z
  .object({
    scheduledFrom: z.string().min(1),
    scheduledTo: z.string().min(1),
  })
  .refine(
    (data) => {
      const from = new Date(data.scheduledFrom);
      const to = new Date(data.scheduledTo);
      const diffDays = Math.round((to.getTime() - from.getTime()) / (1000 * 60 * 60 * 24));
      return diffDays >= 0 && diffDays <= 90;
    },
    { message: 'Range must be between 0 and 90 days inclusive', path: ['scheduledTo'] },
  );

export type GenerateOccurrencesRequest = z.infer<typeof generateOccurrencesSchema>;

export const generationResultSchema = z
  .object({
    requestedFrom: z.string(),
    requestedTo: z.string(),
    createdCount: z.number(),
    existingCount: z.number(),
    cancelledPlacementCount: z.number(),
    outOfScheduleCount: z.number(),
    generatedThrough: z.string().nullable().optional(),
    createdOccurrences: z.array(z.record(z.string(), z.unknown())).optional(),
  })
  .passthrough();

export type GenerationResult = z.infer<typeof generationResultSchema>;

export const scheduleActivationResponseSchema = z
  .object({
    plan: trainingPlanSchema,
    generation: generationResultSchema.nullable().optional(),
  })
  .passthrough();

export const calendarEntrySchema = z
  .object({
    occurrenceId: z.string(),
    trainingPlanId: z.string(),
    trainingPlanName: z.string(),
    workoutDayId: z.string(),
    workoutDayName: z.string(),
    scheduledDate: z.string(),
    plannedStartTime: z.string().nullable().optional(),
    status: z.string(),
    origin: z.string().nullable().optional(),
    manuallyRescheduled: z.boolean().optional(),
    originalScheduledDate: z.string().nullable().optional(),
    startedAt: z.string().nullable().optional(),
    completedAt: z.string().nullable().optional(),
    athleteNotes: z.string().nullable().optional(),
    exerciseCount: z.number(),
    notStartedExerciseCount: z.number().optional(),
    inProgressExerciseCount: z.number().optional(),
    completedExerciseCount: z.number(),
    skippedExerciseCount: z.number().optional(),
  })
  .passthrough();

export type CalendarEntry = z.infer<typeof calendarEntrySchema>;
export const calendarEntriesSchema = z.array(calendarEntrySchema);

const environmentSnapshotSchema = z
  .object({
    trainingEnvironmentId: z.string().nullable().optional(),
    name: z.string().nullable().optional(),
  })
  .passthrough();

export const exerciseExecutionSchema = z
  .object({
    id: z.string(),
    sourceWorkoutExerciseId: z.string().nullable().optional(),
    prescribedExerciseDefinitionId: z.string().nullable().optional(),
    prescribedExerciseName: z.string().nullable().optional(),
    exerciseName: z.string(),
    category: z.string().nullable().optional(),
    type: z.string().nullable().optional(),
    prescribedSets: z.number().nullable().optional(),
    prescribedMinimumReps: z.number().nullable().optional(),
    prescribedMaximumReps: z.number().nullable().optional(),
    prescribedTargetWeight: bigDecimalLike,
    prescribedWeightUnit: z.string().nullable().optional(),
    status: z.string(),
    setCount: z.number().optional(),
    completedSetCount: z.number().optional(),
  })
  .passthrough();

export type ExerciseExecution = z.infer<typeof exerciseExecutionSchema>;

export const workoutOccurrenceDetailSchema = z
  .object({
    id: z.string(),
    workoutDayId: z.string(),
    scheduledDate: z.string(),
    plannedStartTime: z.string().nullable().optional(),
    startedAt: z.string().nullable().optional(),
    completedAt: z.string().nullable().optional(),
    status: z.string(),
    athleteNotes: z.string().nullable().optional(),
    origin: z.string().nullable().optional(),
    originalScheduledDate: z.string().nullable().optional(),
    manuallyRescheduled: z.boolean().optional(),
    environment: z
      .object({
        plannedEnvironment: environmentSnapshotSchema.nullable().optional(),
        actualEnvironment: environmentSnapshotSchema.nullable().optional(),
      })
      .passthrough()
      .nullable()
      .optional(),
    executions: z.array(exerciseExecutionSchema).optional(),
  })
  .passthrough();

export type WorkoutOccurrenceDetail = z.infer<typeof workoutOccurrenceDetailSchema>;
export const workoutOccurrencesSchema = z.array(workoutOccurrenceDetailSchema);

export const createOccurrenceSchema = z.object({
  scheduledDate: z.string().min(1),
  plannedStartTime: z.string().optional(),
  athleteNotes: z.string().optional(),
});

export type CreateOccurrenceRequest = z.infer<typeof createOccurrenceSchema>;

export const rescheduleOccurrenceSchema = z.object({
  scheduledDate: z.string().min(1),
  plannedStartTime: z.string().optional(),
});

export type RescheduleOccurrenceRequest = z.infer<typeof rescheduleOccurrenceSchema>;

export const trainingEnvironmentSchema = z
  .object({
    id: z.string(),
    name: z.string(),
    type: z.string(),
    defaultEnvironment: z.boolean(),
    active: z.boolean(),
  })
  .passthrough();

export type TrainingEnvironment = z.infer<typeof trainingEnvironmentSchema>;
export const trainingEnvironmentsSchema = z.array(trainingEnvironmentSchema);

const overviewPlanSchema = z
  .object({
    trainingPlanId: z.string(),
    name: z.string(),
    type: z.string(),
    status: z.string(),
    startDate: z.string(),
    endDate: z.string().nullable().optional(),
    scheduleTimezone: z.string().nullable().optional(),
  })
  .passthrough();

const overviewOccurrenceSchema = z
  .object({
    occurrenceId: z.string(),
    trainingPlanId: z.string(),
    trainingPlanName: z.string(),
    workoutDayId: z.string(),
    workoutDayName: z.string(),
    scheduledDate: z.string(),
    status: z.string(),
    exerciseCount: z.number(),
    completedExerciseCount: z.number(),
  })
  .passthrough();

export const trainingOverviewSchema = z
  .object({
    date: z.string(),
    activePlans: z.array(overviewPlanSchema).optional(),
    upcomingOccurrences: z.array(overviewOccurrenceSchema).optional(),
    recentCompletedSessions: z.array(overviewOccurrenceSchema).optional(),
  })
  .passthrough();

export type TrainingOverview = z.infer<typeof trainingOverviewSchema>;
