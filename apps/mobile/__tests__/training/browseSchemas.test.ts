import {
  calendarEntrySchema,
  trainingOverviewSchema,
  workoutLaunchContextSchema,
} from '@/src/features/training/models/browseSchemas';

describe('training overview schema', () => {
  it('parses populated overview payload', () => {
    const parsed = trainingOverviewSchema.parse({
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
          occurrenceId: 'occ-1',
          trainingPlanId: 'plan-1',
          trainingPlanName: 'Strength Block',
          workoutDayId: 'day-1',
          workoutDayName: 'Upper A',
          scheduledDate: '2026-08-10',
          status: 'SCHEDULED',
          exerciseCount: 6,
          completedExerciseCount: 0,
        },
      ],
      weeklyLoadSummary: {
        weekStartDate: '2026-08-04',
        weekEndDate: '2026-08-10',
        occurrenceCount: 2,
        trainingDays: 2,
        totalVolumeKilograms: '8500.5',
        totalDurationSeconds: 3600,
        totalDistanceMeters: '1200',
        totalSessionRpeLoad: '300',
        averageSessionRpe: '7.2',
      },
      recentCompletedSessions: [],
      recentPersonalRecords: [],
      activeEnvironments: [],
      outstandingAdaptationProposals: [],
    });

    expect(parsed.activePlans?.[0]?.name).toBe('Strength Block');
    expect(parsed.weeklyLoadSummary?.totalVolumeKilograms).toBe(8500.5);
  });

  it('parses empty overview payload', () => {
    const parsed = trainingOverviewSchema.parse({
      date: '2026-08-10',
      activePlans: [],
      upcomingOccurrences: [],
      weeklyLoadSummary: null,
      recentCompletedSessions: [],
      recentPersonalRecords: [],
      activeEnvironments: [],
      outstandingAdaptationProposals: [],
    });

    expect(parsed.upcomingOccurrences).toEqual([]);
    expect(parsed.weeklyLoadSummary).toBeNull();
  });
});

describe('calendar entry schema', () => {
  it('parses calendar entry with decimal-like fields coerced when present', () => {
    const parsed = calendarEntrySchema.parse({
      occurrenceId: 'occ-1',
      trainingPlanId: 'plan-1',
      trainingPlanName: 'Strength Block',
      workoutDayId: 'day-1',
      workoutDayName: 'Upper A',
      scheduledDate: '2026-08-10',
      plannedStartTime: '09:00:00',
      status: 'SCHEDULED',
      origin: 'SCHEDULED',
      manuallyRescheduled: false,
      originalScheduledDate: '2026-08-10',
      exerciseCount: 6,
      notStartedExerciseCount: 6,
      inProgressExerciseCount: 0,
      completedExerciseCount: 0,
      skippedExerciseCount: 0,
    });

    expect(parsed.workoutDayName).toBe('Upper A');
  });
});

describe('workout launch context schema', () => {
  it('parses launch context payload', () => {
    const parsed = workoutLaunchContextSchema.parse({
      occurrence: {
        occurrenceId: 'occ-1',
        trainingPlanId: 'plan-1',
        workoutDayId: 'day-1',
        status: 'SCHEDULED',
        scheduledDate: '2026-08-10',
        startEligible: true,
      },
      exercises: [],
      environment: {
        plannedEnvironmentName: 'Home Gym',
      },
      feasibility: {
        feasibilityPresent: true,
        status: 'FULLY_FEASIBLE',
        totalExercises: 6,
        feasibleExercises: 6,
        infeasibleExercises: 0,
        feasibilityPercentage: '100',
      },
      recommendationContext: {
        recommendationPresent: false,
      },
      adaptation: {
        activeProposalPresent: false,
      },
      actions: {
        canStart: { allowed: true },
        canChangeEnvironment: { allowed: true },
        canGenerateAdaptation: { allowed: false, reasonCode: 'NOT_ELIGIBLE' },
        canApplyAdaptation: { allowed: false },
        canSubstituteExercise: { allowed: false },
      },
    });

    expect(parsed.actions.canStart.allowed).toBe(true);
    expect(parsed.feasibility?.feasibilityPercentage).toBe(100);
  });
});
