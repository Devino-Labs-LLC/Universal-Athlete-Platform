import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import type { DateOnly } from '@/core/date/dateOnly';
import {
  fetchBodyAreaDiscomfortHistory,
  fetchRecoveryDashboard,
  fetchRecoveryMetricTrend,
} from '@/features/recovery/api/analyticsApi';
import { recoveryKeys } from '@/features/recovery/models/queryKeys';
import type { BaselineWindowDays, RecoveryMetricType } from '@/features/recovery/models/schemas';

export function useRecoveryDashboard(
  baselineWindowDays: BaselineWindowDays,
  targetDate?: DateOnly,
  includeTrainingLoad = false,
) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.dashboard(baselineWindowDays, targetDate, includeTrainingLoad),
    queryFn: () => fetchRecoveryDashboard(apiClient, baselineWindowDays, targetDate, includeTrainingLoad),
    enabled: status === 'AUTHENTICATED',
  });
}

export function useRecoveryMetricTrend(
  metricType: RecoveryMetricType,
  startDate: DateOnly,
  endDate: DateOnly,
  includeTrainingLoad = false,
) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.trend(metricType, startDate, endDate, includeTrainingLoad),
    queryFn: () => fetchRecoveryMetricTrend(apiClient, metricType, startDate, endDate, includeTrainingLoad),
    enabled: status === 'AUTHENTICATED' && Boolean(startDate) && Boolean(endDate),
  });
}

export function useBodyAreaDiscomfortHistory(
  startDate: DateOnly,
  endDate: DateOnly,
  filters?: { bodyArea?: string; bodySide?: string },
) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.discomfortHistory(startDate, endDate, filters),
    queryFn: () => fetchBodyAreaDiscomfortHistory(apiClient, startDate, endDate, filters),
    enabled: status === 'AUTHENTICATED' && Boolean(startDate) && Boolean(endDate),
  });
}
