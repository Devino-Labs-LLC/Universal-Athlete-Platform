import { ApiClient } from '@/src/core/api/apiClient';
import { DateOnly } from '@/src/core/date/dateOnly';

export type BaselineWindowDays = 7 | 14 | 28;

export async function generateAthleteStateSnapshot(
  client: ApiClient,
  date: DateOnly,
  baselineWindowDays: BaselineWindowDays = 7,
): Promise<void> {
  await client.axios.post(`/api/v1/training/athlete-state/daily/${date}`, {
    baselineWindowDays,
  });
}

export async function generateReadinessAssessment(
  client: ApiClient,
  date: DateOnly,
): Promise<void> {
  await client.axios.post(`/api/v1/training/readiness/daily/${date}`);
}

export async function generateTrainingRecommendation(
  client: ApiClient,
  date: DateOnly,
): Promise<void> {
  await client.axios.post(`/api/v1/training/recommendations/daily/${date}`);
}
