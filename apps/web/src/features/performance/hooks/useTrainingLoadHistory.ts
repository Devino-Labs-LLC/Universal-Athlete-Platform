import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import type { DateOnly } from '@/core/date/dateOnly';
import { fetchTrainingLoadHistory } from '@/features/performance/api/trainingLoadApi';
import { performanceKeys, type TrainingLoadHistoryFilters } from '@/features/performance/models/queryKeys';
import type { TrainingLoadGranularity } from '@/features/performance/models/schemas';

export function useTrainingLoadHistory(
  granularity: TrainingLoadGranularity,
  startDate: DateOnly,
  endDate: DateOnly,
  filters: TrainingLoadHistoryFilters = {},
) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: performanceKeys.loadHistory(granularity, startDate, endDate, filters),
    queryFn: () =>
      fetchTrainingLoadHistory(apiClient, {
        startDate,
        endDate,
        granularity,
        trainingPlanId: filters.trainingPlanId,
        workoutDayId: filters.workoutDayId,
        category: filters.category,
        movementPattern: filters.movementPattern,
        page: filters.page,
        size: filters.size,
      }),
    enabled: status === 'AUTHENTICATED' && Boolean(startDate) && Boolean(endDate),
  });
}
