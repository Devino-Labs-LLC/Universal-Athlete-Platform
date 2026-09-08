import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { ApiError } from '@/core/api/errors';
import { TeamReadinessPage } from '@/features/coach/pages/TeamReadinessPage';

const useTeamReadiness = vi.fn();

vi.mock('@/features/coach/hooks/useCoachQueries', () => ({
  useTeamReadiness: (...args: unknown[]) => useTeamReadiness(...args),
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/coach/teams/team-1/readiness']}>
      <Routes>
        <Route path="/coach/teams/:teamId/readiness" element={<TeamReadinessPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('TeamReadinessPage', () => {
  it('renders server suppression without reconstructing hidden counts', () => {
    useTeamReadiness.mockReturnValue({
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
    });

    renderPage();

    expect(screen.getByText(/not a team score/i)).toBeInTheDocument();
    expect(screen.getByText(/High:/)).toHaveTextContent('Suppressed');
    expect(screen.getByText(/Low:/)).toHaveTextContent('6');
    expect(screen.getByText(/not enough shared limiting-dimension/i)).toBeInTheDocument();
    expect(screen.queryByText('3')).not.toBeInTheDocument();
  });

  it('renders insufficient cohort without a sample count', () => {
    useTeamReadiness.mockReturnValue({
      isLoading: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
      data: {
        teamId: 'team-1',
        date: '2026-09-07',
        status: 'INSUFFICIENT_DATA',
        cohort: 'BELOW_MINIMUM',
        categoryDistribution: { status: 'INSUFFICIENT_DATA', cohort: 'BELOW_MINIMUM', cells: [] },
        limitingDimensionDistribution: { status: 'INSUFFICIENT_DATA', cohort: 'BELOW_MINIMUM', cells: [] },
        availability: { status: 'UNSUPPORTED' },
      },
    });

    renderPage();

    expect(screen.getByText(/not enough shared readiness/i)).toBeInTheDocument();
    expect(screen.queryByText(/Included athletes/i)).not.toBeInTheDocument();
  });

  it('renders inaccessible and server error states', () => {
    useTeamReadiness.mockReturnValue({
      isLoading: false,
      isError: true,
      error: new ApiError('missing', { category: 'NOT_FOUND', status: 404 }),
      refetch: vi.fn(),
      data: undefined,
    });
    const { unmount } = renderPage();
    expect(screen.getByText(/unavailable or you do not have access/i)).toBeInTheDocument();
    unmount();

    const refetch = vi.fn();
    useTeamReadiness.mockReturnValue({
      isLoading: false,
      isError: true,
      error: new ApiError('', { category: 'SERVER', status: 500 }),
      refetch,
      data: undefined,
    });
    renderPage();
    expect(screen.getByText(/unable to load team readiness/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(refetch).toHaveBeenCalled();
  });

  it('renders loading, exact included count, and published limiting dimensions', () => {
    useTeamReadiness.mockReturnValue({
      isLoading: true,
      isError: false,
      error: null,
      refetch: vi.fn(),
      data: undefined,
    });
    const { unmount } = renderPage();
    expect(screen.getByText(/loading team readiness/i)).toBeInTheDocument();
    unmount();

    useTeamReadiness.mockReturnValue({
      isLoading: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
      data: {
        teamId: 'team-1',
        date: '2026-09-07',
        status: 'PUBLISHED',
        cohort: 'EXACT',
        includedCount: 20,
        categoryDistribution: {
          status: 'PUBLISHED',
          cohort: 'EXACT',
          includedCount: 20,
          cells: [
            { category: 'HIGH', publication: 'PUBLISHED', count: 5 },
            { category: 'MODERATE', publication: 'PUBLISHED', count: 5 },
            { category: 'LOW', publication: 'PUBLISHED', count: 5 },
            { category: 'INSUFFICIENT_DATA', publication: 'PUBLISHED', count: 5 },
            { category: 'CUSTOM', publication: 'SUPPRESSED' },
          ],
        },
        limitingDimensionDistribution: {
          status: 'PUBLISHED',
          cohort: 'EXACT',
          includedCount: 20,
          cells: [{ dimension: 'FATIGUE', publication: 'PUBLISHED', count: 6 }],
        },
        availability: { status: 'UNSUPPORTED' },
      },
    });
    renderPage();
    expect(screen.getByText(/included athletes with stored shared readiness: 20/i)).toBeInTheDocument();
    expect(screen.getByText(/Moderate:/)).toHaveTextContent('5');
    expect(screen.getByText(/Insufficient stored data:/)).toHaveTextContent('5');
    expect(screen.getByText(/CUSTOM:/)).toHaveTextContent('Suppressed');
    expect(screen.getByText(/FATIGUE:/)).toHaveTextContent('6');
    expect(screen.getByText(/does not assign a team score/i)).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Date'), { target: { value: '2026-09-06' } });
    expect(useTeamReadiness).toHaveBeenCalled();
  });
});
