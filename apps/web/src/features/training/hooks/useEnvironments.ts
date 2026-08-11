import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { fetchTrainingEnvironments } from '@/features/training/api/environmentsApi';
import { trainingKeys } from '@/features/training/models/queryKeys';

export function useTrainingEnvironments(activeOnly = true) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: trainingKeys.environments(),
    queryFn: () => fetchTrainingEnvironments(apiClient, activeOnly),
    enabled: status === 'AUTHENTICATED',
  });
}
