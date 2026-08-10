import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { fetchWorkoutLaunchContext } from '@/src/features/training/api/launchApi';
import { trainingKeys } from '@/src/features/training/models/queryKeys';

export function useWorkoutLaunchContext(planId: string, dayId: string, occurrenceId: string) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: trainingKeys.launch(planId, dayId, occurrenceId),
    queryFn: () => fetchWorkoutLaunchContext(apiClient, planId, dayId, occurrenceId),
    enabled:
      status === 'AUTHENTICATED' &&
      Boolean(planId) &&
      Boolean(dayId) &&
      Boolean(occurrenceId),
    staleTime: 15_000,
    retry: 1,
  });
}
