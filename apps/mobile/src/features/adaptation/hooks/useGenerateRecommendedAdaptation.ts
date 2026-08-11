import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { generateRecommendedAdaptationProposal } from '@/src/features/adaptation/api/proposalApi';
import { GenerateAdaptationProposalRequest } from '@/src/features/adaptation/models/adaptationSchemas';
import { invalidateAfterProposalMutation } from '@/src/features/adaptation/models/invalidation';

interface GenerateRecommendedInput {
  recommendationId: string;
  occurrenceId: string;
  body?: GenerateAdaptationProposalRequest;
}

export function useGenerateRecommendedAdaptation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: GenerateRecommendedInput) =>
      generateRecommendedAdaptationProposal(
        apiClient,
        input.recommendationId,
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
