export const populatedTodayFixture = {
  date: '2026-08-11',
  athlete: { athleteId: 'a1', displayName: 'Alex Runner' },
  recovery: { checkInPresent: true, fatigue: 3 },
  athleteState: { snapshotPresent: true },
  readiness: {
    readinessPresent: true,
    readinessBand: 'HIGH',
    readinessScore: 82,
  },
  recommendation: {
    recommendationPresent: true,
    overallAction: 'PROCEED_AS_PLANNED',
    adjustmentTypes: ['REDUCE_VOLUME'],
  },
  training: {
    scheduledOccurrenceCount: 1,
    primaryOccurrence: {
      occurrenceId: 'occ-1',
      trainingPlanId: 'plan-1',
      workoutDayId: 'day-1',
      trainingPlanName: 'Base Plan',
      workoutDayName: 'Lower Body',
      status: 'SCHEDULED',
      scheduledDate: '2026-08-11',
      exerciseCount: 6,
      completedExerciseCount: 0,
    },
  },
  trainingLoad: { loadPresent: true, completedSetCount: 12 },
  adaptation: { activeProposalPresent: false },
  recentPerformance: [],
  actions: {
    canCreateRecoveryCheckIn: { allowed: false },
    canUpdateRecoveryCheckIn: { allowed: false },
    canGenerateAthleteStateSnapshot: { allowed: true },
    canGenerateReadinessAssessment: { allowed: false },
    canGenerateTrainingRecommendation: { allowed: false },
    canGenerateAdaptationProposal: { allowed: false },
    canStartWorkout: { allowed: true },
    canContinueWorkout: { allowed: false },
    canSubmitSessionEffort: { allowed: false },
  },
};

export const emptyTodayFixture = {
  date: '2026-08-11',
  recovery: { checkInPresent: false },
  readiness: { readinessPresent: false },
  recommendation: { recommendationPresent: false },
  training: { scheduledOccurrenceCount: 0, primaryOccurrence: null },
};
