import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { TeamReadinessPage } from '@/features/coach/pages/TeamReadinessPage';

vi.mock('@/features/coach/hooks/useCoachQueries', () => ({
  useTeamReadiness: () => ({
    isLoading: false,
    isError: false,
    error: null,
    refetch: vi.fn(),
    data: {
      teamId: 'team-1',
      date: '2026-09-07',
      status: 'PUBLISHED',
      cohort: 'AT_LEAST_MINIMUM',
      includedCount: null,
      categoryDistribution: {
        status: 'PUBLISHED',
        cohort: 'AT_LEAST_MINIMUM',
        includedCount: null,
        cells: [
          { category: 'HIGH', publication: 'SUPPRESSED' },
          { category: 'LOW', publication: 'PUBLISHED', count: 6 },
        ],
      },
      limitingDimensionDistribution: {
        status: 'INSUFFICIENT_DATA',
        cohort: 'BELOW_MINIMUM',
        cells: [],
      },
      availability: { status: 'UNSUPPORTED' },
    },
  }),
}));

describe('TeamReadinessPage', () => {
  it('renders server suppression without reconstructing hidden counts', () => {
    render(
      <MemoryRouter initialEntries={['/coach/teams/team-1/readiness']}>
        <Routes>
          <Route path="/coach/teams/:teamId/readiness" element={<TeamReadinessPage />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText(/not a team score/i)).toBeInTheDocument();
    expect(screen.getByText(/High:/)).toHaveTextContent('Suppressed');
    expect(screen.getByText(/Low:/)).toHaveTextContent('6');
    expect(screen.queryByText('3')).not.toBeInTheDocument();
  });
});
