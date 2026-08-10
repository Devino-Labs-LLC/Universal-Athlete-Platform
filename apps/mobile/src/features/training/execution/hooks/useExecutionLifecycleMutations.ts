import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/providers/AuthSessionProvider';
import {
  completeExerciseExecution,
  skipExerciseExecution,
} from '@/src/features/training/execution/api/executionLifecycleApi';
import {
  ExecutionScope,
  invalidateExecutionTerminal,
} from '@/src/features/training/execution/models/invalidation';

export function useExecutionLifecycleMutations(scope: ExecutionScope) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();
  const { planId, dayId, occurrenceId, executionId } = scope;

  const completeMutation = useMutation({
    mutationFn: () =>
      completeExerciseExecution(apiClient, planId, dayId, occurrenceId, executionId),
    onSuccess: async () => {
      await invalidateExecutionTerminal(queryClient, scope);
    },
  });

  const skipMutation = useMutation({
    mutationFn: () => skipExerciseExecution(apiClient, planId, dayId, occurrenceId, executionId),
    onSuccess: async () => {
      await invalidateExecutionTerminal(queryClient, scope);
    },
  });

  return { completeMutation, skipMutation };
}
