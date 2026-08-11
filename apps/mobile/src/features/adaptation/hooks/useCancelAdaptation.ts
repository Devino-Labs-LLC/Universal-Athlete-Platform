import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { cancelAdaptationProposal } from '@/src/features/adaptation/api/proposalApi';
import { invalidateAfterProposalMutation } from '@/src/features/adaptation/models/invalidation';

interface CancelInput {
  proposalId: string;
  planId: string;
  dayId: string;
  occurrenceId: string;
}

export function useCancelAdaptation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: CancelInput) => cancelAdaptationProposal(apiClient, input.proposalId),
    onSuccess: async (proposal, input) => {
      await invalidateAfterProposalMutation(queryClient, {
        planId: input.planId,
        dayId: input.dayId,
        occurrenceId: input.occurrenceId,
        proposalId: proposal.id,
      });
    },
  });
}
