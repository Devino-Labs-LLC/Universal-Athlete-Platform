import { describe, expect, it, vi } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import { fetchRecoveryOverview } from '@/features/recovery/api/overviewApi';

function makeClient() {
  return { axios: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() } };
}

const minimalOverview = {
  date: '2026-02-01',
  trendDays: 7,
  checkInPresent: false,
  baselines: [],
  deviations: [],
  readinessPresent: false,
  recommendationPresent: false,
  trends: [],
  discomfort: [],
};

describe('fetchRecoveryOverview', () => {
  it('calls the exact backend contract path with date and trendDays params', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: minimalOverview });

    await fetchRecoveryOverview(client as never, parseDateOnly('2026-02-01'), 14);

    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/client/recovery-overview', {
      params: { date: '2026-02-01', trendDays: 14 },
    });
  });

  it('defaults trendDays to 7 when omitted', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: minimalOverview });

    await fetchRecoveryOverview(client as never, parseDateOnly('2026-02-01'));

    expect(client.axios.get).toHaveBeenCalledWith(
      '/api/v1/training/client/recovery-overview',
      expect.objectContaining({ params: expect.objectContaining({ trendDays: 7 }) }),
    );
  });

  it('parses the overview envelope', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: minimalOverview });

    const overview = await fetchRecoveryOverview(client as never, parseDateOnly('2026-02-01'));
    expect(overview.trendDays).toBe(7);
    expect(overview.checkInPresent).toBe(false);
  });
});
