import type { ApiClient } from '@/core/api/apiClient';
import type { DateOnly } from '@/core/date/dateOnly';
import {
  type DailyReadinessAssessment,
  dailyReadinessAssessmentComparisonSchema,
  dailyReadinessAssessmentSchema,
  type DailyReadinessAssessmentComparison,
  type DailyReadinessHistory,
  dailyReadinessHistorySchema,
} from '@/features/recovery/models/schemas';

const BASE_PATH = '/api/v1/training/readiness';

export async function fetchReadinessAssessment(
  client: ApiClient,
  assessmentId: string,
): Promise<DailyReadinessAssessment> {
  const response = await client.axios.get(`${BASE_PATH}/assessments/${assessmentId}`);
  return dailyReadinessAssessmentSchema.parse(response.data);
}

export async function fetchReadinessHistory(
  client: ApiClient,
  startDate: DateOnly,
  endDate: DateOnly,
  filters: { currentSnapshotOnly?: boolean; algorithmVersion?: string; page?: number; size?: number } = {},
): Promise<DailyReadinessHistory> {
  const response = await client.axios.get(`${BASE_PATH}/history`, {
    params: {
      startDate,
      endDate,
      currentSnapshotOnly: filters.currentSnapshotOnly ?? true,
      algorithmVersion: filters.algorithmVersion,
      page: filters.page,
      size: filters.size,
    },
  });
  return dailyReadinessHistorySchema.parse(response.data);
}

export async function fetchReadinessComparison(
  client: ApiClient,
  olderAssessmentId: string,
  newerAssessmentId: string,
): Promise<DailyReadinessAssessmentComparison> {
  const response = await client.axios.get(`${BASE_PATH}/assessments/compare`, {
    params: { olderAssessmentId, newerAssessmentId },
  });
  return dailyReadinessAssessmentComparisonSchema.parse(response.data);
}
