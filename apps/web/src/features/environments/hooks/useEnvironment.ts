import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { fetchEnvironment } from '@/features/environments/api/environmentsApi';
import { environmentKeys } from '@/features/environments/models/queryKeys';

export function useEnvironment(environmentId: string) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: environmentKeys.detail(environmentId),
    queryFn: () => fetchEnvironment(apiClient, environmentId),
    enabled: status === 'AUTHENTICATED' && Boolean(environmentId),
  });
}
