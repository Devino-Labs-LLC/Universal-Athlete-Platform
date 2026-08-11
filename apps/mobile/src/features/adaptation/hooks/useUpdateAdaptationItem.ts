import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { updateAdaptationProposalItem } from '@/src/features/adaptation/api/proposalApi';
import { UpdateAdaptationItemRequest } from '@/src/features/adaptation/models/adaptationSchemas';
import { invalidateAfterProposalMutation } from '@/src/features/adaptation/models/invalidation';

interface UpdateItemInput {
  proposalId: string;
  itemId: string;
  planId: string;
  dayId: string;
  occurrenceId: string;
  body: UpdateAdaptationItemRequest;
}

export function useUpdateAdaptationItem() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: UpdateItemInput) =>
      updateAdaptationProposalItem(apiClient, input.proposalId, input.itemId, input.body),
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
