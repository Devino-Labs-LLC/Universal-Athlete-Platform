import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import {
  clearOccurrenceEnvironment,
  setOccurrenceEnvironment,
} from '@/src/features/environments/api/occurrenceEnvironmentApi';
import { invalidateAfterOccurrenceEnvironmentMutation } from '@/src/features/environments/models/invalidation';

interface OccurrenceScope {
  planId: string;
  dayId: string;
  occurrenceId: string;
}

export function useOccurrenceEnvironmentMutations(scope: OccurrenceScope) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  const setMutation = useMutation({
    mutationFn: (trainingEnvironmentId: string) =>
      setOccurrenceEnvironment(
        apiClient,
        scope.planId,
        scope.dayId,
        scope.occurrenceId,
        { trainingEnvironmentId },
      ),
    onSuccess: async () => {
      await invalidateAfterOccurrenceEnvironmentMutation(queryClient, scope);
    },
  });

  const clearMutation = useMutation({
    mutationFn: () =>
      clearOccurrenceEnvironment(apiClient, scope.planId, scope.dayId, scope.occurrenceId),
    onSuccess: async () => {
      await invalidateAfterOccurrenceEnvironmentMutation(queryClient, scope);
    },
  });

  return { setMutation, clearMutation };
}
