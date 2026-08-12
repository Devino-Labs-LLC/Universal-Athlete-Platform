import { describe, expect, it, vi } from 'vitest';

import { PerformanceLandingPage } from '@/features/performance/pages/PerformanceLandingPage';
import { renderWithProviders, screen } from '@/test/utils';

const useRecentPersonalRecords = vi.fn();

vi.mock('@/features/performance/hooks/usePersonalRecords', () => ({
  useRecentPersonalRecords: (...args: unknown[]) => useRecentPersonalRecords(...args),
}));

describe('PerformanceLandingPage', () => {
  it('requests the last 30 days of recent records', () => {
    useRecentPersonalRecords.mockReturnValue({ isLoading: false, isError: false, data: [], refetch: vi.fn() });
    renderWithProviders(<PerformanceLandingPage />);
    expect(useRecentPersonalRecords).toHaveBeenCalledWith(30, 10);
  });

  it('shows a factual empty state with no recent records', () => {
    useRecentPersonalRecords.mockReturnValue({ isLoading: false, isError: false, data: [], refetch: vi.fn() });
    renderWithProviders(<PerformanceLandingPage />);
    expect(screen.getByText('No recent personal records')).toBeInTheDocument();
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
});
