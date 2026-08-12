import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { type DateOnly, parseDateOnly } from '@/core/date/dateOnly';
import {
  generateAthleteStateSnapshot,
  generateReadinessAssessment,
  generateTrainingRecommendation,
} from '@/features/home/api/derivedStateApi';
import { trainingClientKeys } from '@/features/home/queryKeys';

function tryParseDateOnly(date: string | null | undefined): DateOnly | null {
  if (!date) {
    return null;
  }
  try {
    return parseDateOnly(date);
  } catch {
    return null;
  }
}

/**
 * Mutation helpers for Home quick actions.
 * `date` may be unavailable while Today is still loading — must not throw during render.
 */
export function useDerivedStateMutations(date?: string | null) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();
  const dateOnly = tryParseDateOnly(date);

  const requireDate = (): DateOnly => {
    if (!dateOnly) {
      throw new Error('Today date is not available yet');
    }
    return dateOnly;
  };

  const invalidateToday = async () => {
    if (!dateOnly) {
      return;
    }
    await queryClient.invalidateQueries({ queryKey: trainingClientKeys.today(dateOnly) });
  };

  const athleteStateMutation = useMutation({
    mutationFn: () => generateAthleteStateSnapshot(apiClient, requireDate()),
    onSuccess: invalidateToday,
  });

  const readinessMutation = useMutation({
    mutationFn: () => generateReadinessAssessment(apiClient, requireDate()),
    onSuccess: invalidateToday,
  });

  const recommendationMutation = useMutation({
    mutationFn: () => generateTrainingRecommendation(apiClient, requireDate()),
    onSuccess: invalidateToday,
  });

  const mutationError =
    athleteStateMutation.error ?? readinessMutation.error ?? recommendationMutation.error;

  const errorMessage = mutationError instanceof Error ? mutationError.message : null;

  return {
    athleteStateMutation,
    readinessMutation,
    recommendationMutation,
    errorMessage,
  };
}
