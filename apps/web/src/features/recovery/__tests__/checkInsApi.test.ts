import { describe, expect, it, vi } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import {
  createRecoveryCheckIn,
  fetchRecoveryCheckInByDate,
  fetchRecoveryCheckInById,
  fetchRecoveryCheckInList,
  fetchRecoveryCheckInRevisions,
  fetchRecoveryHistory,
  updateRecoveryCheckIn,
  buildCreateRequestFromForm,
  buildUpdateRequestFromForm,
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

  it('creates a check-in with POST /recovery-check-ins', async () => {
    const client = makeClient();
    client.axios.post.mockResolvedValue({ data: checkIn });
    const created = await createRecoveryCheckIn(client as never, {
      checkInDate: '2026-02-01',
      fatigue: 3,
      muscleSoreness: 2,
      stress: 3,
      mood: 4,
      motivation: 4,
    });
    expect(client.axios.post).toHaveBeenCalledWith('/api/v1/training/recovery-check-ins', {
      checkInDate: '2026-02-01',
      fatigue: 3,
      muscleSoreness: 2,
      stress: 3,
      mood: 4,
      motivation: 4,
    });
    expect(created.id).toBe('ci-1');
  });

  it('updates a check-in with PATCH and bare PatchValue fields', async () => {
    const client = makeClient();
    client.axios.patch.mockResolvedValue({ data: checkIn });
    await updateRecoveryCheckIn(client as never, 'ci-1', {
      fatigue: 4,
      sleepDurationMinutes: null,
      expectedVersion: 1,
    });
    expect(client.axios.patch).toHaveBeenCalledWith('/api/v1/training/recovery-check-ins/ci-1', {
      fatigue: 4,
      sleepDurationMinutes: null,
      expectedVersion: 1,
    });
  });

  it('omits blank optional fields on create and sends null on update (missing ≠ zero)', () => {
    const values = {
      checkInDate: '2026-02-01',
      fatigue: 3,
      muscleSoreness: 3,
      stress: 3,
      mood: 3,
      motivation: 3,
      discomfortAreas: [],
    };
    expect(buildCreateRequestFromForm(values).sleepDurationMinutes).toBeUndefined();
    expect(buildCreateRequestFromForm(values).sleepQuality).toBeUndefined();
    expect(buildCreateRequestFromForm(values).discomfortAreas).toBeUndefined();
    expect(buildUpdateRequestFromForm(values, 2).sleepDurationMinutes).toBeNull();
    expect(buildUpdateRequestFromForm(values, 2).sleepQuality).toBeNull();
    expect(buildUpdateRequestFromForm(values, 2).notes).toBeNull();
    expect(buildUpdateRequestFromForm(values, 2).expectedVersion).toBe(2);
  });
});
