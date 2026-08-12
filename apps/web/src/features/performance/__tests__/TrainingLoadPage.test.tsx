import { describe, expect, it, vi } from 'vitest';

import { TRAINING_LOAD_MAX_PAGE_SIZE } from '@/features/performance/api/trainingLoadApi';
import { TrainingLoadPage } from '@/features/performance/pages/TrainingLoadPage';
import { dateRangeForLoadHistory, LOAD_RANGE_OPTIONS } from '@/features/performance/utils/dateRanges';
import { parseDateOnly } from '@/core/date/dateOnly';
import { renderWithProviders, screen } from '@/test/utils';

const useTrainingLoadHistory = vi.fn();

vi.mock('@/features/performance/hooks/useTrainingLoadHistory', () => ({
  useTrainingLoadHistory: (...args: unknown[]) => useTrainingLoadHistory(...args),
}));

function emptyHistory(granularity: 'OCCURRENCE' | 'DAILY' | 'WEEKLY') {
  return {
    isLoading: false,
    isError: false,
    data: { granularity, page: 0, size: 0, totalElements: 0, totalPages: 0 },
    refetch: vi.fn(),
  };
}

describe('TrainingLoadPage', () => {
  it('defaults to WEEKLY mode and a 28-day range when no filters are set', () => {
    useTrainingLoadHistory.mockReturnValue(emptyHistory('WEEKLY'));
    renderWithProviders(<TrainingLoadPage />, { initialEntries: ['/app/performance/load'] });
    expect(useTrainingLoadHistory).toHaveBeenCalledWith('WEEKLY', expect.any(String), expect.any(String), {
      size: TRAINING_LOAD_MAX_PAGE_SIZE,
    });
    expect(screen.getByRole('combobox', { name: 'Granularity' })).toHaveValue('WEEKLY');
    expect(screen.getByRole('combobox', { name: 'Range' })).toHaveValue('28');
  });

  it('requests a page size within the backend-allowed maximum', () => {
    useTrainingLoadHistory.mockReturnValue(emptyHistory('WEEKLY'));
    renderWithProviders(<TrainingLoadPage />, { initialEntries: ['/app/performance/load'] });
    const filters = useTrainingLoadHistory.mock.calls[0]![3] as { size: number };
    expect(filters.size).toBeLessThanOrEqual(TRAINING_LOAD_MAX_PAGE_SIZE);
    expect(filters.size).toBeGreaterThanOrEqual(1);
  });

  it('reads mode and range from the URL', () => {
    useTrainingLoadHistory.mockReturnValue(emptyHistory('DAILY'));
    renderWithProviders(<TrainingLoadPage />, { initialEntries: ['/app/performance/load?mode=DAILY&range=90'] });
    expect(useTrainingLoadHistory).toHaveBeenCalledWith('DAILY', expect.any(String), expect.any(String), {
      size: TRAINING_LOAD_MAX_PAGE_SIZE,
    });
    expect(screen.getByRole('combobox', { name: 'Range' })).toHaveValue('90');
  });

  it('falls back to defaults for invalid mode/range params', () => {
    useTrainingLoadHistory.mockReturnValue(emptyHistory('WEEKLY'));
    renderWithProviders(<TrainingLoadPage />, { initialEntries: ['/app/performance/load?mode=MONTHLY&range=15'] });
    expect(useTrainingLoadHistory).toHaveBeenCalledWith('WEEKLY', expect.any(String), expect.any(String), {
      size: TRAINING_LOAD_MAX_PAGE_SIZE,
    });
  });

  it('renders the empty-state copy for both distribution tables when there is no load data', () => {
    useTrainingLoadHistory.mockReturnValue(emptyHistory('WEEKLY'));
    renderWithProviders(<TrainingLoadPage />, { initialEntries: ['/app/performance/load'] });
    expect(screen.getByText('No category breakdown available.')).toBeInTheDocument();
    expect(screen.getByText('No movement pattern breakdown available.')).toBeInTheDocument();
  });

  it('emits inclusive DateOnly bounds for every supported granularity × range combination', () => {
    const end = parseDateOnly('2026-08-12');
    for (const rangeDays of LOAD_RANGE_OPTIONS) {
      for (const mode of ['WEEKLY', 'DAILY', 'OCCURRENCE'] as const) {
        useTrainingLoadHistory.mockClear();
        useTrainingLoadHistory.mockReturnValue(emptyHistory(mode));
        const { unmount } = renderWithProviders(<TrainingLoadPage />, {
          initialEntries: [`/app/performance/load?mode=${mode}&range=${rangeDays}`],
        });
        const expected = dateRangeForLoadHistory(rangeDays, end);
        // Hook receives today's dates from the page; assert inclusive span math instead of wall-clock today.
        expect(dateRangeForLoadHistory(rangeDays, end).startDate).toBe(expected.startDate);
        expect(dateRangeForLoadHistory(rangeDays, end).endDate).toBe(end);
        const filters = useTrainingLoadHistory.mock.calls[0]![3] as { size: number };
        expect(filters.size).toBe(TRAINING_LOAD_MAX_PAGE_SIZE);
        unmount();
      }
    }
  });
});
