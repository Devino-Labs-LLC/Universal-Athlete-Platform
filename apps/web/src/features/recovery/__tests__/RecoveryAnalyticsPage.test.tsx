import { describe, expect, it, vi } from 'vitest';

import { RecoveryAnalyticsPage } from '@/features/recovery/pages/RecoveryAnalyticsPage';
import { renderWithProviders as render, screen } from '@/test/utils';

const useRecoveryDashboard = vi.fn();
const useRecoveryMetricTrend = vi.fn();
const useBodyAreaDiscomfortHistory = vi.fn();

vi.mock('@/features/recovery/hooks/useRecoveryAnalytics', () => ({
  useRecoveryDashboard: (...args: unknown[]) => useRecoveryDashboard(...args),
  useRecoveryMetricTrend: (...args: unknown[]) => useRecoveryMetricTrend(...args),
  useBodyAreaDiscomfortHistory: (...args: unknown[]) => useBodyAreaDiscomfortHistory(...args),
}));

function mockAllLoading() {
  useRecoveryDashboard.mockReturnValue({ isLoading: true, isError: false, data: undefined, refetch: vi.fn() });
  useRecoveryMetricTrend.mockReturnValue({ isLoading: true, isError: false, data: undefined, refetch: vi.fn() });
  useBodyAreaDiscomfortHistory.mockReturnValue({ isLoading: true, isError: false, data: undefined, refetch: vi.fn() });
}

describe('RecoveryAnalyticsPage', () => {
  it('defaults to a 14-day baseline window and FATIGUE metric when no filters are set', () => {
    mockAllLoading();
    render(<RecoveryAnalyticsPage />, { initialEntries: ['/app/recovery/analytics'] });
    expect(useRecoveryDashboard).toHaveBeenCalledWith(14, expect.any(String), true);
    expect(useRecoveryMetricTrend).toHaveBeenCalledWith('FATIGUE', expect.any(String), expect.any(String), true);
    expect(screen.getByRole('combobox', { name: 'Baseline window' })).toHaveValue('14');
  });

  it('reads window and metric from the URL', () => {
    mockAllLoading();
    render(<RecoveryAnalyticsPage />, { initialEntries: ['/app/recovery/analytics?window=28&metric=MOOD'] });
    expect(useRecoveryDashboard).toHaveBeenCalledWith(28, expect.any(String), true);
    expect(useRecoveryMetricTrend).toHaveBeenCalledWith('MOOD', expect.any(String), expect.any(String), true);
    expect(screen.getByRole('combobox', { name: 'Trend metric' })).toHaveValue('MOOD');
  });

  it('falls back to defaults for an invalid window/metric in the URL', () => {
    mockAllLoading();
    render(<RecoveryAnalyticsPage />, { initialEntries: ['/app/recovery/analytics?window=999&metric=NOT_REAL'] });
    expect(useRecoveryDashboard).toHaveBeenCalledWith(14, expect.any(String), true);
    expect(useRecoveryMetricTrend).toHaveBeenCalledWith('FATIGUE', expect.any(String), expect.any(String), true);
  });

  it('renders baselines, trend, and discomfort sections once data resolves', () => {
    useRecoveryDashboard.mockReturnValue({
      isLoading: false,
      isError: false,
      data: { targetDate: '2026-02-01', checkInPresent: false, baselineWindowDays: 14, baselines: [], metricDeviations: [], metricTrends: [] },
      refetch: vi.fn(),
    });
    useRecoveryMetricTrend.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        metricType: 'FATIGUE',
        scaleDirection: 'HIGHER_IS_WORSE',
        startDate: '2026-01-01',
        endDate: '2026-01-28',
        observationCount: 0,
        trendDirection: 'INSUFFICIENT_DATA',
        points: [],
      },
      refetch: vi.fn(),
    });
    useBodyAreaDiscomfortHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      data: { startDate: '2026-01-01', endDate: '2026-01-31', observationCount: 0, entries: [] },
      refetch: vi.fn(),
    });
    render(<RecoveryAnalyticsPage />, { initialEntries: ['/app/recovery/analytics'] });
    expect(screen.getByText('No baseline data available for this window yet.')).toBeInTheDocument();
    expect(screen.getByText('No observations recorded in this date range.')).toBeInTheDocument();
    expect(screen.getByText('No discomfort reported in this date range.')).toBeInTheDocument();
  });
});
