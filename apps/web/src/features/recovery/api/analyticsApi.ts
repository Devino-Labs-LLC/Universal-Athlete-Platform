import type { ApiClient } from '@/core/api/apiClient';
import type { DateOnly } from '@/core/date/dateOnly';
import {
  type BaselineWindowDays,
  type BodyAreaDiscomfortHistory,
  bodyAreaDiscomfortHistorySchema,
  type RecoveryBaselineDashboard,
  recoveryBaselineDashboardSchema,
  type RecoveryMetricTrend,
  recoveryMetricTrendSchema,
  type RecoveryMetricType,
} from '@/features/recovery/models/schemas';

const BASE_PATH = '/api/v1/training/recovery-analytics';

export async function fetchRecoveryDashboard(
  client: ApiClient,
  baselineWindowDays: BaselineWindowDays,
  targetDate?: DateOnly,
  includeTrainingLoad = false,
): Promise<RecoveryBaselineDashboard> {
  const response = await client.axios.get(`${BASE_PATH}/dashboard`, {
    params: { baselineWindowDays, targetDate, includeTrainingLoad },
  });
  return recoveryBaselineDashboardSchema.parse(response.data);
}

export async function fetchRecoveryMetricTrend(
  client: ApiClient,
  metricType: RecoveryMetricType,
  startDate: DateOnly,
  endDate: DateOnly,
  includeTrainingLoad = false,
): Promise<RecoveryMetricTrend> {
  const response = await client.axios.get(`${BASE_PATH}/trends/${metricType}`, {
    params: { startDate, endDate, includeTrainingLoad },
  });
  return recoveryMetricTrendSchema.parse(response.data);
}

export async function fetchBodyAreaDiscomfortHistory(
  client: ApiClient,
  startDate: DateOnly,
  endDate: DateOnly,
  filters: { bodyArea?: string; bodySide?: string } = {},
): Promise<BodyAreaDiscomfortHistory> {
  const response = await client.axios.get(`${BASE_PATH}/discomfort-history`, {
    params: { startDate, endDate, bodyArea: filters.bodyArea, bodySide: filters.bodySide },
  });
  return bodyAreaDiscomfortHistorySchema.parse(response.data);
}
