import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { type DateOnly, parseDateOnly } from '@/src/core/date/dateOnly';
import {
  generateAthleteStateSnapshot,
  generateReadinessAssessment,
  generateTrainingRecommendation,
  regenerateAthleteStateSnapshot,
} from '@/src/features/home/api/derivedStateApi';
import { invalidateAfterDerivedStateMutation } from '@/src/features/recovery/models/invalidation';

/**
 * Today date is absent while the dashboard query is still loading.
 * Do not call parseDateOnly('') / throw during render — only parse a real value.
 * parseDateOnly itself remains strict; missing/empty is skipped here only.
 */
function tryParseDateOnly(date: string | null | undefined): DateOnly | null {
  if (date == null || date === '') {
    return null;
  }
  try {
    return parseDateOnly(date);
  } catch {
    // Non-empty but invalid: do not crash Home render; mutations refuse via requireDate.
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

  const invalidateDerived = async () => {
    await invalidateAfterDerivedStateMutation(queryClient);
  };

  const athleteStateMutation = useMutation({
    mutationFn: () => generateAthleteStateSnapshot(apiClient, requireDate()),
    onSuccess: invalidateDerived,
  });

  const regenerateAthleteStateMutation = useMutation({
    mutationFn: () => regenerateAthleteStateSnapshot(apiClient, requireDate()),
    onSuccess: invalidateDerived,
  });

  const readinessMutation = useMutation({
    mutationFn: () => generateReadinessAssessment(apiClient, requireDate()),
    onSuccess: invalidateDerived,
  });

  const recommendationMutation = useMutation({
    mutationFn: () => generateTrainingRecommendation(apiClient, requireDate()),
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
