import type { ApiClient } from '@/core/api/apiClient';
import type { DateOnly } from '@/core/date/dateOnly';
import type { RecoveryCheckInListFilters } from '@/features/recovery/models/queryKeys';
import {
  type AthleteRecoveryHistory,
  athleteRecoveryHistorySchema,
  type DailyRecoveryCheckIn,
  dailyRecoveryCheckInListSchema,
  dailyRecoveryCheckInSchema,
  type DailyRecoveryCheckInList,
  type RecoveryCheckInRevision,
  recoveryCheckInRevisionListSchema,
} from '@/features/recovery/models/schemas';

const BASE_PATH = '/api/v1/training/recovery-check-ins';

export async function fetchRecoveryCheckInById(client: ApiClient, checkInId: string): Promise<DailyRecoveryCheckIn> {
  const response = await client.axios.get(`${BASE_PATH}/${checkInId}`);
  return dailyRecoveryCheckInSchema.parse(response.data);
}

export async function fetchRecoveryCheckInByDate(client: ApiClient, date: DateOnly): Promise<DailyRecoveryCheckIn> {
  const response = await client.axios.get(`${BASE_PATH}/by-date/${date}`);
  return dailyRecoveryCheckInSchema.parse(response.data);
}

export async function fetchRecoveryCheckInList(
  client: ApiClient,
  startDate: DateOnly,
  endDate: DateOnly,
  filters: RecoveryCheckInListFilters = {},
): Promise<DailyRecoveryCheckInList> {
  const response = await client.axios.get(BASE_PATH, {
    params: {
      startDate,
      endDate,
      completeness: filters.completeness,
      minimumFatigue: filters.minimumFatigue,
      minimumSoreness: filters.minimumSoreness,
      bodyArea: filters.bodyArea,
      page: filters.page,
      size: filters.size,
    },
  });
  return dailyRecoveryCheckInListSchema.parse(response.data);
}

export async function fetchRecoveryCheckInRevisions(
  client: ApiClient,
  checkInId: string,
): Promise<RecoveryCheckInRevision[]> {
  const response = await client.axios.get(`${BASE_PATH}/${checkInId}/revisions`);
  return recoveryCheckInRevisionListSchema.parse(response.data).revisions;
}

export async function fetchRecoveryHistory(
  client: ApiClient,
  startDate: DateOnly,
  endDate: DateOnly,
  includeTrainingLoad = true,
): Promise<AthleteRecoveryHistory> {
  const response = await client.axios.get(`${BASE_PATH}/history`, {
    params: { startDate, endDate, includeTrainingLoad },
  });
  return athleteRecoveryHistorySchema.parse(response.data);
}
