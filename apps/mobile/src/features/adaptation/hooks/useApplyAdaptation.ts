import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { applyAdaptationProposal } from '@/src/features/adaptation/api/proposalApi';
import { invalidateAfterApplyAdaptation } from '@/src/features/adaptation/models/invalidation';

interface ApplyInput {
  planId: string;
  dayId: string;
  occurrenceId: string;
  proposalId: string;
  expectedProposalVersion: number;
}

export function useApplyAdaptation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: ApplyInput) =>
      applyAdaptationProposal(
        apiClient,
        input.planId,
        input.dayId,
        input.occurrenceId,
        input.proposalId,
        { expectedProposalVersion: input.expectedProposalVersion },
      ),
    onSuccess: async (_result, input) => {
      await invalidateAfterApplyAdaptation(queryClient, input);
    },
  });
}
