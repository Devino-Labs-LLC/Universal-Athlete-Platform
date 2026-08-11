import { describe, expect, it, vi } from 'vitest';

import { fetchExercisePerformanceHistory } from '@/features/performance/api/exercisePerformanceApi';

function makeClient() {
  return { axios: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() } };
}

describe('fetchExercisePerformanceHistory', () => {
  it('calls /performance/exercises/{exercisePerformanceKey} with page/size params', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: {
        exercisePerformanceKey: 'key-1',
        exerciseDefinitionId: 'def-1',
        exerciseName: 'Back Squat',
        entries: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      },
    });
    await fetchExercisePerformanceHistory(client as never, 'key-1', { page: 0, size: 20 });
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/performance/exercises/key-1', {
      params: { page: 0, size: 20 },
    });
  });

  it('passes scheduledFrom/scheduledTo filters through unchanged', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: {
        exercisePerformanceKey: 'key-1',
        exerciseDefinitionId: 'def-1',
        exerciseName: 'Back Squat',
        entries: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      },
    });
    await fetchExercisePerformanceHistory(client as never, 'key-1', {
      scheduledFrom: '2026-01-01' as never,
      scheduledTo: '2026-01-31' as never,
    });
    expect(client.axios.get).toHaveBeenCalledWith(
      '/api/v1/training/performance/exercises/key-1',
      expect.objectContaining({ params: expect.objectContaining({ scheduledFrom: '2026-01-01', scheduledTo: '2026-01-31' }) }),
    );
  });

  it('parses the exercise name and total element count from the response', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: {
        exercisePerformanceKey: 'key-1',
        exerciseDefinitionId: 'def-1',
        exerciseName: 'Back Squat',
        entries: [],
        page: 0,
        size: 20,
        totalElements: 12,
        totalPages: 1,
      },
    });
    const history = await fetchExercisePerformanceHistory(client as never, 'key-1');
    expect(history.exerciseName).toBe('Back Squat');
    expect(history.totalElements).toBe(12);
  });
});
