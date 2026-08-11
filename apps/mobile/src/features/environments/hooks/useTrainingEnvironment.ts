import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { fetchTrainingEnvironment } from '@/src/features/environments/api/environmentsApi';
import { environmentKeys } from '@/src/features/environments/models/environmentKeys';

export function useTrainingEnvironment(environmentId: string) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: environmentKeys.detail(environmentId),
    queryFn: () => fetchTrainingEnvironment(apiClient, environmentId),
    enabled: status === 'AUTHENTICATED' && Boolean(environmentId),
    staleTime: 30_000,
    retry: 1,
  });
}
