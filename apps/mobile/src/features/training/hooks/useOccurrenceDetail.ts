import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { fetchOccurrenceDetail } from '@/src/features/training/api/occurrenceApi';
import { trainingKeys } from '@/src/features/training/models/queryKeys';

export function useOccurrenceDetail(planId: string, dayId: string, occurrenceId: string) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: trainingKeys.occurrence(planId, dayId, occurrenceId),
    queryFn: () => fetchOccurrenceDetail(apiClient, planId, dayId, occurrenceId),
    enabled:
      status === 'AUTHENTICATED' &&
      Boolean(planId) &&
      Boolean(dayId) &&
      Boolean(occurrenceId),
    staleTime: 30_000,
    retry: 1,
  });
}
