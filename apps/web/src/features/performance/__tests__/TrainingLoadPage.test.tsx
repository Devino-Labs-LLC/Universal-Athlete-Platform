import { describe, expect, it, vi } from 'vitest';

import { TrainingLoadPage } from '@/features/performance/pages/TrainingLoadPage';
import { renderWithProviders, screen } from '@/test/utils';

const useTrainingLoadHistory = vi.fn();

vi.mock('@/features/performance/hooks/useTrainingLoadHistory', () => ({
  useTrainingLoadHistory: (...args: unknown[]) => useTrainingLoadHistory(...args),
}));

function emptyHistory(granularity: 'OCCURRENCE' | 'DAILY' | 'WEEKLY') {
  return { isLoading: false, isError: false, data: { granularity, page: 0, size: 0, totalElements: 0, totalPages: 0 }, refetch: vi.fn() };
}

describe('TrainingLoadPage', () => {
  it('defaults to WEEKLY mode and a 28-day range when no filters are set', () => {
    useTrainingLoadHistory.mockReturnValue(emptyHistory('WEEKLY'));
    renderWithProviders(<TrainingLoadPage />, { initialEntries: ['/app/performance/load'] });
    expect(useTrainingLoadHistory).toHaveBeenCalledWith('WEEKLY', expect.any(String), expect.any(String), { size: 200 });
    expect(screen.getByRole('combobox', { name: 'Granularity' })).toHaveValue('WEEKLY');
    expect(screen.getByRole('combobox', { name: 'Range' })).toHaveValue('28');
  });

  it('reads mode and range from the URL', () => {
    useTrainingLoadHistory.mockReturnValue(emptyHistory('DAILY'));
    renderWithProviders(<TrainingLoadPage />, { initialEntries: ['/app/performance/load?mode=DAILY&range=90'] });
    expect(useTrainingLoadHistory).toHaveBeenCalledWith('DAILY', expect.any(String), expect.any(String), { size: 200 });
    expect(screen.getByRole('combobox', { name: 'Range' })).toHaveValue('90');
  });

  it('falls back to defaults for invalid mode/range params', () => {
    useTrainingLoadHistory.mockReturnValue(emptyHistory('WEEKLY'));
    renderWithProviders(<TrainingLoadPage />, { initialEntries: ['/app/performance/load?mode=MONTHLY&range=15'] });
    expect(useTrainingLoadHistory).toHaveBeenCalledWith('WEEKLY', expect.any(String), expect.any(String), { size: 200 });
  });

  it('renders the empty-state copy for both distribution tables when there is no load data', () => {
    useTrainingLoadHistory.mockReturnValue(emptyHistory('WEEKLY'));
    renderWithProviders(<TrainingLoadPage />, { initialEntries: ['/app/performance/load'] });
    expect(screen.getByText('No category breakdown available.')).toBeInTheDocument();
    expect(screen.getByText('No movement pattern breakdown available.')).toBeInTheDocument();
  });
});
