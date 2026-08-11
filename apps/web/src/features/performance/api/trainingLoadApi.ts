import type { ApiClient } from '@/core/api/apiClient';
import type { DateOnly } from '@/core/date/dateOnly';
import {
  type TrainingLoadGranularity,
  type TrainingLoadHistory,
  trainingLoadHistorySchema,
} from '@/features/performance/models/schemas';

const HISTORY_PATH = '/api/v1/training/training-load/history';

export interface TrainingLoadHistoryParams {
  startDate: DateOnly;
  endDate: DateOnly;
  granularity: TrainingLoadGranularity;
  trainingPlanId?: string;
  workoutDayId?: string;
  category?: string;
  movementPattern?: string;
  page?: number;
  size?: number;
}

export async function fetchTrainingLoadHistory(
  client: ApiClient,
  params: TrainingLoadHistoryParams,
): Promise<TrainingLoadHistory> {
  const response = await client.axios.get(HISTORY_PATH, { params });
  return trainingLoadHistorySchema.parse(response.data);
}
