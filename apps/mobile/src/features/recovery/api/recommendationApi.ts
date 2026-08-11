import { ApiClient } from '@/src/core/api/apiClient';
import {
  DailyTrainingRecommendation,
  dailyTrainingRecommendationSchema,
} from '@/src/features/recovery/models/recoverySchemas';

const BASE_PATH = '/api/v1/training/recommendations';

export async function fetchTrainingRecommendation(
  client: ApiClient,
  recommendationId: string,
): Promise<DailyTrainingRecommendation> {
  const response = await client.axios.get(`${BASE_PATH}/${recommendationId}`);
  return dailyTrainingRecommendationSchema.parse(response.data);
}
