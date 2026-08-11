import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { regenerateAdaptationProposal } from '@/src/features/adaptation/api/proposalApi';
import { GenerateAdaptationProposalRequest } from '@/src/features/adaptation/models/adaptationSchemas';
import { invalidateAfterProposalMutation } from '@/src/features/adaptation/models/invalidation';

interface RegenerateInput {
  proposalId: string;
  planId: string;
  dayId: string;
  occurrenceId: string;
  body?: GenerateAdaptationProposalRequest;
}

export function useRegenerateAdaptation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: RegenerateInput) =>
      regenerateAdaptationProposal(apiClient, input.proposalId, input.body),
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
