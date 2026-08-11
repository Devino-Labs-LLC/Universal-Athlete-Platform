import { describe, expect, it, vi } from 'vitest';

import { RecoveryHistoryPage } from '@/features/recovery/pages/RecoveryHistoryPage';
import { renderWithProviders as render, screen } from '@/test/utils';

const useRecoveryHistory = vi.fn();

vi.mock('@/features/recovery/hooks/useRecoveryCheckIns', () => ({
  useRecoveryHistory: (...args: unknown[]) => useRecoveryHistory(...args),
}));

describe('RecoveryHistoryPage', () => {
  it('defaults to a 30-day range when no range query param is present', () => {
    useRecoveryHistory.mockReturnValue({ isLoading: false, isError: false, data: { days: [] }, refetch: vi.fn() });
    render(<RecoveryHistoryPage />, { initialEntries: ['/app/recovery/history'] });
    expect(screen.getByRole('button', { name: '30d', pressed: true })).toBeInTheDocument();
  });

  it('reads the range from the URL query param', () => {
    useRecoveryHistory.mockReturnValue({ isLoading: false, isError: false, data: { days: [] }, refetch: vi.fn() });
    render(<RecoveryHistoryPage />, { initialEntries: ['/app/recovery/history?range=90'] });
    expect(screen.getByRole('button', { name: '90d', pressed: true })).toBeInTheDocument();
    expect(useRecoveryHistory).toHaveBeenCalledWith(expect.any(String), expect.any(String), true);
  });

  it('falls back to 30 days for an invalid range param', () => {
    useRecoveryHistory.mockReturnValue({ isLoading: false, isError: false, data: { days: [] }, refetch: vi.fn() });
    render(<RecoveryHistoryPage />, { initialEntries: ['/app/recovery/history?range=999'] });
    expect(screen.getByRole('button', { name: '30d', pressed: true })).toBeInTheDocument();
  });

  it('renders the check-in history table when data is loaded', () => {
    useRecoveryHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      data: { days: [{ date: '2026-01-05', checkIn: null }] },
      refetch: vi.fn(),
    });
    render(<RecoveryHistoryPage />, { initialEntries: ['/app/recovery/history'] });
    expect(screen.getByText('No recovery check-ins recorded in this date range.')).toBeInTheDocument();
  });
});
