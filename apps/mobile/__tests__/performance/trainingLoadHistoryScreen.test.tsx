import { cleanup, fireEvent, render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { TrainingLoadHistoryScreen } from '@/src/features/performance/screens/TrainingLoadHistoryScreen';

const mockRefetch = jest.fn();

jest.mock('@/src/features/performance/hooks/useTrainingLoadHistory', () => ({
  useTrainingLoadHistory: jest.fn(),
}));

const { useTrainingLoadHistory } = jest.requireMock(
  '@/src/features/performance/hooks/useTrainingLoadHistory',
);

const weeklySummary = {
  weekStartDate: '2026-01-26',
  weekEndDate: '2026-02-01',
  trainingDays: 3,
  occurrenceCount: 3,
  ratedOccurrenceCount: 3,
  unratedOccurrenceCount: 0,
  completedExerciseCount: 15,
  completedSetCount: 45,
  completedRepetitionCount: 400,
  totalVolumeKilograms: 3200,
  totalDurationSeconds: 5400,
  totalDistanceMeters: 0,
  totalSessionRpeLoad: 800,
  averageSessionRpe: 7,
  totalSessionDurationMinutes: 90,
  noImpactExerciseCount: 0,
  lowImpactExerciseCount: 15,
  moderateImpactExerciseCount: 0,
  highImpactExerciseCount: 0,
};

describe('TrainingLoadHistoryScreen', () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows an honest empty state when no weekly load exists', async () => {
    useTrainingLoadHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: { weeklySummaries: [], dailySummaries: [], occurrences: [] },
      refetch: mockRefetch,
    });

    const { getByTestId, getByText } = await render(
      <ThemeProvider>
        <TrainingLoadHistoryScreen />
      </ThemeProvider>,
    );

    expect(getByTestId('training-load-history-screen')).toBeTruthy();
    expect(getByText('No training load data for this period.')).toBeTruthy();
  });

  it('renders stored weekly load without inventing a trend', async () => {
    useTrainingLoadHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: { weeklySummaries: [weeklySummary], dailySummaries: [], occurrences: [] },
      refetch: mockRefetch,
    });

    const { getByTestId, getByText } = await render(
      <ThemeProvider>
        <TrainingLoadHistoryScreen />
      </ThemeProvider>,
    );

    expect(getByTestId('load-history-weekly-2026-01-26')).toBeTruthy();
    expect(getByText(/3 training days/)).toBeTruthy();
  });

  it('shows a retryable error when load history cannot be loaded', async () => {
    useTrainingLoadHistory.mockReturnValue({
      isLoading: false,
      isError: true,
      isFetching: false,
      data: undefined,
      error: new Error('Failed to load training load history'),
      refetch: mockRefetch,
    });

    const { getByText } = await render(
      <ThemeProvider>
        <TrainingLoadHistoryScreen />
      </ThemeProvider>,
    );

    expect(getByText('Failed to load training load history')).toBeTruthy();
    fireEvent.press(getByText('Retry'));
    expect(mockRefetch).toHaveBeenCalledTimes(1);
  });
});
