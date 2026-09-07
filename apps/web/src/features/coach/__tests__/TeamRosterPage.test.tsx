import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiError } from '@/core/api/errors';
import { TeamRosterPage } from '@/features/coach/pages/TeamRosterPage';
import { renderWithProviders, screen } from '@/test/utils';

const refetch = vi.fn();

let mockRosterState: {
  isLoading: boolean;
  isError: boolean;
  data: unknown;
  error?: unknown;
  refetch: typeof refetch;
};

vi.mock('@/features/coach/hooks/useCoachQueries', () => ({
  useTeamRoster: () => mockRosterState,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ teamId: 'team-1' }),
  };
});

const roster = [
  {
    athleteId: 'ath-1',
    membershipId: 'mem-1',
    displayName: 'Alex Runner',
    role: 'ATHLETE' as const,
    status: 'ACTIVE' as const,
  },
];

describe('TeamRosterPage', () => {
  beforeEach(() => {
    refetch.mockReset();
  });

  it('shows loading state', () => {
    mockRosterState = {
      isLoading: true,
      isError: false,
      data: undefined,
      refetch,
    };
    renderWithProviders(<TeamRosterPage />);
    expect(screen.getByText('Loading roster…')).toBeInTheDocument();
  });

  it('shows empty roster', () => {
    mockRosterState = {
      isLoading: false,
      isError: false,
      data: [],
      refetch,
    };
    renderWithProviders(<TeamRosterPage />);
    expect(screen.getByText('No athletes')).toBeInTheDocument();
  });

  it('renders roster-safe athlete links', () => {
    mockRosterState = {
      isLoading: false,
      isError: false,
      data: roster,
      refetch,
    };
    renderWithProviders(<TeamRosterPage />);
    const link = screen.getByRole('link', { name: 'Alex Runner' });
    expect(link).toHaveAttribute('href', '/coach/teams/team-1/athletes/ath-1');
    expect(screen.getByText('Athlete · Active')).toBeInTheDocument();
  });

  it('shows unavailable for 404', () => {
    mockRosterState = {
      isLoading: false,
      isError: true,
      data: undefined,
      error: new ApiError('missing', { category: 'NOT_FOUND', status: 404 }),
      refetch,
    };
    renderWithProviders(<TeamRosterPage />);
    expect(screen.getByText('Team unavailable')).toBeInTheDocument();
  });

  it('shows retryable error for non-404 failures', () => {
    mockRosterState = {
      isLoading: false,
      isError: true,
      data: undefined,
      error: new ApiError('boom', { category: 'SERVER', status: 500 }),
      refetch,
    };
    renderWithProviders(<TeamRosterPage />);
    expect(screen.getByText('boom')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });
});
