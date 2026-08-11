import { render, fireEvent } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { HomeQuickActions } from '@/src/features/home/components/HomeQuickActions';
import { RecoveryCard } from '@/src/features/home/components/RecoveryCard';

jest.mock('expo-router', () => ({
  router: { push: jest.fn() },
}));

const { router } = jest.requireMock('expo-router');

describe('Home recovery routes', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('RecoveryCard routes to check-in screen', async () => {
    const { getByText } = await render(
      <ThemeProvider>
        <RecoveryCard
          recovery={{ checkInPresent: false }}
          canCreateRecoveryCheckIn={{ allowed: true }}
        />
      </ThemeProvider>,
    );

    fireEvent.press(getByText('Check In'));
    expect(router.push).toHaveBeenCalledWith('/(tabs)/recovery/check-in');
  });

  it('HomeQuickActions check-in routes to check-in screen', async () => {
    const { getByTestId } = await render(
      <ThemeProvider>
        <HomeQuickActions
          actions={{
            canCreateRecoveryCheckIn: { allowed: true },
            canUpdateRecoveryCheckIn: { allowed: false },
            canGenerateAthleteStateSnapshot: { allowed: false },
            canGenerateReadinessAssessment: { allowed: false },
            canGenerateTrainingRecommendation: { allowed: false },
            canStartWorkout: { allowed: false },
            canContinueWorkout: { allowed: false },
            canGenerateAdaptationProposal: { allowed: false },
          }}
          onGenerateDailyState={jest.fn()}
          onCalculateReadiness={jest.fn()}
          onGenerateGuidance={jest.fn()}
        />
      </ThemeProvider>,
    );

    fireEvent.press(getByTestId('quick-action-check-in'));
    expect(router.push).toHaveBeenCalledWith('/(tabs)/recovery/check-in');
  });
});
