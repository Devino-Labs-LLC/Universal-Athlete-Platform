import { Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { GuidanceDetailPage } from '@/features/recovery/pages/GuidanceDetailPage';
import { renderWithProviders, screen } from '@/test/utils';

const useRecommendation = vi.fn();

vi.mock('@/features/recovery/hooks/useRecommendations', () => ({
  useRecommendation: (...args: unknown[]) => useRecommendation(...args),
}));

function renderPage(path: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/app/recovery/guidance/:recommendationId" element={<GuidanceDetailPage />} />
    </Routes>,
    { initialEntries: [path] },
  );
}

describe('GuidanceDetailPage', () => {
  it('renders modest, non-mandating recommendation copy', () => {
    useRecommendation.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        recommendationId: 'rec-1',
        stateDate: '2026-02-01',
        overallAction: 'PROCEED_WITH_MODIFICATIONS',
        recommendationStatus: 'ACTIVE',
        adjustments: [],
      },
      refetch: vi.fn(),
    });
    renderPage('/app/recovery/guidance/rec-1');
    expect(screen.getByText("Consider modifying today\u2019s session")).toBeInTheDocument();
    expect(screen.queryByText(/must|required|mandate/i)).not.toBeInTheDocument();
  });

  it('renders the suggested adjustments list', () => {
    useRecommendation.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        recommendationId: 'rec-2',
        stateDate: '2026-02-02',
        overallAction: 'PROCEED_AS_PLANNED',
        recommendationStatus: 'ACTIVE',
        adjustments: [{ adjustmentId: 'a1', type: 'REDUCE_VOLUME', priority: 1, orderIndex: 0 }],
      },
      refetch: vi.fn(),
    });
    renderPage('/app/recovery/guidance/rec-2');
    expect(screen.getByText('Reduce Volume')).toBeInTheDocument();
  });

  it('shows a mapped error message when the recommendation fails to load', () => {
    useRecommendation.mockReturnValue({
      isLoading: false,
      isError: true,
      error: new Error('nope'),
      data: undefined,
      refetch: vi.fn(),
    });
    renderPage('/app/recovery/guidance/rec-3');
    expect(screen.getByText('nope')).toBeInTheDocument();
  });
});
