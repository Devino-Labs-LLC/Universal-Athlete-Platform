import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { fetchCompatibility } from '@/features/exercises/api/compatibilityApi';
import { exerciseKeys } from '@/features/exercises/models/queryKeys';

export function useCompatibility(exerciseDefinitionId: string, environmentId: string) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: exerciseKeys.compatibility(exerciseDefinitionId, environmentId),
    queryFn: () => fetchCompatibility(apiClient, exerciseDefinitionId, environmentId),
    enabled: status === 'AUTHENTICATED' && Boolean(exerciseDefinitionId) && Boolean(environmentId),
  });
}
