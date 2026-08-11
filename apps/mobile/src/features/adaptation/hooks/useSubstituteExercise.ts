import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { substituteExercise } from '@/src/features/adaptation/api/substitutionApi';
import { SubstituteExerciseRequest } from '@/src/features/adaptation/models/adaptationSchemas';
import { invalidateAfterDirectSubstitution } from '@/src/features/adaptation/models/invalidation';

interface SubstituteInput {
  planId: string;
  dayId: string;
  occurrenceId: string;
  executionId: string;
  body: SubstituteExerciseRequest;
  activeProposalId?: string;
}

export function useSubstituteExercise() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: SubstituteInput) =>
      substituteExercise(
        apiClient,
        input.planId,
        input.dayId,
        input.occurrenceId,
        input.executionId,
        input.body,
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
