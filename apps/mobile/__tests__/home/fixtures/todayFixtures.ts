import { TrainingTodayDashboard } from '@/src/features/training/schemas';

const defaultActions = {
  canCreateRecoveryCheckIn: { allowed: false },
  canUpdateRecoveryCheckIn: { allowed: false },
  canGenerateAthleteStateSnapshot: { allowed: false },
  canGenerateReadinessAssessment: { allowed: false },
  canGenerateTrainingRecommendation: { allowed: false },
  canGenerateAdaptationProposal: { allowed: false },
  canStartWorkout: { allowed: false },
  canContinueWorkout: { allowed: false },
  canSubmitSessionEffort: { allowed: false },
};

export const emptyTodayFixture: TrainingTodayDashboard = {
  date: '2026-08-10',
  athlete: { athleteId: 'athlete-1', displayName: 'Jordan Lee' },
  recovery: { checkInPresent: false },
  athleteState: { snapshotPresent: false },
  readiness: { readinessPresent: false },
  recommendation: { recommendationPresent: false },
  training: {
    scheduledOccurrenceCount: 0,
    modifiableOccurrenceCount: 0,
    completedOccurrenceCount: 0,
    inProgressOccurrenceCount: 0,
    occurrences: [],
    primaryOccurrence: null,
  },
  trainingLoad: { loadPresent: false },
  adaptation: { activeProposalPresent: false, unresolvedCount: 0 },
  recentPerformance: [],
  actions: defaultActions,
};

export const populatedTodayFixture: TrainingTodayDashboard = {
  date: '2026-08-10',
  athlete: { athleteId: 'athlete-1', displayName: 'Jordan Lee' },
  recovery: {
    checkInPresent: true,
    fatigue: 2,
    muscleSoreness: 3,
    stress: 2,
    sleepDurationMinutes: 420,
    sleepQuality: 4,
  },
  athleteState: {
    snapshotPresent: true,
    dailyAthleteStateSnapshotId: 'snap-1',
    snapshotVersion: 1,
  },
  readiness: {
    readinessPresent: true,
    readinessAssessmentId: 'assess-1',
    readinessScore: '78.5',
    readinessBand: 'HIGH',
    limitingDimensions: ['SLEEP'],
  },
  recommendation: {
    recommendationPresent: true,
    recommendationId: 'rec-1',
    overallAction: 'PROCEED_AS_PLANNED',
    recommendationStatus: 'ACTIVE',
    adjustmentTypes: ['REDUCE_VOLUME', 'EXTEND_WARMUP'],
  },
  training: {
    scheduledOccurrenceCount: 1,
    modifiableOccurrenceCount: 1,
    completedOccurrenceCount: 0,
    inProgressOccurrenceCount: 0,
    occurrences: [],
    primaryOccurrence: {
      occurrenceId: 'occ-1',
      trainingPlanId: 'plan-1',
      workoutDayId: 'day-1',
      trainingPlanName: 'Strength Block',
      workoutDayName: 'Upper Body A',
      status: 'SCHEDULED',
      scheduledDate: '2026-08-10',
      exerciseCount: 8,
      completedExerciseCount: 0,
      plannedEnvironmentName: 'Main Gym',
      feasibilityStatus: 'FULLY_FEASIBLE',
    },
  },
  trainingLoad: {
    loadPresent: true,
    occurrenceCount: 1,
    ratedOccurrenceCount: 0,
    unratedOccurrenceCount: 1,
    completedExerciseCount: 12,
    completedSetCount: 36,
    totalVolumeKilograms: '4500.5',
    totalDurationSeconds: 3600,
    totalDistanceMeters: '2500',
    totalSessionRpeLoad: '320',
    averageSessionRpe: '7.5',
  },
  adaptation: {
    activeProposalPresent: true,
    adaptationProposalId: 'prop-1',
    status: 'PENDING_REVIEW',
    origin: 'READINESS',
    unresolvedCount: 2,
    occurrenceId: 'occ-1',
  },
  recentPerformance: [
    {
      personalRecordId: 'pr-1',
      exerciseName: 'Back Squat',
      recordType: 'ONE_REP_MAX',
      normalizedValue: '140',
      normalizedUnit: 'KILOGRAM',
    },
    {
      personalRecordId: 'pr-2',
      exerciseName: '5K Run',
      recordType: 'TIME',
      normalizedValue: '1200',
      normalizedUnit: 'SECOND',
    },
  ],
  actions: {
    ...defaultActions,
    canCreateRecoveryCheckIn: { allowed: false },
    canUpdateRecoveryCheckIn: { allowed: true },
    canGenerateAthleteStateSnapshot: { allowed: false },
    canGenerateReadinessAssessment: { allowed: false },
    canGenerateTrainingRecommendation: { allowed: false },
    canStartWorkout: { allowed: true },
    canContinueWorkout: { allowed: false },
    canGenerateAdaptationProposal: { allowed: true },
  },
};

export const inProgressTodayFixture: TrainingTodayDashboard = {
  ...populatedTodayFixture,
  training: {
    ...populatedTodayFixture.training,
    inProgressOccurrenceCount: 1,
    primaryOccurrence: {
      ...populatedTodayFixture.training.primaryOccurrence!,
      status: 'IN_PROGRESS',
      completedExerciseCount: 3,
      startedAt: '2026-08-10T14:00:00Z',
    },
  },
  actions: {
    ...populatedTodayFixture.actions!,
    canStartWorkout: { allowed: false },
    canContinueWorkout: { allowed: true },
  },
};

export const generationActionsFixture: TrainingTodayDashboard = {
  ...emptyTodayFixture,
  actions: {
    ...defaultActions,
    canGenerateAthleteStateSnapshot: { allowed: true },
    canGenerateReadinessAssessment: { allowed: true },
    canGenerateTrainingRecommendation: { allowed: true },
  },
};

export const checkInReadyToGenerateStateFixture: TrainingTodayDashboard = {
  ...emptyTodayFixture,
  recovery: { checkInPresent: true },
  actions: {
    ...defaultActions,
    canGenerateAthleteStateSnapshot: { allowed: true },
  },
};
