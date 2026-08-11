import { describe, expect, it, vi } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import {
  fetchRecommendation,
  fetchRecommendationComparison,
  fetchRecommendationHistory,
} from '@/features/recovery/api/recommendationsApi';

function makeClient() {
  return { axios: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() } };
}

const recommendation = {
  recommendationId: 'rec-1',
  stateDate: '2026-02-01',
  overallAction: 'PROCEED_AS_PLANNED',
  recommendationStatus: 'ACTIVE',
  adjustments: [],
};

describe('recommendationsApi', () => {
  it('fetches a recommendation from /recommendations/{id}', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: recommendation });
    await fetchRecommendation(client as never, 'rec-1');
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/recommendations/rec-1');
  });

  it('fetches history including optional overallAction filter', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: { content: [], page: 0, size: 20, totalElements: 0 } });
    const start = parseDateOnly('2026-01-01');
    const end = parseDateOnly('2026-01-31');
    await fetchRecommendationHistory(client as never, start, end, { overallAction: 'CONSIDER_REST' });
    expect(client.axios.get).toHaveBeenCalledWith(
      '/api/v1/training/recommendations/history',
      expect.objectContaining({ params: expect.objectContaining({ overallAction: 'CONSIDER_REST' }) }),
    );
  });

  it('fetches a comparison from /recommendations/compare', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: {
        olderRecommendationId: 'a',
        newerRecommendationId: 'b',
        olderStateDate: '2026-01-01',
        newerStateDate: '2026-01-02',
        actionChanged: false,
        priorAction: 'PROCEED_AS_PLANNED',
        newAction: 'PROCEED_AS_PLANNED',
        adjustmentsAdded: [],
        adjustmentsRemoved: [],
        limitingDimensionsChanged: false,
        olderLimitingDimensions: [],
        newerLimitingDimensions: [],
      },
    });
    await fetchRecommendationComparison(client as never, 'a', 'b');
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/recommendations/compare', {
      params: { olderRecommendationId: 'a', newerRecommendationId: 'b' },
    });
  });
});
