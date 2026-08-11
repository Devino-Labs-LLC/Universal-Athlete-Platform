import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { fetchOccurrencePerformance } from '@/features/performance/api/occurrencePerformanceApi';
import { performanceKeys } from '@/features/performance/models/queryKeys';

export function useOccurrencePerformance(
  planId: string | undefined,
  dayId: string | undefined,
  occurrenceId: string | undefined,
) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: performanceKeys.occurrenceSummary(planId ?? '', dayId ?? '', occurrenceId ?? ''),
    queryFn: () => fetchOccurrencePerformance(apiClient, planId as string, dayId as string, occurrenceId as string),
    enabled: status === 'AUTHENTICATED' && Boolean(planId) && Boolean(dayId) && Boolean(occurrenceId),
  });
}
