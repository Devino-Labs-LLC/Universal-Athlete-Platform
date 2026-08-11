import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  createSubstitutionRelationship,
  deleteSubstitutionRelationship,
  updateSubstitutionRelationship,
} from '@/features/exercises/api/substitutionsApi';
import { invalidateSubstitutionQueries } from '@/features/exercises/models/invalidation';
import type { CreateSubstitutionRequest, UpdateSubstitutionRequest } from '@/features/exercises/models/schemas';

export function useCreateSubstitutionMutation(sourceId: string) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateSubstitutionRequest) =>
      createSubstitutionRelationship(apiClient, sourceId, request),
    onSuccess: () => {
      invalidateSubstitutionQueries(queryClient, sourceId);
    },
  });
}

export function useUpdateSubstitutionMutation(sourceId: string, relationshipId: string) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: UpdateSubstitutionRequest) =>
      updateSubstitutionRelationship(apiClient, relationshipId, request),
    onSuccess: () => {
      invalidateSubstitutionQueries(queryClient, sourceId, relationshipId);
    },
  });
}

export function useDeleteSubstitutionMutation(sourceId: string) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (relationshipId: string) => deleteSubstitutionRelationship(apiClient, relationshipId),
    onSuccess: (_result, relationshipId) => {
      invalidateSubstitutionQueries(queryClient, sourceId, relationshipId);
    },
  });
}
