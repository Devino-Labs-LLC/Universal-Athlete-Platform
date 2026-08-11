import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { fetchExerciseDefinition } from '@/features/exercises/api/exerciseDefinitionsApi';
import { exerciseKeys } from '@/features/exercises/models/queryKeys';

export function useExerciseDefinition(definitionId: string) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: exerciseKeys.detail(definitionId),
    queryFn: () => fetchExerciseDefinition(apiClient, definitionId),
    enabled: status === 'AUTHENTICATED' && Boolean(definitionId),
  });
}
