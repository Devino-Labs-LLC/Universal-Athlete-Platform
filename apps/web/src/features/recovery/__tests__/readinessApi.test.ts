import { describe, expect, it, vi } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import {
  fetchReadinessAssessment,
  fetchReadinessComparison,
  fetchReadinessHistory,
} from '@/features/recovery/api/readinessApi';

function makeClient() {
  return { axios: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() } };
}

const assessment = {
  assessmentId: 'ra-1',
  stateDate: '2026-02-01',
  readinessBand: 'HIGH',
  dataSufficiency: 'SUFFICIENT',
  limitingDimensions: [],
  strongestDimensions: [],
  contributions: [],
};

describe('readinessApi', () => {
  it('fetches an assessment from /readiness/assessments/{id}', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: assessment });
    await fetchReadinessAssessment(client as never, 'ra-1');
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/readiness/assessments/ra-1');
  });

  it('fetches history with currentSnapshotOnly defaulted to true', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: { content: [], page: 0, size: 20, totalElements: 0 } });
    const start = parseDateOnly('2026-01-01');
    const end = parseDateOnly('2026-01-31');
    await fetchReadinessHistory(client as never, start, end);
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/readiness/history', {
      params: { startDate: start, endDate: end, currentSnapshotOnly: true, algorithmVersion: undefined, page: undefined, size: undefined },
    });
  });

  it('fetches a comparison from /readiness/assessments/compare with older/newer ids', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: {
        olderAssessmentId: 'a',
        newerAssessmentId: 'b',
        olderStateDate: '2026-01-01',
        newerStateDate: '2026-01-02',
        bandChanged: false,
        olderBand: 'HIGH',
        newerBand: 'HIGH',
        dataSufficiencyChanged: false,
        olderDataSufficiency: 'SUFFICIENT',
        newerDataSufficiency: 'SUFFICIENT',
        limitingDimensionsChanged: false,
        olderLimitingDimensions: [],
        newerLimitingDimensions: [],
        dimensionChanges: [],
      },
    });
    await fetchReadinessComparison(client as never, 'a', 'b');
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/readiness/assessments/compare', {
      params: { olderAssessmentId: 'a', newerAssessmentId: 'b' },
    });
  });
});
