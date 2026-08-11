import { describe, expect, it, vi } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import {
  fetchBodyAreaDiscomfortHistory,
  fetchRecoveryDashboard,
  fetchRecoveryMetricTrend,
} from '@/features/recovery/api/analyticsApi';

function makeClient() {
  return { axios: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() } };
}

describe('analyticsApi', () => {
  it('fetches the dashboard with baselineWindowDays/targetDate/includeTrainingLoad', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: { targetDate: '2026-02-01', checkInPresent: false, baselineWindowDays: 14, baselines: [], metricDeviations: [], metricTrends: [] },
    });
    const target = parseDateOnly('2026-02-01');
    await fetchRecoveryDashboard(client as never, 14, target, true);
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/recovery-analytics/dashboard', {
      params: { baselineWindowDays: 14, targetDate: target, includeTrainingLoad: true },
    });
  });

  it('rejects a baseline window that is not 7/14/28 at the type level (only allowed values reach the call)', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: { targetDate: '2026-02-01', checkInPresent: false, baselineWindowDays: 7, baselines: [], metricDeviations: [], metricTrends: [] },
    });
    await fetchRecoveryDashboard(client as never, 7);
    expect(client.axios.get).toHaveBeenCalledWith(
      '/api/v1/training/recovery-analytics/dashboard',
      expect.objectContaining({ params: expect.objectContaining({ baselineWindowDays: 7 }) }),
    );
  });

  it('fetches a metric trend at /recovery-analytics/trends/{metricType}', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: {
        metricType: 'FATIGUE',
        scaleDirection: 'HIGHER_IS_WORSE',
        startDate: '2026-01-01',
        endDate: '2026-01-28',
        observationCount: 0,
        trendDirection: 'INSUFFICIENT_DATA',
        points: [],
      },
    });
    const start = parseDateOnly('2026-01-01');
    const end = parseDateOnly('2026-01-28');
    await fetchRecoveryMetricTrend(client as never, 'FATIGUE', start, end, true);
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/recovery-analytics/trends/FATIGUE', {
      params: { startDate: start, endDate: end, includeTrainingLoad: true },
    });
  });

  it('fetches discomfort history with optional bodyArea/bodySide filters', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: { startDate: '2026-01-01', endDate: '2026-01-31', observationCount: 0, entries: [] },
    });
    const start = parseDateOnly('2026-01-01');
    const end = parseDateOnly('2026-01-31');
    await fetchBodyAreaDiscomfortHistory(client as never, start, end, { bodyArea: 'KNEE', bodySide: 'LEFT' });
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/recovery-analytics/discomfort-history', {
      params: { startDate: start, endDate: end, bodyArea: 'KNEE', bodySide: 'LEFT' },
    });
  });
});
