import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  createAthleteGoal,
  fetchAthleteGoals,
  removeAthleteGoal,
} from '@/features/profile/api/athleteApi';
import { athleteQueryKeys } from '@/features/profile/queryKeys';
import type { AthleteGoal, CreateAthleteGoalRequest } from '@/features/profile/schemas';

export function useAthleteGoalsQuery(enabled = true) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: athleteQueryKeys.goals(),
    queryFn: () => fetchAthleteGoals(apiClient),
    enabled: enabled && status === 'AUTHENTICATED',
  });
}

export function useCreateAthleteGoalMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateAthleteGoalRequest) => createAthleteGoal(apiClient, request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: athleteQueryKeys.goals() });
    },
  });
}

export function useRemoveAthleteGoalMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (goal: AthleteGoal) => removeAthleteGoal(apiClient, goal),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: athleteQueryKeys.goals() });
    },
  });
}
