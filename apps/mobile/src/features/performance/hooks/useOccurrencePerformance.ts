import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/providers/AuthSessionProvider';
import { fetchOccurrencePerformance } from '@/src/features/performance/api/occurrencePerformanceApi';
import { performanceKeys } from '@/src/features/performance/models/performanceKeys';

export function useOccurrencePerformance(
  planId: string,
  dayId: string,
  occurrenceId: string,
  enabled = true,
) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: performanceKeys.occurrenceSummary(planId, dayId, occurrenceId),
    queryFn: () => fetchOccurrencePerformance(apiClient, planId, dayId, occurrenceId),
    enabled:
      enabled &&
      status === 'AUTHENTICATED' &&
      Boolean(planId) &&
      Boolean(dayId) &&
      Boolean(occurrenceId),
    staleTime: 30_000,
    retry: 1,
  });
}
