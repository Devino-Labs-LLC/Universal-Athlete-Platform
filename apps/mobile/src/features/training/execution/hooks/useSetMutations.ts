import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/providers/AuthSessionProvider';
import {
  addExerciseSet,
  completeExerciseSet,
  deleteExerciseSet,
  patchExerciseSet,
  skipExerciseSet,
} from '@/src/features/training/execution/api/setApi';
import { PatchWorkoutExerciseSetRequest } from '@/src/features/training/execution/models/executionSchemas';
import {
  ExecutionScope,
  invalidateSetTerminal,
  invalidateSetValueUpdate,
} from '@/src/features/training/execution/models/invalidation';

export function useSetMutations(scope: ExecutionScope) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();
  const { planId, dayId, occurrenceId, executionId } = scope;

  const patchMutation = useMutation({
    mutationFn: ({ setId, request }: { setId: string; request: PatchWorkoutExerciseSetRequest }) =>
      patchExerciseSet(apiClient, planId, dayId, occurrenceId, executionId, setId, request),
    onSuccess: async () => {
      await invalidateSetValueUpdate(queryClient, scope);
    },
  });

  const completeMutation = useMutation({
    mutationFn: (setId: string) =>
      completeExerciseSet(apiClient, planId, dayId, occurrenceId, executionId, setId),
    onSuccess: async () => {
      await invalidateSetTerminal(queryClient, scope);
    },
  });

  const skipMutation = useMutation({
    mutationFn: (setId: string) =>
      skipExerciseSet(apiClient, planId, dayId, occurrenceId, executionId, setId),
    onSuccess: async () => {
      await invalidateSetTerminal(queryClient, scope);
    },
  });

  const addMutation = useMutation({
    mutationFn: () => addExerciseSet(apiClient, planId, dayId, occurrenceId, executionId),
    onSuccess: async () => {
      await invalidateSetValueUpdate(queryClient, scope);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (setId: string) =>
      deleteExerciseSet(apiClient, planId, dayId, occurrenceId, executionId, setId),
    onSuccess: async () => {
      await invalidateSetValueUpdate(queryClient, scope);
    },
  });

  const saveAndCompleteMutation = useMutation({
    mutationFn: async ({
      setId,
      request,
    }: {
      setId: string;
      request: PatchWorkoutExerciseSetRequest;
    }) => {
      await patchExerciseSet(apiClient, planId, dayId, occurrenceId, executionId, setId, request);
      return completeExerciseSet(apiClient, planId, dayId, occurrenceId, executionId, setId);
    },
    onSuccess: async () => {
      await invalidateSetTerminal(queryClient, scope);
    },
  });

  return {
    patchMutation,
    completeMutation,
    skipMutation,
    addMutation,
    deleteMutation,
    saveAndCompleteMutation,
  };
}
