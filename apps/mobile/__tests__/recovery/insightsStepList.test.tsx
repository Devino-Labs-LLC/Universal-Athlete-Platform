import { fireEvent, render } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { InsightsStepList } from '@/src/features/recovery/components/InsightsStepList';

const mockMutateState = jest.fn();
const mockMutateReadiness = jest.fn();
const mockMutateGuidance = jest.fn();

jest.mock('@/src/features/home/hooks/useDerivedStateMutations', () => ({
  useDerivedStateMutations: jest.fn(),
}));

jest.mock('expo-router', () => ({
  router: { push: jest.fn() },
}));

const { useDerivedStateMutations } = jest.requireMock(
  '@/src/features/home/hooks/useDerivedStateMutations',
);

describe('InsightsStepList', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useDerivedStateMutations.mockReturnValue({
      athleteStateMutation: { mutate: mockMutateState, isPending: false, error: null },
      readinessMutation: { mutate: mockMutateReadiness, isPending: false, error: null },
      recommendationMutation: { mutate: mockMutateGuidance, isPending: false, error: null },
      errorMessage: null,
    });
  });

  it('invokes athlete state mutation without readiness when generating state', async () => {
    const { getByText } = await render(
      <QueryClientProvider client={new QueryClient()}>
        <ThemeProvider>
          <InsightsStepList
            date="2026-08-10"
            overviewCheckInPresent
            athleteState={{ snapshotPresent: false }}
            actions={{
              canGenerateAthleteStateSnapshot: { allowed: true },
              canGenerateReadinessAssessment: { allowed: false },
              canGenerateTrainingRecommendation: { allowed: false },
              canCreateRecoveryCheckIn: { allowed: false },
              canUpdateRecoveryCheckIn: { allowed: true },
              canStartWorkout: { allowed: false },
              canContinueWorkout: { allowed: false },
              canGenerateAdaptationProposal: { allowed: false },
            }}
          />
        </ThemeProvider>
      </QueryClientProvider>,
    );

    fireEvent.press(getByText('Generate Daily State'));

    expect(mockMutateState).toHaveBeenCalledTimes(1);
    expect(mockMutateReadiness).not.toHaveBeenCalled();
    expect(mockMutateGuidance).not.toHaveBeenCalled();
  });

  it('offers generate daily state only after check-in is present', async () => {
    const { getByText, queryByText } = await render(
      <QueryClientProvider client={new QueryClient()}>
        <ThemeProvider>
          <InsightsStepList
            date="2026-08-10"
            overviewCheckInPresent
            athleteState={{ snapshotPresent: false }}
            actions={{
              canGenerateAthleteStateSnapshot: { allowed: true },
              canGenerateReadinessAssessment: { allowed: false },
              canGenerateTrainingRecommendation: { allowed: false },
              canCreateRecoveryCheckIn: { allowed: false },
              canUpdateRecoveryCheckIn: { allowed: true },
              canStartWorkout: { allowed: false },
              canContinueWorkout: { allowed: false },
              canGenerateAdaptationProposal: { allowed: false },
            }}
          />
        </ThemeProvider>
      </QueryClientProvider>,
    );

    expect(queryByText('Check in')).toBeNull();
    fireEvent.press(getByText('Generate Daily State'));
    expect(mockMutateState).toHaveBeenCalledTimes(1);
  });
});
