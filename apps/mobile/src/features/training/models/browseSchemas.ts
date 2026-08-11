import { z } from 'zod';

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

export type BrowseActionFlag = z.infer<typeof actionFlagSchema>;

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

export type OverviewPlan = z.infer<typeof overviewPlanSchema>;

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

export type OverviewOccurrence = z.infer<typeof overviewOccurrenceSchema>;

const overviewWeeklyLoadSchema = z
  .object({
    weekStartDate: z.string(),
    weekEndDate: z.string(),
    occurrenceCount: z.number(),
    trainingDays: z.number(),
    totalVolumeKilograms: bigDecimalLike,
    totalDurationSeconds: z.number(),
    totalDistanceMeters: bigDecimalLike,
    totalSessionRpeLoad: bigDecimalLike,
    averageSessionRpe: bigDecimalLike,
  })
  .passthrough();

export type OverviewWeeklyLoad = z.infer<typeof overviewWeeklyLoadSchema>;

const overviewCompletedSessionSchema = z
  .object({
    occurrenceId: z.string(),
    trainingPlanId: z.string(),
    trainingPlanName: z.string(),
    workoutDayId: z.string(),
    workoutDayName: z.string(),
    scheduledDate: z.string(),
    completedAt: z.string().nullable().optional(),
    exerciseCount: z.number(),
    completedExerciseCount: z.number(),
  })
  .passthrough();

export type OverviewCompletedSession = z.infer<typeof overviewCompletedSessionSchema>;

const overviewPersonalRecordSchema = z
  .object({
    personalRecordId: z.string(),
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

const overviewEnvironmentSchema = z
  .object({
    trainingEnvironmentId: z.string(),
    name: z.string(),
    type: z.string(),
    defaultEnvironment: z.boolean(),
    availableEquipmentCount: z.number(),
  })
  .passthrough();

const overviewAdaptationSchema = z
  .object({
    adaptationProposalId: z.string(),
    occurrenceId: z.string(),
    status: z.string(),
    unresolvedCount: z.number(),
    generatedAt: z.string().nullable().optional(),
    expiresAt: z.string().nullable().optional(),
  })
  .passthrough();

export type OverviewAdaptation = z.infer<typeof overviewAdaptationSchema>;

export const trainingOverviewSchema = z
  .object({
    date: z.string(),
    activePlans: z.array(overviewPlanSchema).optional(),
    upcomingOccurrences: z.array(overviewOccurrenceSchema).optional(),
    weeklyLoadSummary: overviewWeeklyLoadSchema.nullable().optional(),
    recentCompletedSessions: z.array(overviewCompletedSessionSchema).optional(),
    recentPersonalRecords: z.array(overviewPersonalRecordSchema).optional(),
    activeEnvironments: z.array(overviewEnvironmentSchema).optional(),
    outstandingAdaptationProposals: z.array(overviewAdaptationSchema).optional(),
  })
  .passthrough();

export type TrainingOverview = z.infer<typeof trainingOverviewSchema>;

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
    notStartedExerciseCount: z.number(),
    inProgressExerciseCount: z.number(),
    completedExerciseCount: z.number(),
    skippedExerciseCount: z.number(),
  })
  .passthrough();

export type CalendarEntry = z.infer<typeof calendarEntrySchema>;

export const calendarEntriesSchema = z.array(calendarEntrySchema);

export const trainingPlanSchema = z
  .object({
    id: z.string(),
    type: z.string(),
    customTypeName: z.string().nullable().optional(),
    name: z.string(),
    description: z.string().nullable().optional(),
    status: z.string(),
    startDate: z.string(),
    endDate: z.string().nullable().optional(),
    athleteSportId: z.string().nullable().optional(),
    athleteGoalId: z.string().nullable().optional(),
    defaultTrainingEnvironmentId: z.string().nullable().optional(),
    scheduleStartDate: z.string().nullable().optional(),
    scheduleEndDate: z.string().nullable().optional(),
    scheduleTimezone: z.string().nullable().optional(),
    scheduleStatus: z.string().nullable().optional(),
    recurrenceMode: z.string().nullable().optional(),
    scheduleGeneratedThrough: z.string().nullable().optional(),
    scheduleActivatedAt: z.string().nullable().optional(),
    schedulePausedAt: z.string().nullable().optional(),
    createdAt: z.string().nullable().optional(),
    updatedAt: z.string().nullable().optional(),
  })
  .passthrough();

export type TrainingPlan = z.infer<typeof trainingPlanSchema>;

export const workoutDaySchema = z
  .object({
    id: z.string(),
    displayOrder: z.number(),
    title: z.string(),
    description: z.string().nullable().optional(),
    planWeekNumber: z.number().nullable().optional(),
    scheduledDayOfWeek: z.string().nullable().optional(),
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

const environmentSnapshotSchema = z
  .object({
    trainingEnvironmentId: z.string().nullable().optional(),
    name: z.string().nullable().optional(),
  })
  .passthrough();

const occurrenceEnvironmentSchema = z
  .object({
    plannedEnvironment: environmentSnapshotSchema.nullable().optional(),
    actualEnvironment: environmentSnapshotSchema.nullable().optional(),
  })
  .passthrough()
  .nullable()
  .optional();

export const exerciseExecutionSchema = z
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
    status: z.string(),
    setCount: z.number().optional(),
    completedSetCount: z.number().optional(),
    skippedSetCount: z.number().optional(),
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
    environment: occurrenceEnvironmentSchema,
    executions: z.array(exerciseExecutionSchema).optional(),
  })
  .passthrough();

