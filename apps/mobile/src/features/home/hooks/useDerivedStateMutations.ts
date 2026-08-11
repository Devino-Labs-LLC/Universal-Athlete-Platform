import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { parseDateOnly } from '@/src/core/date/dateOnly';
import {
  generateAthleteStateSnapshot,
  generateReadinessAssessment,
  generateTrainingRecommendation,
  regenerateAthleteStateSnapshot,
} from '@/src/features/home/api/derivedStateApi';
import { invalidateAfterDerivedStateMutation } from '@/src/features/recovery/models/invalidation';

export function useDerivedStateMutations(date: string) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();
  const dateOnly = parseDateOnly(date);

  const invalidateDerived = async () => {
    await invalidateAfterDerivedStateMutation(queryClient);
  };

  const athleteStateMutation = useMutation({
    mutationFn: () => generateAthleteStateSnapshot(apiClient, dateOnly),
    onSuccess: invalidateDerived,
  });

  const regenerateAthleteStateMutation = useMutation({
    mutationFn: () => regenerateAthleteStateSnapshot(apiClient, dateOnly),
    onSuccess: invalidateDerived,
  });

  const readinessMutation = useMutation({
    mutationFn: () => generateReadinessAssessment(apiClient, dateOnly),
    onSuccess: invalidateDerived,
  });

  const recommendationMutation = useMutation({
    mutationFn: () => generateTrainingRecommendation(apiClient, dateOnly),
    onSuccess: invalidateDerived,
  });

  const mutationError =
    athleteStateMutation.error ??
    regenerateAthleteStateMutation.error ??
    readinessMutation.error ??
    recommendationMutation.error;

  const errorMessage =
    mutationError instanceof Error ? mutationError.message : null;

  return {
    athleteStateMutation,
    regenerateAthleteStateMutation,
    readinessMutation,
    recommendationMutation,
    errorMessage,
  };
}
