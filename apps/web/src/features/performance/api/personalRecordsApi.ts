import type { ApiClient } from '@/core/api/apiClient';
import { type PersonalRecord, personalRecordsSchema } from '@/features/performance/models/schemas';

const BASE_PATH = '/api/v1/training/performance';

export async function fetchRecentPersonalRecords(client: ApiClient, days = 30, limit = 5): Promise<PersonalRecord[]> {
  const response = await client.axios.get(`${BASE_PATH}/personal-records/recent`, {
    params: { days, limit },
  });
  return personalRecordsSchema.parse(response.data);
}

export async function fetchPersonalRecords(
  client: ApiClient,
  filters: { exercisePerformanceKey?: string; recordType?: string } = {},
): Promise<PersonalRecord[]> {
  const response = await client.axios.get(`${BASE_PATH}/personal-records`, {
    params: filters,
  });
  return personalRecordsSchema.parse(response.data);
}

export async function fetchExercisePersonalRecords(
  client: ApiClient,
  exercisePerformanceKey: string,
): Promise<PersonalRecord[]> {
  const response = await client.axios.get(`${BASE_PATH}/exercises/${exercisePerformanceKey}/personal-records`);
  return personalRecordsSchema.parse(response.data);
}
