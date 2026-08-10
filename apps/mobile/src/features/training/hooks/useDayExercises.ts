import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { fetchDayExercises } from '@/src/features/training/api/planApi';
import { trainingKeys } from '@/src/features/training/models/queryKeys';

export function useDayExercises(planId: string, dayId: string) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: trainingKeys.dayExercises(planId, dayId),
    queryFn: () => fetchDayExercises(apiClient, planId, dayId),
    enabled: status === 'AUTHENTICATED' && Boolean(planId) && Boolean(dayId),
    staleTime: 60_000,
    retry: 1,
  });
}
