import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { fetchExerciseDefinitions } from '@/features/exercises/api/exerciseDefinitionsApi';
import { exerciseKeys } from '@/features/exercises/models/queryKeys';
import type { ExerciseDefinitionListFilters } from '@/features/exercises/models/schemas';

export function useExerciseDefinitions(filters?: ExerciseDefinitionListFilters) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: exerciseKeys.list(filters),
    queryFn: () => fetchExerciseDefinitions(apiClient, filters),
    enabled: status === 'AUTHENTICATED',
    placeholderData: (previous) => previous,
  });
}
