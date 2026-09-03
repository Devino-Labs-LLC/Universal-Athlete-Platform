import { cleanup, fireEvent, render, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { TrainingOverviewScreen } from '@/src/features/training/screens/TrainingOverviewScreen';

import {
  emptyOverviewFixture,
  populatedOverviewFixture,
} from './fixtures/overviewFixtures';

const mockRefetch = jest.fn();
const mockPush = jest.fn();

jest.mock('expo-router', () => ({
  router: { push: (...args: unknown[]) => mockPush(...args) },
}));

jest.mock('@/src/features/training/hooks/useTrainingOverview', () => ({
  useTrainingOverview: jest.fn(),
}));

const { useTrainingOverview } = jest.requireMock(
  '@/src/features/training/hooks/useTrainingOverview',
);

async function renderOverview() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <TrainingOverviewScreen />
      </ThemeProvider>
    </QueryClientProvider>,
  );
}

describe('TrainingOverviewScreen', () => {
  beforeEach(() => {
    mockRefetch.mockClear();
    mockPush.mockClear();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders loading state', async () => {
    useTrainingOverview.mockReturnValue({
      isLoading: true,
      isError: false,
      isFetching: false,
      data: undefined,
      refetch: mockRefetch,
    });

    const { getByText } = await renderOverview();
    expect(getByText('Loading training overview…')).toBeTruthy();
  });

  it('renders error state with retry', async () => {
    useTrainingOverview.mockReturnValue({
      isLoading: false,
      isError: true,
      isFetching: false,
      data: undefined,
      error: new Error('Network failed'),
      refetch: mockRefetch,
    });

    const { getByText } = await renderOverview();
    fireEvent.press(getByText('Retry'));
    expect(mockRefetch).toHaveBeenCalledTimes(1);
  });

  it('renders populated sections', async () => {
    useTrainingOverview.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: populatedOverviewFixture,
      refetch: mockRefetch,
    });

    const screen = await renderOverview();

    expect(screen.getByTestId('training-overview-screen')).toBeTruthy();
    expect(screen.getByTestId('next-workout-card')).toBeTruthy();
    expect(screen.getByTestId('upcoming-section')).toBeTruthy();
    expect(screen.getByTestId('active-plans-section')).toBeTruthy();
    expect(screen.getByTestId('completed-section')).toBeTruthy();
    expect(screen.getByTestId('weekly-load-summary-card')).toBeTruthy();
    expect(screen.getByTestId('adaptations-section')).toBeTruthy();
  }, 15000);

  it('renders empty sections', async () => {
    useTrainingOverview.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: emptyOverviewFixture,
      refetch: mockRefetch,
    });

    const { getByText, getByTestId } = await renderOverview();

    expect(getByText('No upcoming workouts scheduled.')).toBeTruthy();
    expect(getByText('No active training plans.')).toBeTruthy();
    expect(getByText('No recently completed sessions.')).toBeTruthy();
    expect(getByTestId('create-personal-plan-cta')).toBeTruthy();
  });

  it('navigates to the personal plan path from the empty-plan CTA', async () => {
    useTrainingOverview.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: emptyOverviewFixture,
      refetch: mockRefetch,
    });

    const { getByTestId } = await renderOverview();
    fireEvent.press(getByTestId('create-personal-plan-cta'));
    expect(mockPush).toHaveBeenCalledWith('/(tabs)/training/create-plan');
  });

  it('navigates to calendar from CTA', async () => {
    useTrainingOverview.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: populatedOverviewFixture,
      refetch: mockRefetch,
    });

    const { getByText } = await renderOverview();
    fireEvent.press(getByText('Open Calendar'));

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/(tabs)/training/calendar');
    });
  });

  it('pull-to-refresh refetches overview', async () => {
    useTrainingOverview.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: true,
      data: populatedOverviewFixture,
      refetch: mockRefetch,
    });

    await renderOverview();
    expect(mockRefetch).not.toHaveBeenCalled();
  });
});
