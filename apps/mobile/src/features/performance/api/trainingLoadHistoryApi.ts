import { ApiClient } from '@/src/core/api/apiClient';
import { DateOnly } from '@/src/core/date/dateOnly';
import {
  TrainingLoadGranularity,
  TrainingLoadHistory,
  trainingLoadHistorySchema,
} from '@/src/features/performance/models/performanceSchemas';

const HISTORY_PATH = '/api/v1/training/training-load/history';

export interface TrainingLoadHistoryParams {
  startDate: DateOnly;
  endDate: DateOnly;
  granularity: TrainingLoadGranularity;
  trainingPlanId?: string;
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
