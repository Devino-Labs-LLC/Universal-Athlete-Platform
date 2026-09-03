import { render } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { PerformanceOverviewScreen } from '@/src/features/performance/screens/PerformanceOverviewScreen';

jest.mock('expo-router', () => ({
  router: { push: jest.fn() },
}));

jest.mock('@/src/features/performance/hooks/useRecentPersonalRecords', () => ({
  useRecentPersonalRecords: jest.fn(),
}));
jest.mock('@/src/features/performance/hooks/useTrainingLoadHistory', () => ({
  useTrainingLoadHistory: jest.fn(),
}));
jest.mock('@/src/features/training/hooks/useTrainingOverview', () => ({
  useTrainingOverview: jest.fn(),
}));
jest.mock('@/src/features/recovery/hooks/useRecoveryHistory', () => ({
  useRecoveryHistory: jest.fn(),
}));

const { useRecentPersonalRecords } = jest.requireMock(
  '@/src/features/performance/hooks/useRecentPersonalRecords',
);
const { useTrainingLoadHistory } = jest.requireMock(
  '@/src/features/performance/hooks/useTrainingLoadHistory',
);
const { useTrainingOverview } = jest.requireMock(
  '@/src/features/training/hooks/useTrainingOverview',
);
const { useRecoveryHistory } = jest.requireMock(
  '@/src/features/recovery/hooks/useRecoveryHistory',
);

describe('PerformanceOverviewScreen', () => {
  it('shows an honest empty progress state when history is missing', async () => {
    useRecentPersonalRecords.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: [],
      refetch: jest.fn(),
    });
    useTrainingLoadHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: { weeklySummaries: [] },
      refetch: jest.fn(),
    });
    useTrainingOverview.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: { recentCompletedSessions: [] },
      refetch: jest.fn(),
    });
    useRecoveryHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: { days: [] },
      refetch: jest.fn(),
    });

    const { getByTestId, getByText } = await render(
      <QueryClientProvider client={new QueryClient()}>
        <ThemeProvider>
          <PerformanceOverviewScreen />
        </ThemeProvider>
      </QueryClientProvider>,
    );

    expect(getByTestId('performance-overview-screen')).toBeTruthy();
    expect(getByTestId('progress-summary-card')).toBeTruthy();
    expect(getByText('More training history is needed.')).toBeTruthy();
  });

  it('shows an insufficient-history headline when some data exists but not enough for a trend', async () => {
    useRecentPersonalRecords.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: [],
      refetch: jest.fn(),
    });
    useTrainingLoadHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: { weeklySummaries: [{ ratedOccurrenceCount: 1 }] },
      refetch: jest.fn(),
    });
    useTrainingOverview.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: { recentCompletedSessions: [{ occurrenceId: 'o1' }] },
      refetch: jest.fn(),
    });
    useRecoveryHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: { days: [{ date: '2026-08-10' }] },
      refetch: jest.fn(),
    });

    const { getByText } = await render(
      <QueryClientProvider client={new QueryClient()}>
        <ThemeProvider>
          <PerformanceOverviewScreen />
        </ThemeProvider>
      </QueryClientProvider>,
    );

    expect(getByText('Some history is on file, but not enough to show a trend.')).toBeTruthy();
    expect(getByText(/Sessions · 1 · Not enough yet/)).toBeTruthy();
  });

  it('shows stored progress counts when enough history exists, without inventing a trend', async () => {
    useRecentPersonalRecords.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: [{ id: 'pr-1', exerciseName: 'Back Squat' }],
      refetch: jest.fn(),
    });
    useTrainingLoadHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: {
        weeklySummaries: [
          { ratedOccurrenceCount: 2 },
          { ratedOccurrenceCount: 1 },
          { ratedOccurrenceCount: 1 },
        ],
      },
      refetch: jest.fn(),
    });
    useTrainingOverview.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: {
        recentCompletedSessions: [{ occurrenceId: 'o1' }, { occurrenceId: 'o2' }, { occurrenceId: 'o3' }],
        weeklyLoadSummary: { trainingDays: 3 },
      },
      refetch: jest.fn(),
    });
    useRecoveryHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: { days: [{ date: '2026-08-08' }, { date: '2026-08-09' }, { date: '2026-08-10' }] },
      refetch: jest.fn(),
    });

    const { getByText } = await render(
      <QueryClientProvider client={new QueryClient()}>
        <ThemeProvider>
          <PerformanceOverviewScreen />
        </ThemeProvider>
      </QueryClientProvider>,
    );

    expect(
      getByText('Progress from completed sessions, effort, records, and recovery check-ins.'),
    ).toBeTruthy();
    expect(getByText(/Sessions · 3 · Ready/)).toBeTruthy();
    expect(getByText(/Rated effort · 4 · Ready/)).toBeTruthy();
    expect(getByText(/Personal records · 1 · Ready/)).toBeTruthy();
  });
});
