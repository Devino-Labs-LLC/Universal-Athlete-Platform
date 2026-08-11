import { Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { SessionPerformancePage } from '@/features/performance/pages/SessionPerformancePage';
import { renderWithProviders, screen } from '@/test/utils';

const useOccurrencePerformance = vi.fn();

vi.mock('@/features/performance/hooks/useOccurrencePerformance', () => ({
  useOccurrencePerformance: (...args: unknown[]) => useOccurrencePerformance(...args),
}));

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/app/performance/sessions/:planId/:dayId/:occurrenceId" element={<SessionPerformancePage />} />
    </Routes>,
    { initialEntries: ['/app/performance/sessions/plan-1/day-1/occ-1'] },
  );
}

describe('SessionPerformancePage', () => {
  it('passes planId/dayId/occurrenceId from the nested route to the hook', () => {
    useOccurrencePerformance.mockReturnValue({ isLoading: true, isError: false, data: undefined, refetch: vi.fn() });
    renderPage();
    expect(useOccurrencePerformance).toHaveBeenCalledWith('plan-1', 'day-1', 'occ-1');
  });

  it('shows an empty state rather than a table when there are no completed exercises', () => {
    useOccurrencePerformance.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        occurrenceId: 'occ-1',
        scheduledDate: '2026-02-01',
        status: 'COMPLETED',
        totals: { completedExerciseCount: 0, completedSetCount: 0, averageRpe: null },
        exercises: [],
      },
      refetch: vi.fn(),
    });
    renderPage();
    expect(screen.getByText('No performance data yet')).toBeInTheDocument();
  });

  it('links back to the underlying training occurrence detail page', () => {
    useOccurrencePerformance.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        occurrenceId: 'occ-1',
        scheduledDate: '2026-02-01',
        status: 'COMPLETED',
        totals: { completedExerciseCount: 1, completedSetCount: 3, averageRpe: null },
        exercises: [],
      },
      refetch: vi.fn(),
    });
    renderPage();
    expect(screen.getByRole('link', { name: 'View training session' })).toHaveAttribute(
      'href',
      '/app/training/plans/plan-1/days/day-1/occurrences/occ-1',
    );
  });
});
