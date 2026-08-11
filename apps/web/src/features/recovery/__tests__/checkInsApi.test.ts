import { describe, expect, it, vi } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import {
  fetchRecoveryCheckInByDate,
  fetchRecoveryCheckInById,
  fetchRecoveryCheckInList,
  fetchRecoveryCheckInRevisions,
  fetchRecoveryHistory,
} from '@/features/recovery/api/checkInsApi';

function makeClient() {
  return { axios: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() } };
}

const checkIn = {
  id: 'ci-1',
  checkInDate: '2026-02-01',
  fatigue: { value: 3, label: 'Moderate' },
  muscleSoreness: { value: 2, label: 'Mild' },
  stress: { value: 3, label: 'Moderate' },
  mood: { value: 4, label: 'Good' },
  motivation: { value: 4, label: 'High' },
  completeness: 'COMPLETE',
  discomfortAreas: [],
  version: 1,
};

describe('checkInsApi', () => {
  it('fetches by id from /recovery-check-ins/{id}', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: checkIn });
    await fetchRecoveryCheckInById(client as never, 'ci-1');
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/recovery-check-ins/ci-1');
  });

  it('fetches by date from /recovery-check-ins/by-date/{date}', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: checkIn });
    await fetchRecoveryCheckInByDate(client as never, parseDateOnly('2026-02-01'));
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/recovery-check-ins/by-date/2026-02-01');
  });

  it('fetches revisions from /recovery-check-ins/{id}/revisions and unwraps the envelope', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: { revisions: [{ id: 'rev-1', revisionNumber: 1 }] } });
    const revisions = await fetchRecoveryCheckInRevisions(client as never, 'ci-1');
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/recovery-check-ins/ci-1/revisions');
    expect(revisions).toHaveLength(1);
  });

  it('fetches the list with startDate/endDate/page/size and optional filters', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: { checkIns: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } });
    const start = parseDateOnly('2026-01-01');
    const end = parseDateOnly('2026-01-31');
    await fetchRecoveryCheckInList(client as never, start, end, { completeness: 'COMPLETE', page: 1, size: 10 });
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/recovery-check-ins', {
      params: {
        startDate: start,
        endDate: end,
        completeness: 'COMPLETE',
        minimumFatigue: undefined,
        minimumSoreness: undefined,
        bodyArea: undefined,
        page: 1,
        size: 10,
      },
    });
  });

  it('fetches history with includeTrainingLoad defaulted to true', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: { days: [] } });
    const start = parseDateOnly('2026-01-01');
    const end = parseDateOnly('2026-01-31');
    await fetchRecoveryHistory(client as never, start, end);
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/recovery-check-ins/history', {
      params: { startDate: start, endDate: end, includeTrainingLoad: true },
    });
  });

  it('allows disabling includeTrainingLoad for history', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: { days: [] } });
    const start = parseDateOnly('2026-01-01');
    const end = parseDateOnly('2026-01-31');
    await fetchRecoveryHistory(client as never, start, end, false);
    expect(client.axios.get).toHaveBeenCalledWith(
      '/api/v1/training/recovery-check-ins/history',
      expect.objectContaining({ params: expect.objectContaining({ includeTrainingLoad: false }) }),
    );
  });
});
