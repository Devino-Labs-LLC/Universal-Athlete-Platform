import { Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders, screen } from '@/test/utils';
import { OccurrenceDetailPage } from '@/features/training/pages/OccurrenceDetailPage';

const useOccurrenceDetail = vi.fn();
const usePlan = vi.fn();
const useOccurrencePerformance = vi.fn();

vi.mock('@/features/training/hooks/useOccurrences', () => ({
  useOccurrenceDetail: (...args: unknown[]) => useOccurrenceDetail(...args),
  useOccurrenceMutations: () => ({
    reschedule: { mutateAsync: vi.fn() },
    remove: { mutateAsync: vi.fn() },
  }),
}));

vi.mock('@/features/training/hooks/usePlans', () => ({
  usePlan: (...args: unknown[]) => usePlan(...args),
}));

vi.mock('@/features/performance/hooks/useOccurrencePerformance', () => ({
  useOccurrencePerformance: (...args: unknown[]) => useOccurrencePerformance(...args),
}));

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route
        path="/app/training/plans/:planId/days/:dayId/occurrences/:occurrenceId"
        element={<OccurrenceDetailPage />}
      />
    </Routes>,
    { initialEntries: ['/app/training/plans/plan-1/days/day-1/occurrences/occ-1'] },
  );
}

const scheduledOccurrence = {
  id: 'occ-1',
  status: 'SCHEDULED',
  scheduledDate: '2026-02-01',
  plannedStartTime: null,
  origin: 'PLANNED',
  executions: [],
};

const completedOccurrence = { ...scheduledOccurrence, status: 'COMPLETED' };

describe('OccurrenceDetailPage — performance integration', () => {
  it('does not render a Performance section for a scheduled (not yet completed) occurrence', () => {
    usePlan.mockReturnValue({ isLoading: false, data: { name: 'Base Plan' } });
    useOccurrenceDetail.mockReturnValue({ isLoading: false, isError: false, data: scheduledOccurrence, refetch: vi.fn() });
    renderPage();
    expect(screen.queryByRole('heading', { name: 'Performance' })).not.toBeInTheDocument();
    expect(useOccurrencePerformance).not.toHaveBeenCalledWith('plan-1', 'day-1', 'occ-1');
  });

  it('renders an embedded performance summary with a link to the full session page for a completed occurrence', () => {
    usePlan.mockReturnValue({ isLoading: false, data: { name: 'Base Plan' } });
    useOccurrenceDetail.mockReturnValue({ isLoading: false, isError: false, data: completedOccurrence, refetch: vi.fn() });
    useOccurrencePerformance.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        occurrenceId: 'occ-1',
        scheduledDate: '2026-02-01',
        status: 'COMPLETED',
        totals: { completedExerciseCount: 3, completedSetCount: 9, averageRpe: null },
        exercises: [],
      },
    });
    renderPage();
    expect(screen.getByRole('heading', { name: 'Performance' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'View performance details' })).toHaveAttribute(
      'href',
      '/app/performance/sessions/plan-1/day-1/occ-1',
    );
  });

  it('shows a factual message rather than an error when performance is not yet available', () => {
    usePlan.mockReturnValue({ isLoading: false, data: { name: 'Base Plan' } });
    useOccurrenceDetail.mockReturnValue({ isLoading: false, isError: false, data: completedOccurrence, refetch: vi.fn() });
    useOccurrencePerformance.mockReturnValue({ isLoading: false, isError: true, data: undefined });
    renderPage();
    expect(screen.getByText('Performance summary is not available for this session yet.')).toBeInTheDocument();
  });
});
