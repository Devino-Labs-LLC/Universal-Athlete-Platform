import type { QueryClient } from '@tanstack/react-query';

import { exerciseKeys, TRAINING_EXERCISE_DEFINITIONS_PREFIX } from '@/features/exercises/models/queryKeys';

export function invalidateExerciseDefinitionQueries(
  queryClient: QueryClient,
  definitionId?: string,
): void {
  void queryClient.invalidateQueries({ queryKey: exerciseKeys.lists() });
  void queryClient.invalidateQueries({ queryKey: TRAINING_EXERCISE_DEFINITIONS_PREFIX });
  if (definitionId) {
    void queryClient.invalidateQueries({ queryKey: exerciseKeys.detail(definitionId) });
  }
}

export function invalidateSubstitutionQueries(
  queryClient: QueryClient,
  sourceId?: string,
  relationshipId?: string,
): void {
  if (sourceId) {
    void queryClient.invalidateQueries({ queryKey: exerciseKeys.candidatesFor(sourceId) });
  }
  if (relationshipId) {
    void queryClient.invalidateQueries({ queryKey: exerciseKeys.relationship(relationshipId) });
  }
}

export function invalidateCompatibilityQueries(
  queryClient: QueryClient,
  exerciseDefinitionId: string,
): void {
  void queryClient.invalidateQueries({ queryKey: exerciseKeys.compatibilityFor(exerciseDefinitionId) });
}
