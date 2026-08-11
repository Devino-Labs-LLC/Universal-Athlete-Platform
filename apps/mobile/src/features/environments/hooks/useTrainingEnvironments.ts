import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { listTrainingEnvironments } from '@/src/features/environments/api/environmentsApi';
import { environmentKeys } from '@/src/features/environments/models/environmentKeys';
import { TrainingEnvironmentListFilters } from '@/src/features/environments/models/environmentSchemas';

export function useTrainingEnvironments(filters?: TrainingEnvironmentListFilters) {
  const { apiClient, status } = useAuthSession();
  const resolvedFilters: TrainingEnvironmentListFilters = {
    activeOnly: true,
    page: 0,
    size: 50,
    ...filters,
  };

  return useQuery({
    queryKey: environmentKeys.list(resolvedFilters),
    queryFn: () => listTrainingEnvironments(apiClient, resolvedFilters),
    enabled: status === 'AUTHENTICATED',
    staleTime: 30_000,
    retry: 1,
  });
}
