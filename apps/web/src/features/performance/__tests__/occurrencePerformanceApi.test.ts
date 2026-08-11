import { describe, expect, it, vi } from 'vitest';

import { fetchOccurrencePerformance } from '@/features/performance/api/occurrencePerformanceApi';

function makeClient() {
  return { axios: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() } };
}

describe('fetchOccurrencePerformance', () => {
  it('calls the nested plan/day/occurrence performance path', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: {
        occurrenceId: 'occ-1',
        scheduledDate: '2026-02-01',
        status: 'COMPLETED',
        totals: {
          completedExerciseCount: 3,
          completedSetCount: 9,
          averageRpe: null,
        },
        exercises: [],
      },
    });
    await fetchOccurrencePerformance(client as never, 'plan-1', 'day-1', 'occ-1');
    expect(client.axios.get).toHaveBeenCalledWith(
      '/api/v1/training/plans/plan-1/days/day-1/occurrences/occ-1/performance',
    );
  });

  it('parses a null averageRpe as unrated rather than zero', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: {
        occurrenceId: 'occ-1',
        scheduledDate: '2026-02-01',
        status: 'COMPLETED',
        totals: { completedExerciseCount: 1, completedSetCount: 3, averageRpe: null },
        exercises: [],
      },
    });
    const performance = await fetchOccurrencePerformance(client as never, 'plan-1', 'day-1', 'occ-1');
    expect(performance.totals.averageRpe).toBeNull();
  });
});
