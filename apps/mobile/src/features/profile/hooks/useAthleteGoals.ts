import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import {
  createAthleteGoal,
  deleteAthleteGoal,
  fetchAthleteGoals,
} from '@/src/features/profile/api/athleteApi';
import { athleteQueryKeys } from '@/src/features/profile/queryKeys';
import { CreateAthleteGoalRequest } from '@/src/features/profile/schemas';

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

export function useDeleteAthleteGoalMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (goalId: string) => deleteAthleteGoal(apiClient, goalId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: athleteQueryKeys.goals() });
    },
  });
}
