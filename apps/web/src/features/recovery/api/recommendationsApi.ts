import type { ApiClient } from '@/core/api/apiClient';
import type { DateOnly } from '@/core/date/dateOnly';
import {
  type DailyTrainingRecommendation,
  dailyTrainingRecommendationComparisonSchema,
  type DailyTrainingRecommendationComparison,
  type DailyTrainingRecommendationHistory,
  dailyTrainingRecommendationHistorySchema,
  dailyTrainingRecommendationSchema,
} from '@/features/recovery/models/schemas';

const BASE_PATH = '/api/v1/training/recommendations';

export async function fetchRecommendation(
  client: ApiClient,
  recommendationId: string,
): Promise<DailyTrainingRecommendation> {
  const response = await client.axios.get(`${BASE_PATH}/${recommendationId}`);
  return dailyTrainingRecommendationSchema.parse(response.data);
}

export async function fetchRecommendationHistory(
  client: ApiClient,
  startDate: DateOnly,
  endDate: DateOnly,
  filters: {
    currentSnapshotOnly?: boolean;
    algorithmVersion?: string;
    overallAction?: string;
    page?: number;
    size?: number;
  } = {},
): Promise<DailyTrainingRecommendationHistory> {
  const response = await client.axios.get(`${BASE_PATH}/history`, {
    params: {
      startDate,
      endDate,
      currentSnapshotOnly: filters.currentSnapshotOnly ?? true,
      algorithmVersion: filters.algorithmVersion,
      overallAction: filters.overallAction,
      page: filters.page,
      size: filters.size,
    },
  });
  return dailyTrainingRecommendationHistorySchema.parse(response.data);
}

export async function fetchRecommendationComparison(
  client: ApiClient,
  olderRecommendationId: string,
  newerRecommendationId: string,
): Promise<DailyTrainingRecommendationComparison> {
  const response = await client.axios.get(`${BASE_PATH}/compare`, {
    params: { olderRecommendationId, newerRecommendationId },
  });
  return dailyTrainingRecommendationComparisonSchema.parse(response.data);
}
