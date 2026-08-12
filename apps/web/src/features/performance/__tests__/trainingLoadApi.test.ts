import { describe, expect, it, vi } from 'vitest';

import { fetchTrainingLoadHistory, TRAINING_LOAD_MAX_PAGE_SIZE } from '@/features/performance/api/trainingLoadApi';

function makeClient() {
  return { axios: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() } };
}

describe('fetchTrainingLoadHistory', () => {
  it('calls /training-load/history with startDate/endDate/granularity and optional filters', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: { granularity: 'WEEKLY', weeklySummaries: [], page: 0, size: 50, totalElements: 0, totalPages: 0 },
    });
    await fetchTrainingLoadHistory(client as never, {
      startDate: '2026-01-01' as never,
      endDate: '2026-01-28' as never,
      granularity: 'WEEKLY',
      category: 'STRENGTH',
    });
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/training-load/history', {
      params: {
        startDate: '2026-01-01',
        endDate: '2026-01-28',
        granularity: 'WEEKLY',
        category: 'STRENGTH',
      },
    });
  });

  it('documents the backend page-size ceiling the UI must honor', () => {
    expect(TRAINING_LOAD_MAX_PAGE_SIZE).toBe(100);
  });

  it('parses the granularity discriminant from the response', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: { granularity: 'OCCURRENCE', occurrences: [], page: 0, size: 50, totalElements: 0, totalPages: 0 },
    });
    const history = await fetchTrainingLoadHistory(client as never, {
      startDate: '2026-01-01' as never,
      endDate: '2026-01-28' as never,
      granularity: 'OCCURRENCE',
    });
    expect(history.granularity).toBe('OCCURRENCE');
  });
});
