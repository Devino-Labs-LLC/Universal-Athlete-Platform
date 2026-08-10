import { TrainingOverview } from '@/src/features/training/models/browseSchemas';

export const emptyOverviewFixture: TrainingOverview = {
  date: '2026-08-10',
  activePlans: [],
  upcomingOccurrences: [],
  weeklyLoadSummary: null,
  recentCompletedSessions: [],
  recentPersonalRecords: [],
  activeEnvironments: [],
  outstandingAdaptationProposals: [],
};

export const populatedOverviewFixture: TrainingOverview = {
  date: '2026-08-10',
  activePlans: [
    {
      trainingPlanId: 'plan-1',
      name: 'Strength Block',
      type: 'STRENGTH',
      status: 'ACTIVE',
      startDate: '2026-08-01',
      endDate: '2026-09-01',
      scheduleTimezone: 'America/New_York',
    },
  ],
  upcomingOccurrences: [
    {
      occurrenceId: 'occ-in-progress',
      trainingPlanId: 'plan-1',
      trainingPlanName: 'Strength Block',
      workoutDayId: 'day-1',
      workoutDayName: 'Upper A',
      scheduledDate: '2026-08-10',
      status: 'IN_PROGRESS',
      exerciseCount: 6,
      completedExerciseCount: 2,
    },
    {
      occurrenceId: 'occ-scheduled',
      trainingPlanId: 'plan-1',
      trainingPlanName: 'Strength Block',
      workoutDayId: 'day-2',
      workoutDayName: 'Lower A',
      scheduledDate: '2026-08-12',
      status: 'SCHEDULED',
      exerciseCount: 5,
      completedExerciseCount: 0,
    },
  ],
  weeklyLoadSummary: {
    weekStartDate: '2026-08-04',
    weekEndDate: '2026-08-10',
    occurrenceCount: 3,
    trainingDays: 3,
    totalVolumeKilograms: 12500,
    totalDurationSeconds: 5400,
    totalDistanceMeters: 0,
    totalSessionRpeLoad: 420,
    averageSessionRpe: 7.5,
  },
  recentCompletedSessions: [
    {
      occurrenceId: 'occ-completed',
      trainingPlanId: 'plan-1',
      trainingPlanName: 'Strength Block',
      workoutDayId: 'day-3',
      workoutDayName: 'Upper B',
      scheduledDate: '2026-08-08',
      completedAt: '2026-08-08T18:30:00Z',
      exerciseCount: 6,
      completedExerciseCount: 6,
    },
  ],
  recentPersonalRecords: [],
  activeEnvironments: [],
  outstandingAdaptationProposals: [
    {
      adaptationProposalId: 'adapt-1',
      occurrenceId: 'occ-in-progress',
      status: 'PENDING',
      unresolvedCount: 2,
      generatedAt: '2026-08-10T10:00:00Z',
      expiresAt: '2026-08-11T10:00:00Z',
    },
    {
      adaptationProposalId: 'adapt-2',
      occurrenceId: 'occ-unknown',
      status: 'PENDING',
      unresolvedCount: 1,
    },
  ],
};
