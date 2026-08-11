import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { parseDateOnly } from '@/core/date/dateOnly';
import {
  generateAthleteStateSnapshot,
  generateReadinessAssessment,
  generateTrainingRecommendation,
} from '@/features/home/api/derivedStateApi';
import { trainingClientKeys } from '@/features/home/queryKeys';

export function useDerivedStateMutations(date: string) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();
  const dateOnly = parseDateOnly(date);

  const invalidateToday = async () => {
    await queryClient.invalidateQueries({ queryKey: trainingClientKeys.today(dateOnly) });
  };

  const athleteStateMutation = useMutation({
    mutationFn: () => generateAthleteStateSnapshot(apiClient, dateOnly),
    onSuccess: invalidateToday,
  });

  const readinessMutation = useMutation({
    mutationFn: () => generateReadinessAssessment(apiClient, dateOnly),
    onSuccess: invalidateToday,
  });

  const recommendationMutation = useMutation({
    mutationFn: () => generateTrainingRecommendation(apiClient, dateOnly),
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
