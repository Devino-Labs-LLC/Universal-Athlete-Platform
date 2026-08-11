import type { ApiClient } from '@/core/api/apiClient';
import type { DateOnly } from '@/core/date/dateOnly';
import {
  type DailyAthleteStateHistory,
  dailyAthleteStateHistorySchema,
  type DailyAthleteStateSnapshot,
  dailyAthleteStateSnapshotSchema,
  type DailyAthleteStateSnapshotComparison,
  dailyAthleteStateSnapshotComparisonSchema,
  type DailyAthleteStateSnapshotVersion,
  dailyAthleteStateSnapshotVersionSchema,
} from '@/features/recovery/models/schemas';
import { z } from 'zod';

const BASE_PATH = '/api/v1/training/athlete-state';

export async function fetchAthleteStateForDate(
  client: ApiClient,
  date: DateOnly,
): Promise<DailyAthleteStateSnapshot> {
  const response = await client.axios.get(`${BASE_PATH}/daily/${date}`);
  return dailyAthleteStateSnapshotSchema.parse(response.data);
}

export async function fetchAthleteStateSnapshot(
  client: ApiClient,
  snapshotId: string,
): Promise<DailyAthleteStateSnapshot> {
  const response = await client.axios.get(`${BASE_PATH}/snapshots/${snapshotId}`);
  return dailyAthleteStateSnapshotSchema.parse(response.data);
}

export async function fetchAthleteStateVersions(
  client: ApiClient,
  date: DateOnly,
): Promise<DailyAthleteStateSnapshotVersion[]> {
  const response = await client.axios.get(`${BASE_PATH}/daily/${date}/versions`);
  return z.array(dailyAthleteStateSnapshotVersionSchema).parse(response.data);
}

export async function fetchAthleteStateHistory(
  client: ApiClient,
  startDate: DateOnly,
  endDate: DateOnly,
  filters: { currentOnly?: boolean; page?: number; size?: number } = {},
): Promise<DailyAthleteStateHistory> {
  const response = await client.axios.get(`${BASE_PATH}/history`, {
    params: {
      startDate,
      endDate,
      currentOnly: filters.currentOnly ?? true,
      page: filters.page,
      size: filters.size,
    },
  });
  return dailyAthleteStateHistorySchema.parse(response.data);
}

export async function fetchAthleteStateComparison(
  client: ApiClient,
  olderSnapshotId: string,
  newerSnapshotId: string,
): Promise<DailyAthleteStateSnapshotComparison> {
  const response = await client.axios.get(`${BASE_PATH}/snapshots/compare`, {
    params: { olderSnapshotId, newerSnapshotId },
  });
  return dailyAthleteStateSnapshotComparisonSchema.parse(response.data);
}
