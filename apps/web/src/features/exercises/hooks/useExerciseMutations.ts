import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  archiveExerciseDefinition,
  createExerciseDefinition,
  updateExerciseDefinition,
} from '@/features/exercises/api/exerciseDefinitionsApi';
import { invalidateExerciseDefinitionQueries } from '@/features/exercises/models/invalidation';
import { exerciseKeys } from '@/features/exercises/models/queryKeys';
import type {
  CreateExerciseDefinitionRequest,
  UpdateExerciseDefinitionRequest,
} from '@/features/exercises/models/schemas';

export function useCreateExerciseDefinitionMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateExerciseDefinitionRequest) =>
      createExerciseDefinition(apiClient, request),
    onSuccess: (definition) => {
      queryClient.setQueryData(exerciseKeys.detail(definition.id), definition);
      invalidateExerciseDefinitionQueries(queryClient, definition.id);
    },
  });
}

export function useUpdateExerciseDefinitionMutation(definitionId: string) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (patch: UpdateExerciseDefinitionRequest) =>
      updateExerciseDefinition(apiClient, definitionId, patch),
    onSuccess: (definition) => {
      queryClient.setQueryData(exerciseKeys.detail(definitionId), definition);
      invalidateExerciseDefinitionQueries(queryClient, definitionId);
    },
  });
}

export function useArchiveExerciseDefinitionMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (definitionId: string) => archiveExerciseDefinition(apiClient, definitionId),
    onSuccess: (_result, definitionId) => {
      invalidateExerciseDefinitionQueries(queryClient, definitionId);
    },
  });
}
