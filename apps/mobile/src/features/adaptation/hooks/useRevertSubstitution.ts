import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { revertExerciseSubstitution } from '@/src/features/adaptation/api/substitutionApi';
import { invalidateAfterDirectSubstitution } from '@/src/features/adaptation/models/invalidation';

interface RevertInput {
  planId: string;
  dayId: string;
  occurrenceId: string;
  executionId: string;
  notes?: string;
  activeProposalId?: string;
}

export function useRevertSubstitution() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: RevertInput) =>
      revertExerciseSubstitution(
        apiClient,
        input.planId,
        input.dayId,
        input.occurrenceId,
        input.executionId,
        input.notes,
      ),
    onSuccess: async (_result, input) => {
      await invalidateAfterDirectSubstitution(queryClient, {
        planId: input.planId,
        dayId: input.dayId,
        occurrenceId: input.occurrenceId,
        executionId: input.executionId,
        proposalId: input.activeProposalId,
      });
    },
  });
}
