import { useInfiniteQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/providers/AuthSessionProvider';
import { DateOnly } from '@/src/core/date/dateOnly';
import { fetchExercisePerformanceHistory } from '@/src/features/performance/api/exercisePerformanceApi';
import { performanceKeys } from '@/src/features/performance/models/performanceKeys';
import { getNextHistoryPage } from '@/src/features/performance/models/performanceSchemas';

const PAGE_SIZE = 20;

export function useExercisePerformanceHistory(
  exercisePerformanceKey: string,
  scheduledFrom?: DateOnly,
  scheduledTo?: DateOnly,
) {
  const { apiClient, status } = useAuthSession();

  return useInfiniteQuery({
    queryKey: performanceKeys.exerciseHistory(
      exercisePerformanceKey,
      scheduledFrom,
      scheduledTo,
    ),
    queryFn: ({ pageParam = 0 }) =>
      fetchExercisePerformanceHistory(apiClient, exercisePerformanceKey, {
        scheduledFrom,
        scheduledTo,
        page: pageParam,
        size: PAGE_SIZE,
      }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => getNextHistoryPage(lastPage.page, lastPage.totalPages),
    enabled: status === 'AUTHENTICATED' && Boolean(exercisePerformanceKey),
    staleTime: 30_000,
    retry: 1,
  });
}
