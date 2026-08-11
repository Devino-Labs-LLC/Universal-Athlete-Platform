import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { fetchExerciseDefinitions } from '@/features/training/api/exerciseDefinitionsApi';
import type { ExerciseDefinitionFilters } from '@/features/training/models/queryKeys';
import { trainingKeys } from '@/features/training/models/queryKeys';

export function useExerciseDefinitions(filters?: ExerciseDefinitionFilters) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: trainingKeys.exerciseDefinitions(filters),
    queryFn: () => fetchExerciseDefinitions(apiClient, filters),
    enabled: status === 'AUTHENTICATED',
  });
}
