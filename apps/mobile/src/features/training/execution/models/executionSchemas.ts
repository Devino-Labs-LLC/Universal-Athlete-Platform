import { z } from 'zod';

const bigDecimalLike = z
  .union([z.number(), z.string()])
  .transform(Number)
  .nullable()
  .optional();

export const workoutExerciseSetSchema = z
  .object({
    id: z.string(),
    workoutExerciseExecutionId: z.string(),
    setNumber: z.number(),
    displayOrder: z.number().optional(),
    setType: z.string().nullable().optional(),
    prescribedMinimumReps: z.number().nullable().optional(),
    prescribedMaximumReps: z.number().nullable().optional(),
    prescribedWeight: bigDecimalLike,
    prescribedWeightUnit: z.string().nullable().optional(),
    prescribedDurationSeconds: z.number().nullable().optional(),
    prescribedDistance: bigDecimalLike,
    prescribedDistanceUnit: z.string().nullable().optional(),
    prescribedTargetRpe: z.number().nullable().optional(),
    prescribedRestSeconds: z.number().nullable().optional(),
    actualReps: z.number().nullable().optional(),
    actualWeight: bigDecimalLike,
    actualWeightUnit: z.string().nullable().optional(),
    actualDurationSeconds: z.number().nullable().optional(),
    actualDistance: bigDecimalLike,
    actualDistanceUnit: z.string().nullable().optional(),
    actualRestSeconds: z.number().nullable().optional(),
    actualRpe: z.number().nullable().optional(),
    status: z.string(),
    startedAt: z.string().nullable().optional(),
    completedAt: z.string().nullable().optional(),
    athleteNotes: z.string().nullable().optional(),
    createdAt: z.string().nullable().optional(),
    updatedAt: z.string().nullable().optional(),
  })
  .passthrough();

export type WorkoutExerciseSet = z.infer<typeof workoutExerciseSetSchema>;

export const workoutExerciseSetsSchema = z.array(workoutExerciseSetSchema);

export const exerciseExecutionDetailSchema = z
  .object({
    id: z.string(),
    sourceWorkoutExerciseId: z.string().nullable().optional(),
    prescribedExerciseDefinitionId: z.string().nullable().optional(),
    prescribedExerciseName: z.string().nullable().optional(),
    performedExerciseDefinitionId: z.string().nullable().optional(),
    performedExerciseName: z.string().nullable().optional(),
    substituted: z.boolean().optional(),
    substitutionReason: z.string().nullable().optional(),
    displayOrder: z.number().optional(),
    exerciseName: z.string(),
    category: z.string().nullable().optional(),
    type: z.string().nullable().optional(),
    prescribedSets: z.number().nullable().optional(),
    prescribedMinimumReps: z.number().nullable().optional(),
    prescribedMaximumReps: z.number().nullable().optional(),
    prescribedTargetWeight: bigDecimalLike,
    prescribedWeightUnit: z.string().nullable().optional(),
    prescribedTargetDurationSeconds: z.number().nullable().optional(),
    prescribedTargetDistance: bigDecimalLike,
    prescribedDistanceUnit: z.string().nullable().optional(),
    prescribedTargetRestSeconds: z.number().nullable().optional(),
    prescribedTargetRpe: z.number().nullable().optional(),
    status: z.string(),
    athleteNotes: z.string().nullable().optional(),
    setCount: z.number().optional(),
    notStartedSetCount: z.number().optional(),
    inProgressSetCount: z.number().optional(),
    completedSetCount: z.number().optional(),
    skippedSetCount: z.number().optional(),
  })
  .passthrough();

export type ExerciseExecutionDetail = z.infer<typeof exerciseExecutionDetailSchema>;

export const exerciseExecutionsSchema = z.array(exerciseExecutionDetailSchema);

export const sessionEffortSchema = z
  .object({
    sessionRpe: z.number(),
    sessionDurationMinutes: z.number().nullable().optional(),
    perceivedNotes: z.string().nullable().optional(),
    createdAt: z.string().nullable().optional(),
    updatedAt: z.string().nullable().optional(),
  })
  .passthrough();

export type SessionEffort = z.infer<typeof sessionEffortSchema>;

export const sessionEffortRequestSchema = z.object({
  sessionRpe: z.number().min(0).max(10),
  sessionDurationMinutes: z.number().min(1).max(1440).optional(),
  perceivedNotes: z.string().max(1000).optional(),
});

export type SessionEffortRequest = z.infer<typeof sessionEffortRequestSchema>;

export const trainingLoadSummarySchema = z
  .object({
    sessionRpe: bigDecimalLike,
    sessionDurationMinutes: z.number().nullable().optional(),
    sessionRpeLoad: bigDecimalLike,
    prescribedExerciseCount: z.number().optional(),
    completedExerciseCount: z.number().optional(),
    substitutedExerciseCount: z.number().optional(),
    completedSetCount: z.number().optional(),
    skippedSetCount: z.number().optional(),
    completedRepetitionCount: z.number().optional(),
    totalVolumeKilograms: bigDecimalLike,
    totalDurationSeconds: z.number().nullable().optional(),
    totalDistanceMeters: bigDecimalLike,
    calculatedAt: z.string().nullable().optional(),
  })
  .passthrough();

export type TrainingLoadSummary = z.infer<typeof trainingLoadSummarySchema>;

export const patchWorkoutExerciseSetSchema = z.object({
  actualReps: z.number().nullable().optional(),
  actualWeight: z.number().nullable().optional(),
  actualWeightUnit: z.string().nullable().optional(),
  actualDurationSeconds: z.number().nullable().optional(),
  actualDistance: z.number().nullable().optional(),
  actualDistanceUnit: z.string().nullable().optional(),
  actualRestSeconds: z.number().nullable().optional(),
  actualRpe: z.number().nullable().optional(),
  athleteNotes: z.string().nullable().optional(),
});

export type PatchWorkoutExerciseSetRequest = z.infer<typeof patchWorkoutExerciseSetSchema>;

export const addWorkoutExerciseSetSchema = z.object({
  copyFromSetId: z.string().optional(),
});

export type AddWorkoutExerciseSetRequest = z.infer<typeof addWorkoutExerciseSetSchema>;