export type WorkoutOccurrenceDetail = z.infer<typeof workoutOccurrenceDetailSchema>;

export const workoutExerciseSchema = z
  .object({
    id: z.string(),
    exerciseDefinitionId: z.string().nullable().optional(),
    displayOrder: z.number(),
    exerciseName: z.string(),
    category: z.string().nullable().optional(),
    type: z.string().nullable().optional(),
    sets: z.number().nullable().optional(),
    minimumReps: z.number().nullable().optional(),
    maximumReps: z.number().nullable().optional(),
    targetWeight: bigDecimalLike,
    weightUnit: z.string().nullable().optional(),
    targetDurationSeconds: z.number().nullable().optional(),
    targetDistance: bigDecimalLike,
    distanceUnit: z.string().nullable().optional(),
    targetRestSeconds: z.number().nullable().optional(),
    targetRpe: z.number().nullable().optional(),
    tempo: z.string().nullable().optional(),
    coachingNotes: z.string().nullable().optional(),
    status: z.string().nullable().optional(),
  })
  .passthrough();

export type WorkoutExercise = z.infer<typeof workoutExerciseSchema>;

export const workoutExercisesSchema = z.array(workoutExerciseSchema);

const launchOccurrenceSchema = z
  .object({
    occurrenceId: z.string(),
    trainingPlanId: z.string(),
    workoutDayId: z.string(),
    status: z.string(),
    scheduledDate: z.string(),
    startedAt: z.string().nullable().optional(),
    completedAt: z.string().nullable().optional(),
    startEligible: z.boolean().optional(),
  })
  .passthrough();

const launchExerciseSchema = z
  .object({
    executionId: z.string(),
    orderIndex: z.number(),
    prescribedExerciseDefinitionId: z.string().nullable().optional(),
    prescribedExerciseName: z.string().nullable().optional(),
    performedExerciseDefinitionId: z.string().nullable().optional(),
    performedExerciseName: z.string().nullable().optional(),
    substituted: z.boolean().optional(),
    substitutionReason: z.string().nullable().optional(),
    status: z.string(),
    prescribedSets: z.number().nullable().optional(),
    prescribedMinimumReps: z.number().nullable().optional(),
    prescribedMaximumReps: z.number().nullable().optional(),
    prescribedTargetWeight: bigDecimalLike,
    prescribedWeightUnit: z.string().nullable().optional(),
    prescribedTargetDurationSeconds: z.number().nullable().optional(),
    prescribedTargetDistance: bigDecimalLike,
    prescribedTargetRestSeconds: z.number().nullable().optional(),
    prescribedTargetRpe: z.number().nullable().optional(),
  })
  .passthrough();

const launchEnvironmentSchema = z
  .object({
    plannedEnvironmentId: z.string().nullable().optional(),
    plannedEnvironmentName: z.string().nullable().optional(),
    plannedEquipment: z.array(z.string()).optional(),
    actualEnvironmentId: z.string().nullable().optional(),
    actualEnvironmentName: z.string().nullable().optional(),
    actualEquipment: z.array(z.string()).optional(),
    availableEquipment: z.array(z.string()).optional(),
  })
  .passthrough();

const launchFeasibilitySchema = z
  .object({
    feasibilityPresent: z.boolean(),
    status: z.string().nullable().optional(),
    totalExercises: z.number().optional(),
    feasibleExercises: z.number().optional(),
    infeasibleExercises: z.number().optional(),
    feasibilityPercentage: bigDecimalLike,
    exercisesWithCompatibleSuggestions: z.number().optional(),
    exercisesWithoutCompatibleSuggestions: z.number().optional(),
  })
  .passthrough();

const launchRecommendationSchema = z
  .object({
    recommendationPresent: z.boolean(),
    recommendationId: z.string().nullable().optional(),
    overallAction: z.string().nullable().optional(),
    readinessBand: z.string().nullable().optional(),
    adjustmentTypes: z.array(z.string()).optional(),
    occurrenceInRecommendationContexts: z.boolean().optional(),
  })
  .passthrough();

const launchAdaptationSchema = z
  .object({
    activeProposalPresent: z.boolean(),
    adaptationProposalId: z.string().nullable().optional(),
    status: z.string().nullable().optional(),
    unresolvedCount: z.number().optional(),
  })
  .passthrough();

const launchActionsSchema = z
  .object({
    canStart: actionFlagSchema,
    canChangeEnvironment: actionFlagSchema,
    canGenerateAdaptation: actionFlagSchema,
    canApplyAdaptation: actionFlagSchema,
    canSubstituteExercise: actionFlagSchema,
  })
  .passthrough();

export const workoutLaunchContextSchema = z
  .object({
    occurrence: launchOccurrenceSchema,
    exercises: z.array(launchExerciseSchema).optional(),
    environment: launchEnvironmentSchema.optional(),
    feasibility: launchFeasibilitySchema.optional(),
    recommendationContext: launchRecommendationSchema.optional(),
    adaptation: launchAdaptationSchema.optional(),
    actions: launchActionsSchema,
  })
  .passthrough();

export type WorkoutLaunchContext = z.infer<typeof workoutLaunchContextSchema>;
