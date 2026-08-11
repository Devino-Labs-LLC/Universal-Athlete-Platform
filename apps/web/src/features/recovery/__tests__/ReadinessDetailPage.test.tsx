import { describe, expect, it, vi } from 'vitest';

import { Route, Routes } from 'react-router-dom';

import { ReadinessDetailPage } from '@/features/recovery/pages/ReadinessDetailPage';
import { renderWithProviders, screen } from '@/test/utils';

const useReadinessAssessment = vi.fn();
const useReadinessComparison = vi.fn();

vi.mock('@/features/recovery/hooks/useReadiness', () => ({
  useReadinessAssessment: (...args: unknown[]) => useReadinessAssessment(...args),
  useReadinessComparison: (...args: unknown[]) => useReadinessComparison(...args),
}));

function renderPage(path: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/app/recovery/readiness/:assessmentId" element={<ReadinessDetailPage />} />
    </Routes>,
    { initialEntries: [path] },
  );
}

describe('ReadinessDetailPage', () => {
  it('shows the loading view while the assessment loads', () => {
    useReadinessAssessment.mockReturnValue({ isLoading: true, isError: false, data: undefined, refetch: vi.fn() });
    useReadinessComparison.mockReturnValue({ isLoading: false, isError: false, data: undefined, refetch: vi.fn() });
    renderPage('/app/recovery/readiness/ra-1');
    expect(screen.getByText('Loading readiness assessment…')).toBeInTheDocument();
  });

  it('renders the readiness band, sufficiency, and limiting dimensions', () => {
    useReadinessAssessment.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        assessmentId: 'ra-1',
        stateDate: '2026-02-01',
        readinessScore: 78.4,
        readinessBand: 'HIGH',
        dataSufficiency: 'SUFFICIENT',
        limitingDimensions: ['FATIGUE'],
        strongestDimensions: ['SLEEP'],
        contributions: [],
      },
      refetch: vi.fn(),
    });
    useReadinessComparison.mockReturnValue({ isLoading: false, isError: false, data: undefined, refetch: vi.fn() });
    renderPage('/app/recovery/readiness/ra-1');
    expect(screen.getByText('High')).toBeInTheDocument();
    expect(screen.getByText('Baseline established')).toBeInTheDocument();
    expect(screen.getByText(/Limiting dimensions: Fatigue/)).toBeInTheDocument();
  });

  it('renders a comparison section only when a ?compare= query param is present', () => {
    useReadinessAssessment.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        assessmentId: 'ra-2',
        stateDate: '2026-02-02',
        readinessScore: 60,
        readinessBand: 'MODERATE',
        dataSufficiency: 'LIMITED',
        limitingDimensions: [],
        strongestDimensions: [],
        contributions: [],
      },
      refetch: vi.fn(),
    });
    useReadinessComparison.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        scoreDelta: -5,
        bandChanged: true,
        limitingDimensionsChanged: false,
      },
      refetch: vi.fn(),
    });
    renderPage('/app/recovery/readiness/ra-2?compare=ra-1');
    expect(screen.getByText('Comparison')).toBeInTheDocument();
    expect(screen.getByText('-5.0')).toBeInTheDocument();
  });
});
