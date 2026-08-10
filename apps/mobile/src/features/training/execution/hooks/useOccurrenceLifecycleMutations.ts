import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/providers/AuthSessionProvider';
import {
  completeWorkoutOccurrence,
  skipWorkoutOccurrence,
  startWorkoutOccurrence,
} from '@/src/features/training/execution/api/occurrenceLifecycleApi';
import {
  invalidateOccurrenceTerminal,
  invalidateStartWorkout,
  OccurrenceScope,
} from '@/src/features/training/execution/models/invalidation';

export function useOccurrenceLifecycleMutations(scope: OccurrenceScope) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();
  const { planId, dayId, occurrenceId } = scope;

  const startMutation = useMutation({
    mutationFn: () => startWorkoutOccurrence(apiClient, planId, dayId, occurrenceId),
    onSuccess: async () => {
      await invalidateStartWorkout(queryClient, scope);
    },
  });

  const completeMutation = useMutation({
    mutationFn: () => completeWorkoutOccurrence(apiClient, planId, dayId, occurrenceId),
    onSuccess: async () => {
      await invalidateOccurrenceTerminal(queryClient, scope);
    },
  });

  const skipMutation = useMutation({
    mutationFn: () => skipWorkoutOccurrence(apiClient, planId, dayId, occurrenceId),
    onSuccess: async () => {
      await invalidateOccurrenceTerminal(queryClient, scope);
    },
  });

  return { startMutation, completeMutation, skipMutation };
}
