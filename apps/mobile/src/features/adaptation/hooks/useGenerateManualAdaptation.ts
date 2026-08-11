import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { generateManualAdaptationProposal } from '@/src/features/adaptation/api/proposalApi';
import { GenerateAdaptationProposalRequest } from '@/src/features/adaptation/models/adaptationSchemas';
import { invalidateAfterProposalMutation } from '@/src/features/adaptation/models/invalidation';

interface GenerateManualInput {
  planId: string;
  dayId: string;
  occurrenceId: string;
  body?: GenerateAdaptationProposalRequest;
}

export function useGenerateManualAdaptation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: GenerateManualInput) =>
      generateManualAdaptationProposal(
        apiClient,
        input.planId,
        input.dayId,
        input.occurrenceId,
        input.body,
      ),
    onSuccess: async (proposal) => {
      await invalidateAfterProposalMutation(queryClient, {
        planId: proposal.trainingPlanId,
        dayId: proposal.workoutDayId,
        occurrenceId: proposal.workoutOccurrenceId,
        proposalId: proposal.id,
      });
    },
  });
}
