import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { fetchTrainingPlan } from '@/src/features/training/api/planApi';
import { trainingKeys } from '@/src/features/training/models/queryKeys';

export function useTrainingPlan(planId: string) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: trainingKeys.plan(planId),
    queryFn: () => fetchTrainingPlan(apiClient, planId),
    enabled: status === 'AUTHENTICATED' && Boolean(planId),
    staleTime: 60_000,
    retry: 1,
  });
}
