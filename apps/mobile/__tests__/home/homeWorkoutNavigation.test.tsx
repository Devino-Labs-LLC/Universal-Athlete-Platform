import { fireEvent, render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { HomeQuickActions } from '@/src/features/home/components/HomeQuickActions';
import { PrimaryWorkoutCard } from '@/src/features/home/components/PrimaryWorkoutCard';

import {
  inProgressTodayFixture,
  populatedTodayFixture,
} from './fixtures/todayFixtures';

const mockPush = jest.fn();

jest.mock('expo-router', () => ({
  router: { push: (...args: unknown[]) => mockPush(...args) },
}));

const scheduledOccurrence = populatedTodayFixture.training.primaryOccurrence!;
const inProgressOccurrence = inProgressTodayFixture.training.primaryOccurrence!;
const completedOccurrence = {
  ...scheduledOccurrence,
  status: 'COMPLETED',
  completedExerciseCount: scheduledOccurrence.exerciseCount,
};

const noop = () => undefined;

describe('Home workout Start/Continue routing', () => {
  beforeEach(() => {
    mockPush.mockClear();
  });

  it('routes a scheduled Home Start Workout to launch/prep', async () => {
    const { getByText } = await render(
      <ThemeProvider>
        <PrimaryWorkoutCard
          occurrence={scheduledOccurrence}
          canStartWorkout={{ allowed: true }}
          canContinueWorkout={{ allowed: false }}
          dominant
        />
      </ThemeProvider>,
    );

    fireEvent.press(getByText('Start Workout'));

    expect(mockPush).toHaveBeenCalledWith(
      '/(tabs)/training/plans/plan-1/days/day-1/occurrences/occ-1/launch',
    );
  });

  it('routes an in-progress Home Continue Workout to execute/resume', async () => {
    const { getByText } = await render(
      <ThemeProvider>
        <PrimaryWorkoutCard
          occurrence={inProgressOccurrence}
          canStartWorkout={{ allowed: false }}
          canContinueWorkout={{ allowed: true }}
          dominant
        />
      </ThemeProvider>,
    );

    fireEvent.press(getByText('Continue Workout'));

    expect(mockPush).toHaveBeenCalledWith(
      '/(tabs)/training/plans/plan-1/days/day-1/occurrences/occ-1/execute',
    );
  });

  it('does not show Start or Continue for a completed workout', async () => {
    const { queryByText, getByText } = await render(
      <ThemeProvider>
        <PrimaryWorkoutCard
          occurrence={completedOccurrence}
          canStartWorkout={{ allowed: false }}
          canContinueWorkout={{ allowed: false }}
          dominant
        />
      </ThemeProvider>,
    );

    expect(queryByText('Start Workout')).toBeNull();
    expect(queryByText('Continue Workout')).toBeNull();
    expect(getByText('View Training')).toBeTruthy();
  });

  it('falls back to Training when Start is allowed but occurrence identity is missing', async () => {
    const { getByTestId } = await render(
      <ThemeProvider>
        <HomeQuickActions
          actions={{
            canCreateRecoveryCheckIn: { allowed: false },
            canUpdateRecoveryCheckIn: { allowed: false },
            canGenerateAthleteStateSnapshot: { allowed: false },
            canGenerateReadinessAssessment: { allowed: false },
            canGenerateTrainingRecommendation: { allowed: false },
            canGenerateAdaptationProposal: { allowed: false },
            canStartWorkout: { allowed: true },
            canContinueWorkout: { allowed: false },
            canSubmitSessionEffort: { allowed: false },
          }}
          primaryOccurrence={null}
          onGenerateDailyState={noop}
          onCalculateReadiness={noop}
          onGenerateGuidance={noop}
        />
      </ThemeProvider>,
    );

    fireEvent.press(getByTestId('quick-action-start'));

    expect(mockPush).toHaveBeenCalledWith('/(tabs)/training');
  });
});
