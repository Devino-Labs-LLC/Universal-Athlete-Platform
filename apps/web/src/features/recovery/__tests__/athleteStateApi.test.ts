import { describe, expect, it, vi } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import {
  fetchAthleteStateComparison,
  fetchAthleteStateForDate,
  fetchAthleteStateHistory,
  fetchAthleteStateSnapshot,
  fetchAthleteStateVersions,
} from '@/features/recovery/api/athleteStateApi';

function makeClient() {
  return { axios: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() } };
}

const snapshot = {
  snapshotId: 'snap-1',
  stateDate: '2026-02-01',
  snapshotVersion: 1,
  current: true,
  recovery: { checkInPresent: false, discomfortObservations: [] },
  recoveryMetrics: [],
  trainingLoad: {
    occurrenceCount: 0,
    completedOccurrenceCount: 0,
    ratedOccurrenceCount: 0,
    unratedOccurrenceCount: 0,
    completedExerciseCount: 0,
    completedSetCount: 0,
    completedRepetitionCount: 0,
    totalDurationSeconds: 0,
    totalSessionDurationMinutes: 0,
    noImpactExerciseCount: 0,
    lowImpactExerciseCount: 0,
    moderateImpactExerciseCount: 0,
    highImpactExerciseCount: 0,
    categorySummaries: [],
    movementSummaries: [],
  },
  schedule: {
    scheduledOccurrenceCount: 0,
    scheduledWorkoutCount: 0,
    completedScheduledCount: 0,
    skippedScheduledCount: 0,
    cancelledScheduledCount: 0,
    inProgressScheduledCount: 0,
    scheduledOccurrences: [],
  },
};

describe('athleteStateApi', () => {
  it('fetches by date from /athlete-state/daily/{date}', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: snapshot });
    await fetchAthleteStateForDate(client as never, parseDateOnly('2026-02-01'));
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/athlete-state/daily/2026-02-01');
  });

  it('fetches by snapshot id from /athlete-state/snapshots/{id}', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: snapshot });
    await fetchAthleteStateSnapshot(client as never, 'snap-1');
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/athlete-state/snapshots/snap-1');
  });

  it('fetches versions from /athlete-state/daily/{date}/versions', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: [] });
    await fetchAthleteStateVersions(client as never, parseDateOnly('2026-02-01'));
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/athlete-state/daily/2026-02-01/versions');
  });

  it('fetches history with startDate/endDate/currentOnly/page/size', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: { content: [], page: 0, size: 20, totalElements: 0 } });
    const start = parseDateOnly('2026-01-01');
    const end = parseDateOnly('2026-01-31');
    await fetchAthleteStateHistory(client as never, start, end, { page: 1, size: 10 });
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/athlete-state/history', {
      params: { startDate: start, endDate: end, currentOnly: true, page: 1, size: 10 },
    });
  });

  it('fetches a snapshot comparison from /athlete-state/snapshots/compare', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: {
        olderSnapshotId: 'a',
        newerSnapshotId: 'b',
        olderStateDate: '2026-01-01',
        newerStateDate: '2026-01-02',
        olderVersion: 1,
        newerVersion: 1,
        recoveryChanged: false,
        baselineChanged: false,
        trainingLoadChanged: false,
        scheduleChanged: false,
        discomfortChanged: false,
        fieldDifferences: [],
      },
    });
    await fetchAthleteStateComparison(client as never, 'a', 'b');
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/athlete-state/snapshots/compare', {
      params: { olderSnapshotId: 'a', newerSnapshotId: 'b' },
    });
  });
});
