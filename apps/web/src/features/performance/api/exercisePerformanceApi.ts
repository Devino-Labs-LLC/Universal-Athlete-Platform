import type { ApiClient } from '@/core/api/apiClient';
import type { DateOnly } from '@/core/date/dateOnly';
import {
  type AthleteExercisePerformanceHistory,
  athleteExercisePerformanceHistorySchema,
} from '@/features/performance/models/schemas';

const BASE_PATH = '/api/v1/training/performance';

export interface ExerciseHistoryParams {
  scheduledFrom?: DateOnly;
  scheduledTo?: DateOnly;
  page?: number;
  size?: number;
}

export async function fetchExercisePerformanceHistory(
  client: ApiClient,
  exercisePerformanceKey: string,
  params: ExerciseHistoryParams = {},
): Promise<AthleteExercisePerformanceHistory> {
  const response = await client.axios.get(`${BASE_PATH}/exercises/${exercisePerformanceKey}`, {
    params,
  });
  return athleteExercisePerformanceHistorySchema.parse(response.data);
}
