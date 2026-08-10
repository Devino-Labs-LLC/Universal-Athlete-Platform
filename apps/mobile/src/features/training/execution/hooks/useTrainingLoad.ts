import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/providers/AuthSessionProvider';
import { fetchTrainingLoad } from '@/src/features/training/execution/api/trainingLoadApi';
import { OccurrenceScope } from '@/src/features/training/execution/models/invalidation';
import { trainingKeys } from '@/src/features/training/models/queryKeys';

export function useTrainingLoad(scope: OccurrenceScope, enabled = true) {
  const { apiClient, status } = useAuthSession();
  const { planId, dayId, occurrenceId } = scope;

  return useQuery({
    queryKey: trainingKeys.trainingLoad(planId, dayId, occurrenceId),
    queryFn: () => fetchTrainingLoad(apiClient, planId, dayId, occurrenceId),
    enabled:
      enabled && status === 'AUTHENTICATED' && Boolean(planId) && Boolean(dayId) && Boolean(occurrenceId),
    staleTime: 30_000,
    retry: 1,
  });
}
