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
});
