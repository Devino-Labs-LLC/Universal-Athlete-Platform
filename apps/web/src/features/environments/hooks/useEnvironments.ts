import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { fetchEnvironments } from '@/features/environments/api/environmentsApi';
import { environmentKeys } from '@/features/environments/models/queryKeys';
import type { EnvironmentListFilters } from '@/features/environments/models/schemas';

export function useEnvironments(filters?: EnvironmentListFilters) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: environmentKeys.list(filters),
    queryFn: () => fetchEnvironments(apiClient, filters),
    enabled: status === 'AUTHENTICATED',
  });
}
