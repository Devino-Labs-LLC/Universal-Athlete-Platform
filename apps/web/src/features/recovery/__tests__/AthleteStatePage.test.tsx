import { Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { AthleteStatePage } from '@/features/recovery/pages/AthleteStatePage';
import { renderWithProviders, screen } from '@/test/utils';

const useAthleteStateSnapshot = vi.fn();
const useAthleteStateVersions = vi.fn();

vi.mock('@/features/recovery/hooks/useAthleteState', () => ({
  useAthleteStateSnapshot: (...args: unknown[]) => useAthleteStateSnapshot(...args),
  useAthleteStateVersions: (...args: unknown[]) => useAthleteStateVersions(...args),
}));

function renderPage(path: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/app/recovery/state/:snapshotId" element={<AthleteStatePage />} />
    </Routes>,
    { initialEntries: [path] },
  );
}

const baseSnapshot = {
  snapshotId: 'snap-1',
  stateDate: '2026-02-01',
  snapshotVersion: 2,
  current: true,
  trainingLoad: {
    totalVolumeKilograms: 4200,
    totalDurationSeconds: 3600,
    totalDistanceMeters: null,
    completedExerciseCount: 5,
  },
  schedule: {
    scheduledOccurrenceCount: 1,
    completedScheduledCount: 1,
    skippedScheduledCount: 0,
    cancelledScheduledCount: 0,
  },
};

describe('AthleteStatePage', () => {
  it('renders training load metrics and treats a null distance as unavailable, not zero', () => {
    useAthleteStateSnapshot.mockReturnValue({ isLoading: false, isError: false, data: baseSnapshot, refetch: vi.fn() });
    useAthleteStateVersions.mockReturnValue({ data: [] });
    renderPage('/app/recovery/state/snap-1');
    expect(screen.getByText('4.2 t')).toBeInTheDocument();
    expect(screen.getByText('1h')).toBeInTheDocument();
    // Distance is null -> em dash, not "0 m"
    expect(screen.getByText('—')).toBeInTheDocument();
  });

  it('links to compare against other versions on the same date', () => {
    useAthleteStateSnapshot.mockReturnValue({ isLoading: false, isError: false, data: baseSnapshot, refetch: vi.fn() });
    useAthleteStateVersions.mockReturnValue({
      data: [
        { snapshotId: 'snap-1', stateDate: '2026-02-01', snapshotVersion: 2, current: true },
        { snapshotId: 'snap-0', stateDate: '2026-02-01', snapshotVersion: 1, current: false, generationReason: 'INITIAL' },
      ],
    });
    renderPage('/app/recovery/state/snap-1');
    const compareLink = screen.getByRole('link', { name: 'Compare' });
    expect(compareLink).toHaveAttribute('href', '/app/recovery/state/snap-1/compare?other=snap-0');
  });

  it('shows a mapped error message when the snapshot fails to load', () => {
    useAthleteStateSnapshot.mockReturnValue({
      isLoading: false,
      isError: true,
      error: new Error('snapshot missing'),
      data: undefined,
      refetch: vi.fn(),
    });
    useAthleteStateVersions.mockReturnValue({ data: [] });
    renderPage('/app/recovery/state/snap-missing');
    expect(screen.getByText('snapshot missing')).toBeInTheDocument();
  });
});
