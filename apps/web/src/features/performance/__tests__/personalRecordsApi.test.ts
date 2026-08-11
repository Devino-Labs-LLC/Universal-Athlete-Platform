import { describe, expect, it, vi } from 'vitest';

import {
  fetchExercisePersonalRecords,
  fetchPersonalRecords,
  fetchRecentPersonalRecords,
} from '@/features/performance/api/personalRecordsApi';

function makeClient() {
  return { axios: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() } };
}

describe('personalRecordsApi', () => {
  it('fetches recent records with days/limit params and sensible defaults', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: [] });
    await fetchRecentPersonalRecords(client as never);
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/performance/personal-records/recent', {
      params: { days: 30, limit: 5 },
    });
  });

  it('fetches all personal records with optional exercisePerformanceKey/recordType filters', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: [] });
    await fetchPersonalRecords(client as never, { exercisePerformanceKey: 'key-1', recordType: 'HEAVIEST_WEIGHT' });
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/performance/personal-records', {
      params: { exercisePerformanceKey: 'key-1', recordType: 'HEAVIEST_WEIGHT' },
    });
  });

  it('fetches records scoped to a single exercise via exercisePerformanceKey path segment', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: [] });
    await fetchExercisePersonalRecords(client as never, 'key-abc');
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/performance/exercises/key-abc/personal-records');
  });

  it('parses the returned array into personal record objects', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: [
        {
          id: 'pr-1',
          exercisePerformanceKey: 'key-1',
          exerciseDefinitionId: 'def-1',
          recordType: 'HEAVIEST_WEIGHT',
          exerciseName: 'Back Squat',
          measuredValue: 140,
          measuredUnit: 'KILOGRAM',
        },
      ],
    });
    const records = await fetchPersonalRecords(client as never);
    expect(records).toHaveLength(1);
    expect(records[0]!.exercisePerformanceKey).toBe('key-1');
  });
});
