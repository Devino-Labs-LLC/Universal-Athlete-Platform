import { describe, expect, it, vi } from 'vitest';

import { HomePage } from '@/features/home/pages/HomePage';
import {
  freshAthleteTodayFixture,
  populatedTodayFixture,
} from '@/features/home/__tests__/fixtures/todayFixtures';
import { todayDashboardSchema } from '@/features/home/schemas';
import { renderWithProviders, screen } from '@/test/utils';

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    account: { email: 'ra1.user1@devinolabs.test' },
    apiClient: { axios: {} },
    status: 'AUTHENTICATED',
  }),
}));

vi.mock('@/app/providers/AthleteOnboardingProvider', () => ({
  useAthleteOnboarding: () => ({
    snapshot: {
      profile: { firstName: 'Jordan' },
      sports: [{ sportCode: 'RUNNING' }],
      goals: [{ goalType: 'GENERAL_FITNESS' }],
    },
    state: 'COMPLETE',
  }),
}));

const todayState = vi.hoisted(() => ({
  current: {
    isLoading: false,
    isError: false,
    data: undefined as unknown,
    error: null as unknown,
    refetch: vi.fn(),
  },
}));

vi.mock('@/features/home/hooks/useTodayDashboard', () => ({
  useTodayDashboard: () => todayState.current,
}));

// Intentionally NOT mocking useDerivedStateMutations — live blank was a render throw there.

describe('HomePage fresh-athlete / loading stability', () => {
  it('does not blank while Today is loading (date absent)', () => {
    todayState.current = {
      isLoading: true,
      isError: false,
      data: undefined,
      error: null,
      refetch: vi.fn(),
    };

    expect(() => renderWithProviders(<HomePage />)).not.toThrow();
    expect(screen.getByText('Loading today dashboard…')).toBeInTheDocument();
    expect(screen.queryByText("Today's workout")).not.toBeInTheDocument();
  });

  it('renders stable empty/CTA states for a valid fresh-athlete Today payload', () => {
    const parsed = todayDashboardSchema.parse(freshAthleteTodayFixture);
    todayState.current = {
      isLoading: false,
      isError: false,
      data: parsed,
      error: null,
      refetch: vi.fn(),
    };

    expect(() => renderWithProviders(<HomePage />)).not.toThrow();

    expect(screen.getByText(/Good (morning|afternoon|evening), Jordan/)).toBeInTheDocument();
    expect(screen.getByText("Today's workout")).toBeInTheDocument();
    expect(screen.getByText('No workout scheduled for today.')).toBeInTheDocument();
    expect(screen.getByText('No readiness assessment yet.')).toBeInTheDocument();
    expect(screen.getByText('No training recommendation yet.')).toBeInTheDocument();
    expect(screen.getByText('No recovery check-in yet.')).toBeInTheDocument();
    expect(screen.getByText('No training load data yet.')).toBeInTheDocument();
    expect(screen.getByText('No active adaptation proposal.')).toBeInTheDocument();
    expect(screen.getByText('No recent personal records.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Generate Daily State' })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Check in' }).length).toBeGreaterThan(0);
  });

  it('still renders populated Home content', () => {
    todayState.current = {
      isLoading: false,
      isError: false,
      data: populatedTodayFixture,
      error: null,
      refetch: vi.fn(),
    };

    renderWithProviders(<HomePage />);
    expect(screen.getByText('Lower Body')).toBeInTheDocument();
    expect(screen.getByText('Proceed as planned')).toBeInTheDocument();
  });

  it('shows safe ErrorView when Today fails (no blank screen)', () => {
    todayState.current = {
      isLoading: false,
      isError: true,
      data: undefined,
      error: new Error('Invalid today payload'),
      refetch: vi.fn(),
    };

    expect(() => renderWithProviders(<HomePage />)).not.toThrow();
    expect(screen.getByText('Failed to load today dashboard')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
  });
});
