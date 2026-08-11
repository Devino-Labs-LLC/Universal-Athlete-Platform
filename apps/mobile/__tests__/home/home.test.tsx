import { cleanup, fireEvent, render, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { HomeScreen } from '@/src/features/home/screens/HomeScreen';
import { todayQueryKeys } from '@/src/features/home/models/queryKeys';

import {
  emptyTodayFixture,
  generationActionsFixture,
  populatedTodayFixture,
} from './fixtures/todayFixtures';

const mockRefetch = jest.fn();
const mockMutateAthleteState = jest.fn();
const mockMutateReadiness = jest.fn();
const mockMutateRecommendation = jest.fn();

jest.mock('expo-router', () => ({
  router: { push: jest.fn() },
}));

jest.mock('@/src/features/home/hooks/useTodayDashboard', () => ({
  useTodayDashboard: jest.fn(),
}));

jest.mock('@/src/features/home/hooks/useDerivedStateMutations', () => ({
  useDerivedStateMutations: jest.fn(),
}));

jest.mock('@/src/app/providers/AuthSessionProvider', () => ({
  useAuthSession: jest.fn(),
}));

jest.mock('@/src/app/providers/AthleteOnboardingProvider', () => ({
  useAthleteOnboarding: jest.fn(),
}));

const { useTodayDashboard } = jest.requireMock('@/src/features/home/hooks/useTodayDashboard');
const { useDerivedStateMutations } = jest.requireMock(
  '@/src/features/home/hooks/useDerivedStateMutations',
);
const { useAuthSession } = jest.requireMock('@/src/app/providers/AuthSessionProvider');
const { useAthleteOnboarding } = jest.requireMock(
  '@/src/app/providers/AthleteOnboardingProvider',
);

async function renderHome() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <HomeScreen />
      </ThemeProvider>
    </QueryClientProvider>,
  );
}

function setupTodayQuery(overrides: Record<string, unknown> = {}) {
  useTodayDashboard.mockReturnValue({
    isLoading: false,
    isError: false,
    isFetching: false,
    data: populatedTodayFixture,
    refetch: mockRefetch,
    ...overrides,
  });
}

function setupMutations(overrides: Record<string, unknown> = {}) {
  useDerivedStateMutations.mockReturnValue({
    athleteStateMutation: { mutate: mockMutateAthleteState, isPending: false, error: null },
    regenerateAthleteStateMutation: { mutate: jest.fn(), isPending: false, error: null },
    readinessMutation: { mutate: mockMutateReadiness, isPending: false, error: null },
    recommendationMutation: { mutate: mockMutateRecommendation, isPending: false, error: null },
    errorMessage: null,
    ...overrides,
  });
}

describe('HomeScreen', () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    useAuthSession.mockReturnValue({
      account: { email: 'jordan@example.com' },
    });
    useAthleteOnboarding.mockReturnValue({
      snapshot: { profile: { firstName: 'Jordan' }, sports: [], goals: [] },
    });
    setupTodayQuery();
    setupMutations();
  });

  it('renders loading skeleton on initial load', async () => {
    useTodayDashboard.mockReturnValue({
      isLoading: true,
      isError: false,
      isFetching: false,
      data: undefined,
      refetch: mockRefetch,
    });

    const { getByTestId } = await renderHome();
    expect(getByTestId('home-skeleton')).toBeTruthy();
  });

  it('renders greeting and populated cards', async () => {
    const { getByText, getByTestId } = await renderHome();

    expect(getByText(/Good (morning|afternoon|evening), Jordan/)).toBeTruthy();
    expect(getByTestId('readiness-card')).toBeTruthy();
    expect(getByTestId('recommendation-card')).toBeTruthy();
    expect(getByTestId('recovery-card')).toBeTruthy();
    expect(getByTestId('training-load-card')).toBeTruthy();
    expect(getByTestId('adaptation-card')).toBeTruthy();
    expect(getByTestId('recent-performance-card')).toBeTruthy();
    expect(getByText('High')).toBeTruthy();
    expect(getByText('Proceed as planned')).toBeTruthy();
  });

  it('omits load and performance cards when empty', async () => {
    setupTodayQuery({ data: emptyTodayFixture });

    const { queryByTestId } = await renderHome();

    expect(queryByTestId('training-load-card')).toBeNull();
    expect(queryByTestId('recent-performance-card')).toBeNull();
    expect(queryByTestId('adaptation-card')).toBeNull();
  });

  it('shows enabled quick actions from action flags', async () => {
    setupTodayQuery({ data: generationActionsFixture });

    const { getByTestId } = await renderHome();

    expect(getByTestId('quick-action-state')).toBeTruthy();
    expect(getByTestId('quick-action-readiness')).toBeTruthy();
    expect(getByTestId('quick-action-guidance')).toBeTruthy();
  });

  it('invokes generation mutations only on explicit tap', async () => {
    setupTodayQuery({ data: generationActionsFixture });

    const { getByTestId } = await renderHome();

    fireEvent.press(getByTestId('quick-action-state'));
    fireEvent.press(getByTestId('quick-action-readiness'));
    fireEvent.press(getByTestId('quick-action-guidance'));

    expect(mockMutateAthleteState).toHaveBeenCalledTimes(1);
    expect(mockMutateReadiness).toHaveBeenCalledTimes(1);
    expect(mockMutateRecommendation).toHaveBeenCalledTimes(1);
  });

  it('does not invoke mutations on mount', async () => {
    setupTodayQuery({ data: generationActionsFixture });

    await renderHome();

    expect(mockMutateAthleteState).not.toHaveBeenCalled();
    expect(mockMutateReadiness).not.toHaveBeenCalled();
    expect(mockMutateRecommendation).not.toHaveBeenCalled();
  });
});

describe('useDerivedStateMutations invalidate', () => {
  it('uses today query keys for invalidation scope', () => {
    expect(todayQueryKeys.date()).toEqual(['training', 'today', 'current']);
    expect(todayQueryKeys.all).toEqual(['training', 'today']);
  });
});

describe('no hidden write requests on Home mount', () => {
  afterEach(() => {
    cleanup();
  });

  it('does not call axios.post when Home renders', async () => {
    const axiosPost = jest.fn();

    useAuthSession.mockReturnValue({
      account: { email: 'jordan@example.com' },
      apiClient: { axios: { post: axiosPost, get: jest.fn() } },
    });
    setupTodayQuery({ data: generationActionsFixture });
    setupMutations();

    await renderHome();

    await waitFor(() => {
      expect(axiosPost).not.toHaveBeenCalled();
    });
  });
});
