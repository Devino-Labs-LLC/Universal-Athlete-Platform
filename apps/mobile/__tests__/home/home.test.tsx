import { cleanup, fireEvent, render, waitFor, within } from '@testing-library/react-native';
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
const { router } = jest.requireMock('expo-router');

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
    // Must pass undefined — never '' — while Today date is absent.
    expect(useDerivedStateMutations).toHaveBeenCalledWith(undefined);
  });

  it('renders greeting and populated cards with readiness hero hierarchy', async () => {
    const { getByText, getByTestId, getByLabelText } = await renderHome();

    expect(getByText(/Good (morning|afternoon|evening), Jordan/)).toBeTruthy();
    expect(getByTestId('today-header')).toBeTruthy();
    expect(getByTestId('readiness-card')).toBeTruthy();
    expect(getByTestId('readiness-score-ring')).toBeTruthy();
    expect(getByLabelText('Score: 79')).toBeTruthy();
    expect(getByTestId('primary-workout-card')).toBeTruthy();
    expect(getByTestId('recommendation-card')).toBeTruthy();
    expect(getByTestId('recovery-card')).toBeTruthy();
    expect(getByTestId('training-load-card')).toBeTruthy();
    expect(getByTestId('adaptation-card')).toBeTruthy();
    expect(getByTestId('recent-performance-card')).toBeTruthy();
    expect(getByText('High')).toBeTruthy();
    expect(getByText('Proceed as planned')).toBeTruthy();
    expect(getByText(/Sleep is a limiting factor from today's evidence/)).toBeTruthy();
    // Hierarchy is readiness hero first; quick actions remain secondary when flags allow.
    expect(getByTestId('home-quick-actions')).toBeTruthy();
  });

  it('omits load and performance cards when empty', async () => {
    setupTodayQuery({ data: emptyTodayFixture });

    const { queryByTestId, getByTestId } = await renderHome();

    expect(queryByTestId('training-load-card')).toBeNull();
    expect(queryByTestId('recent-performance-card')).toBeNull();
    expect(queryByTestId('adaptation-card')).toBeNull();
    expect(queryByTestId('insights-step-list')).toBeTruthy();
    expect(within(getByTestId('readiness-card')).getByText(/No recovery check-in today/)).toBeTruthy();
  });

  it('hides the insights pipeline when today’s intelligence is already generated', async () => {
    const { queryByTestId } = await renderHome();
    expect(queryByTestId('insights-step-list')).toBeNull();
  });

  it('opens readiness and guidance details from Home cards', async () => {
    const { getByTestId } = await renderHome();

    fireEvent.press(getByTestId('readiness-card'));
    expect(router.push).toHaveBeenCalledWith('/(tabs)/recovery/readiness/assess-1');

    fireEvent.press(getByTestId('recommendation-card'));
    expect(router.push).toHaveBeenCalledWith('/(tabs)/recovery/guidance/rec-1');
  });

  it('shows the insights pipeline as the next daily action', async () => {
    setupTodayQuery({ data: generationActionsFixture });

    const { getByTestId, getByText, queryByTestId } = await renderHome();

    await waitFor(() => {
      expect(getByTestId('home-insights')).toBeTruthy();
    });
    expect(getByTestId('insights-step-list')).toBeTruthy();
    expect(getByText('Check in')).toBeTruthy();
    expect(queryByTestId('quick-action-state')).toBeNull();
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
