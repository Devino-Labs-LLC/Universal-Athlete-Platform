import { render, fireEvent, waitFor, cleanup, within } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { RecoveryOverviewScreen } from '@/src/features/recovery/screens/RecoveryOverviewScreen';

import { emptyOverviewFixture, overviewFixture } from './fixtures/overviewFixtures';
import { generationActionsFixture } from '../home/fixtures/todayFixtures';

jest.mock('expo-router', () => ({
  router: { push: jest.fn() },
}));

jest.mock('@/src/features/recovery/hooks/useRecoveryOverview', () => ({
  useRecoveryOverview: jest.fn(),
}));

jest.mock('@/src/features/home/hooks/useTodayDashboard', () => ({
  useTodayDashboard: jest.fn(),
}));

jest.mock('@/src/features/home/hooks/useDerivedStateMutations', () => ({
  useDerivedStateMutations: jest.fn(),
}));

const { useRecoveryOverview } = jest.requireMock('@/src/features/recovery/hooks/useRecoveryOverview');
const { useTodayDashboard } = jest.requireMock('@/src/features/home/hooks/useTodayDashboard');
const { useDerivedStateMutations } = jest.requireMock(
  '@/src/features/home/hooks/useDerivedStateMutations',
);

async function renderOverview() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <RecoveryOverviewScreen />
      </ThemeProvider>
    </QueryClientProvider>,
  );
}

describe('RecoveryOverviewScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useDerivedStateMutations.mockReturnValue({
      athleteStateMutation: { mutate: jest.fn(), isPending: false, error: null },
      regenerateAthleteStateMutation: { mutate: jest.fn(), isPending: false, error: null },
      readinessMutation: { mutate: jest.fn(), isPending: false, error: null },
      recommendationMutation: { mutate: jest.fn(), isPending: false, error: null },
      errorMessage: null,
    });
    useRecoveryOverview.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: overviewFixture,
      refetch: jest.fn(),
    });
    useTodayDashboard.mockReturnValue({
      isFetching: false,
      data: generationActionsFixture,
      refetch: jest.fn(),
    });
  });

  it('renders check-in, readiness, guidance, trends, and load sections', async () => {
    const { getByTestId, getByText } = await renderOverview();

    expect(getByTestId('recovery-overview-screen')).toBeTruthy();
    expect(getByTestId('recovery-check-in-section')).toBeTruthy();
    expect(getByTestId('insights-step-list')).toBeTruthy();
    expect(getByTestId('recovery-readiness-summary')).toBeTruthy();
    expect(getByTestId('recovery-guidance-summary')).toBeTruthy();
    expect(getByTestId('recovery-baselines-section')).toBeTruthy();
    expect(getByTestId('recovery-trends-section')).toBeTruthy();
    expect(getByTestId('training-load-context-card')).toBeTruthy();
    expect(getByText('Update Check In')).toBeTruthy();
  });

  it('shows check-in CTA when no check-in present', async () => {
    useRecoveryOverview.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: emptyOverviewFixture,
      refetch: jest.fn(),
    });

    const { getByTestId } = await renderOverview();
    const section = getByTestId('recovery-check-in-section');
    expect(within(section).getByText('Check In')).toBeTruthy();
  });
});
