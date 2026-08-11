import {
  adaptationProposalPath,
  resolveAdaptationRouteFromToday,
  resolveOccurrenceForAdaptation,
} from '@/src/features/adaptation/utils/proposalNavigation';
import { populatedTodayFixture } from '../home/fixtures/todayFixtures';

describe('proposalNavigation', () => {
  it('builds adaptation proposal route path', () => {
    expect(adaptationProposalPath('plan-1', 'day-1', 'occ-1', 'prop-1')).toBe(
      '/(tabs)/training/plans/plan-1/days/day-1/occurrences/occ-1/adaptation/prop-1',
    );
  });

  it('resolves occurrence from today dashboard occurrences', () => {
    const training = {
      ...populatedTodayFixture.training,
      occurrences: [
        {
          occurrenceId: 'occ-1',
          trainingPlanId: 'plan-1',
          workoutDayId: 'day-1',
          trainingPlanName: 'Strength',
          workoutDayName: 'Upper',
          status: 'SCHEDULED',
          scheduledDate: '2026-08-10',
          exerciseCount: 8,
          completedExerciseCount: 0,
        },
      ],
    };

    const match = resolveOccurrenceForAdaptation('occ-1', training);
    expect(match?.trainingPlanId).toBe('plan-1');
  });

  it('resolves adaptation route from today adaptation + training', () => {
    const route = resolveAdaptationRouteFromToday(
      populatedTodayFixture.adaptation!,
      populatedTodayFixture.training,
    );
    expect(route).toEqual({
      planId: 'plan-1',
      dayId: 'day-1',
      occurrenceId: 'occ-1',
    });
  });
});
