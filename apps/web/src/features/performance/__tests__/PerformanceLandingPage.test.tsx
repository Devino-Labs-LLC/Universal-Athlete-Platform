import { describe, expect, it, vi } from 'vitest';

import { PerformanceLandingPage } from '@/features/performance/pages/PerformanceLandingPage';
import { renderWithProviders, screen } from '@/test/utils';

const useRecentPersonalRecords = vi.fn();
const useTrainingOverview = vi.fn();
const useRecoveryHistory = vi.fn();
const useTrainingLoadHistory = vi.fn();

vi.mock('@/features/performance/hooks/usePersonalRecords', () => ({
  useRecentPersonalRecords: (...args: unknown[]) => useRecentPersonalRecords(...args),
}));

vi.mock('@/features/training/hooks/usePlans', () => ({
  useTrainingOverview: (...args: unknown[]) => useTrainingOverview(...args),
}));

vi.mock('@/features/recovery/hooks/useRecoveryCheckIns', () => ({
  useRecoveryHistory: (...args: unknown[]) => useRecoveryHistory(...args),
}));

vi.mock('@/features/performance/hooks/useTrainingLoadHistory', () => ({
  useTrainingLoadHistory: (...args: unknown[]) => useTrainingLoadHistory(...args),
}));

const idleQuery = { isLoading: false, isError: false, data: undefined, refetch: vi.fn() };

describe('PerformanceLandingPage', () => {
  beforeEach(() => {
    useTrainingOverview.mockReturnValue(idleQuery);
    useRecoveryHistory.mockReturnValue(idleQuery);
    useTrainingLoadHistory.mockReturnValue(idleQuery);
  });

  it('requests the last 30 days of recent records', () => {
    useRecentPersonalRecords.mockReturnValue({ isLoading: false, isError: false, data: [], refetch: vi.fn() });
    renderWithProviders(<PerformanceLandingPage />);
    expect(useRecentPersonalRecords).toHaveBeenCalledWith(30, 10);
  });

  it('shows a factual empty state with no recent records', () => {
    useRecentPersonalRecords.mockReturnValue({ isLoading: false, isError: false, data: [], refetch: vi.fn() });
    renderWithProviders(<PerformanceLandingPage />);
    expect(screen.getByText('No recent personal records')).toBeInTheDocument();
    expect(screen.getByText('More training history is needed.')).toBeInTheDocument();
  });

  it('links to the full records page and the training load page', () => {
    useRecentPersonalRecords.mockReturnValue({ isLoading: false, isError: false, data: [], refetch: vi.fn() });
    renderWithProviders(<PerformanceLandingPage />);
    expect(screen.getByRole('link', { name: 'View all records' })).toHaveAttribute('href', '/app/performance/records');
    expect(screen.getByRole('link', { name: 'Training load' })).toHaveAttribute('href', '/app/performance/load');
  });

  it('renders a headline personal record when recent records exist', () => {
    useRecentPersonalRecords.mockReturnValue({
      isLoading: false,
      isError: false,
      data: [
        {
          id: 'pr-1',
          exercisePerformanceKey: 'key-1',
          exerciseDefinitionId: 'def-1',
          recordType: 'HEAVIEST_WEIGHT',
          exerciseName: 'Bench Press',
          measuredValue: 100,
          measuredUnit: 'KILOGRAM',
          normalizedValue: 100,
          normalizedUnit: 'KILOGRAM',
          estimated: false,
          achievedAt: '2026-02-10T12:00:00Z',
          scheduledDate: '2026-02-10',
        },
        {
          id: 'pr-0',
          exercisePerformanceKey: 'key-2',
          exerciseDefinitionId: 'def-2',
          recordType: 'MOST_REPETITIONS',
          exerciseName: 'Pull-Up',
          repetitions: 12,
          estimated: false,
          achievedAt: '2026-02-01T12:00:00Z',
          scheduledDate: '2026-02-01',
        },
      ],
      refetch: vi.fn(),
    });
    renderWithProviders(<PerformanceLandingPage />);
    expect(screen.getByRole('heading', { name: 'Bench Press' })).toBeInTheDocument();
    expect(screen.getAllByText('100 kg').length).toBeGreaterThan(0);
    expect(screen.getByRole('heading', { name: 'Recent performance activity' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'View exercise history' })).toHaveAttribute(
      'href',
      '/app/performance/exercises/key-1',
    );
  });

  it('does not fabricate a zero score when there are no recent records', () => {
    useRecentPersonalRecords.mockReturnValue({ isLoading: false, isError: false, data: [], refetch: vi.fn() });
    renderWithProviders(<PerformanceLandingPage />);
    expect(screen.queryByText('0 kg')).not.toBeInTheDocument();
    expect(screen.getByText(/log completed work in Training/i)).toBeInTheDocument();
  });

  it('shows an insufficient progress headline when some history exists but not enough for a trend', () => {
    useRecentPersonalRecords.mockReturnValue({
      isLoading: false,
      isError: false,
      data: [],
      refetch: vi.fn(),
    });
    useTrainingOverview.mockReturnValue({
      isLoading: false,
      isError: false,
      data: { recentCompletedSessions: [{ occurrenceId: 'o1' }] },
      refetch: vi.fn(),
    });
    useRecoveryHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      data: { days: [{ date: '2026-02-01' }] },
      refetch: vi.fn(),
    });
    useTrainingLoadHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      data: { weeklySummaries: [{ ratedOccurrenceCount: 1 }] },
      refetch: vi.fn(),
    });
    renderWithProviders(<PerformanceLandingPage />);
    expect(screen.getByText('Some history is on file, but not enough to show a trend.')).toBeInTheDocument();
    expect(
      screen.getByText('Load charts stay hidden until at least three weekly summaries exist.'),
    ).toBeInTheDocument();
  });
});
