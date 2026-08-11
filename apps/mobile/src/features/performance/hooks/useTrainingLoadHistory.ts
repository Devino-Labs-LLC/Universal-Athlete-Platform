import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/providers/AuthSessionProvider';
import { DateOnly } from '@/src/core/date/dateOnly';
import { fetchTrainingLoadHistory } from '@/src/features/performance/api/trainingLoadHistoryApi';
import { performanceKeys } from '@/src/features/performance/models/performanceKeys';
import { TrainingLoadGranularity } from '@/src/features/performance/models/performanceSchemas';

export interface UseTrainingLoadHistoryOptions {
  startDate: DateOnly;
  endDate: DateOnly;
  granularity: TrainingLoadGranularity;
  trainingPlanId?: string;
  page?: number;
  size?: number;
}

export function useTrainingLoadHistory(options: UseTrainingLoadHistoryOptions) {
  const { apiClient, status } = useAuthSession();
  const { startDate, endDate, granularity, trainingPlanId, page = 0, size = 20 } = options;

  return useQuery({
    queryKey: performanceKeys.loadHistory(granularity, startDate, endDate, {
      trainingPlanId,
      page,
      size,
    }),
    queryFn: () =>
      fetchTrainingLoadHistory(apiClient, {
        startDate,
        endDate,
        granularity,
        trainingPlanId,
        page,
        size,
      }),
    enabled: status === 'AUTHENTICATED' && Boolean(startDate) && Boolean(endDate),
    staleTime: 30_000,
    retry: 1,
  });
}
