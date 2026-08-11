import { Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { AthleteStateComparePage } from '@/features/recovery/pages/AthleteStateComparePage';
import { renderWithProviders, screen } from '@/test/utils';

const useAthleteStateComparison = vi.fn();

vi.mock('@/features/recovery/hooks/useAthleteState', () => ({
  useAthleteStateComparison: (...args: unknown[]) => useAthleteStateComparison(...args),
}));

function renderPage(path: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/app/recovery/state/:snapshotId/compare" element={<AthleteStateComparePage />} />
    </Routes>,
    { initialEntries: [path] },
  );
}

describe('AthleteStateComparePage', () => {
  it('prompts for a comparison snapshot when ?other= is missing', () => {
    useAthleteStateComparison.mockReturnValue({ isLoading: false, isError: false, data: undefined, refetch: vi.fn() });
    renderPage('/app/recovery/state/snap-1/compare');
    expect(screen.getByText('No comparison snapshot was specified.')).toBeInTheDocument();
  });

  it('renders field-level differences when comparison data resolves', () => {
    useAthleteStateComparison.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        olderSnapshotId: 'snap-0',
        newerSnapshotId: 'snap-1',
        olderStateDate: '2026-02-01',
        newerStateDate: '2026-02-01',
        olderVersion: 1,
        newerVersion: 2,
        recoveryChanged: true,
        baselineChanged: false,
        trainingLoadChanged: false,
        scheduleChanged: false,
        discomfortChanged: false,
        fieldDifferences: [{ field: 'recovery.fatigue', previousValue: '2', newValue: '4' }],
      },
      refetch: vi.fn(),
    });
    renderPage('/app/recovery/state/snap-1/compare?other=snap-0');
    expect(screen.getByText('recovery.fatigue')).toBeInTheDocument();
    expect(screen.getAllByText('Yes')[0]).toBeInTheDocument();
  });

  it('reports no field-level differences factually when none exist', () => {
    useAthleteStateComparison.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        olderSnapshotId: 'snap-0',
        newerSnapshotId: 'snap-1',
        olderStateDate: '2026-02-01',
        newerStateDate: '2026-02-01',
        olderVersion: 1,
        newerVersion: 2,
        recoveryChanged: false,
        baselineChanged: false,
        trainingLoadChanged: false,
        scheduleChanged: false,
        discomfortChanged: false,
        fieldDifferences: [],
      },
      refetch: vi.fn(),
    });
    renderPage('/app/recovery/state/snap-1/compare?other=snap-0');
    expect(screen.getByText('No field-level differences were reported.')).toBeInTheDocument();
  });
});
