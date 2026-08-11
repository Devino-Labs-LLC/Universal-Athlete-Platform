import { ApiClient } from '@/src/core/api/apiClient';
import { DateOnly } from '@/src/core/date/dateOnly';
import {
  AthleteExercisePerformanceHistory,
  athleteExercisePerformanceHistorySchema,
} from '@/src/features/performance/models/performanceSchemas';

const BASE = '/api/v1/training/performance';

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
  const response = await client.axios.get(`${BASE}/exercises/${exercisePerformanceKey}`, {
    params,
  });
  return athleteExercisePerformanceHistorySchema.parse(response.data);
}
