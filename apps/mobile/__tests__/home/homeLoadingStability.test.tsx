import { cleanup, render } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { parseDateOnly } from '@/src/core/date/dateOnly';
import { HomeScreen } from '@/src/features/home/screens/HomeScreen';

import {
  emptyTodayFixture,
  populatedTodayFixture,
} from './fixtures/todayFixtures';

jest.mock('expo-router', () => ({
  router: { push: jest.fn() },
}));

jest.mock('@/src/features/home/hooks/useTodayDashboard', () => ({
  useTodayDashboard: jest.fn(),
}));

// Intentionally NOT mocking useDerivedStateMutations — live crash was a render throw there.

jest.mock('@/src/features/adaptation/hooks/useGenerateManualAdaptation', () => ({
  useGenerateManualAdaptation: () => ({
    mutate: jest.fn(),
    isPending: false,
    error: null,
  }),
}));

jest.mock('@/src/app/providers/AuthSessionProvider', () => ({
  useAuthSession: jest.fn(),
}));

jest.mock('@/src/app/providers/AthleteOnboardingProvider', () => ({
  useAthleteOnboarding: jest.fn(),
}));

const { useTodayDashboard } = jest.requireMock('@/src/features/home/hooks/useTodayDashboard');
const { useAuthSession } = jest.requireMock('@/src/app/providers/AuthSessionProvider');
const { useAthleteOnboarding } = jest.requireMock(
  '@/src/app/providers/AthleteOnboardingProvider',
);

async function renderHome() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <HomeScreen />
      </ThemeProvider>
    </QueryClientProvider>,
  );
}

describe('HomeScreen loading / missing-date stability', () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    useAuthSession.mockReturnValue({
      account: { email: 'ra1.user1@devinolabs.test' },
      apiClient: { axios: { get: jest.fn(), post: jest.fn() } },
      status: 'AUTHENTICATED',
    });
    useAthleteOnboarding.mockReturnValue({
      snapshot: { profile: { firstName: 'Jordan' }, sports: [], goals: [] },
      state: 'COMPLETE',
    });
  });

  it('does not throw while Today is loading (date absent)', async () => {
    useTodayDashboard.mockReturnValue({
      isLoading: true,
      isError: false,
      isFetching: true,
      data: undefined,
      error: null,
      refetch: jest.fn(),
    });

    const { getByTestId } = await renderHome();
    expect(getByTestId('home-skeleton')).toBeTruthy();
  });

  it('does not throw on authenticated transition while queries are still loading', async () => {
    useTodayDashboard.mockReturnValue({
      isLoading: true,
      isError: false,
      isFetching: true,
      data: undefined,
      error: null,
      refetch: jest.fn(),
    });

    const { getByTestId, queryByTestId } = await renderHome();
    expect(queryByTestId('home-screen-scroll')).toBeNull();
    expect(getByTestId('home-skeleton')).toBeTruthy();
  });

  it('renders stable empty Home for fresh-athlete Today (optional sections absent)', async () => {
    useTodayDashboard.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: emptyTodayFixture,
      error: null,
      refetch: jest.fn(),
    });

    const { getByTestId, getByText, getByLabelText, queryByTestId } = await renderHome();
    expect(getByTestId('home-screen-scroll')).toBeTruthy();
    expect(getByText('No workout scheduled for today.')).toBeTruthy();
    expect(getByTestId('readiness-card')).toBeTruthy();
    expect(getByTestId('readiness-score-ring')).toBeTruthy();
    expect(getByLabelText('Score: —')).toBeTruthy();
    expect(getByTestId('recommendation-card')).toBeTruthy();
    expect(getByTestId('recovery-card')).toBeTruthy();
    expect(queryByTestId('training-load-card')).toBeNull();
    expect(queryByTestId('adaptation-card')).toBeNull();
    expect(queryByTestId('recent-performance-card')).toBeNull();
  });

  it('still renders populated Home content', async () => {
    useTodayDashboard.mockReturnValue({
      isLoading: false,
      isError: false,
      isFetching: false,
      data: populatedTodayFixture,
      error: null,
      refetch: jest.fn(),
    });

    const { getByText, getByTestId } = await renderHome();
    expect(getByText('Proceed as planned')).toBeTruthy();
    expect(getByTestId('training-load-card')).toBeTruthy();
  });

  it('shows ErrorView when Today fails (no render crash)', async () => {
    useTodayDashboard.mockReturnValue({
      isLoading: false,
      isError: true,
      isFetching: false,
      data: undefined,
      error: new Error('Invalid today payload'),
      refetch: jest.fn(),
    });

    const { getByText } = await renderHome();
    expect(getByText('Failed to load today dashboard')).toBeTruthy();
    expect(getByText('Retry')).toBeTruthy();
  });
});

describe('useDerivedStateMutations missing date (via Home render path)', () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    useAuthSession.mockReturnValue({
      account: { email: 'ra1.user1@devinolabs.test' },
      apiClient: { axios: { get: jest.fn(), post: jest.fn() } },
      status: 'AUTHENTICATED',
    });
    useAthleteOnboarding.mockReturnValue({
      snapshot: { profile: { firstName: 'Jordan' }, sports: [], goals: [] },
      state: 'COMPLETE',
    });
  });

  it('passes undefined date into mutations while loading without crashing', async () => {
    useTodayDashboard.mockReturnValue({
      isLoading: true,
      isError: false,
      isFetching: true,
      data: undefined,
      error: null,
      refetch: jest.fn(),
    });

    await expect(renderHome()).resolves.toBeTruthy();
  });

  it('keeps parseDateOnly strict for non-empty invalid values', () => {
    expect(() => parseDateOnly('')).toThrow(/Invalid date-only value/);
    expect(() => parseDateOnly('not-a-date')).toThrow(/Invalid date-only value/);
  });
});
