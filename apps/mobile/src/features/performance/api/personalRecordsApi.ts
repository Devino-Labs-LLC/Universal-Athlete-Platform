import { ApiClient } from '@/src/core/api/apiClient';
import {
  PersonalRecord,
  personalRecordsSchema,
} from '@/src/features/performance/models/performanceSchemas';

const BASE = '/api/v1/training/performance';

export async function fetchRecentPersonalRecords(
  client: ApiClient,
  days = 30,
  limit = 5,
): Promise<PersonalRecord[]> {
  const response = await client.axios.get(`${BASE}/personal-records/recent`, {
    params: { days, limit },
  });
  return personalRecordsSchema.parse(response.data);
}

export async function fetchPersonalRecords(
  client: ApiClient,
  filters?: { exercisePerformanceKey?: string; recordType?: string },
): Promise<PersonalRecord[]> {
  const response = await client.axios.get(`${BASE}/personal-records`, {
    params: filters,
  });
  return personalRecordsSchema.parse(response.data);
}

export async function fetchExercisePersonalRecords(
  client: ApiClient,
  exercisePerformanceKey: string,
): Promise<PersonalRecord[]> {
  const response = await client.axios.get(
    `${BASE}/exercises/${exercisePerformanceKey}/personal-records`,
  );
  return personalRecordsSchema.parse(response.data);
}
