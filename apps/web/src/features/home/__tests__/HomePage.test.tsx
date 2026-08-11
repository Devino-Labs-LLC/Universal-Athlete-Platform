import { describe, expect, it, vi } from 'vitest';

import { HomePage } from '@/features/home/pages/HomePage';
import { populatedTodayFixture } from '@/features/home/__tests__/fixtures/todayFixtures';
import { renderWithProviders, screen } from '@/test/utils';

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    account: { email: 'athlete@example.com' },
  }),
}));

vi.mock('@/app/providers/AthleteOnboardingProvider', () => ({
  useAthleteOnboarding: () => ({
    snapshot: { profile: { firstName: 'Alex' }, sports: [], goals: [] },
  }),
}));

vi.mock('@/features/home/hooks/useTodayDashboard', () => ({
  useTodayDashboard: () => ({
    isLoading: false,
    isError: false,
    data: populatedTodayFixture,
    refetch: vi.fn(),
  }),
}));

vi.mock('@/features/home/hooks/useDerivedStateMutations', () => ({
  useDerivedStateMutations: () => ({
    athleteStateMutation: { isPending: false, mutate: vi.fn() },
    readinessMutation: { isPending: false, mutate: vi.fn() },
    recommendationMutation: { isPending: false, mutate: vi.fn() },
    errorMessage: null,
  }),
}));

describe('HomePage', () => {
  it('renders greeting and dashboard cards from populated fixture', () => {
    renderWithProviders(<HomePage />);

    expect(screen.getByText(/Good (morning|afternoon|evening), Alex/)).toBeInTheDocument();
    expect(screen.getByText("Today's workout")).toBeInTheDocument();
    expect(screen.getByText('Readiness')).toBeInTheDocument();
    expect(screen.getByText('Recommendation')).toBeInTheDocument();
    expect(screen.getByText('Proceed as planned')).toBeInTheDocument();
    expect(screen.getByText('Lower Body')).toBeInTheDocument();
  });
});
