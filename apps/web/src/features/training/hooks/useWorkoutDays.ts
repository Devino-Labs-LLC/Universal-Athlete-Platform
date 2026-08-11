import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  changeDayStatus,
  createWorkoutDay,
  deleteWorkoutDay,
  fetchWorkoutDay,
  fetchWorkoutDays,
  reorderWorkoutDays,
  updateWorkoutDay,
} from '@/features/training/api/daysApi';
import { invalidateDayQueries } from '@/features/training/models/invalidation';
import { trainingKeys } from '@/features/training/models/queryKeys';
import type { CreateWorkoutDayRequest, UpdateWorkoutDayRequest } from '@/features/training/models/schemas';

export function useWorkoutDays(planId: string) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: trainingKeys.days(planId),
    queryFn: () => fetchWorkoutDays(apiClient, planId),
    enabled: status === 'AUTHENTICATED' && Boolean(planId),
  });
}

export function useWorkoutDay(planId: string, dayId: string) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: trainingKeys.day(planId, dayId),
    queryFn: () => fetchWorkoutDay(apiClient, planId, dayId),
    enabled: status === 'AUTHENTICATED' && Boolean(planId) && Boolean(dayId),
  });
}

export function useDayMutations(planId: string) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  const create = useMutation({
    mutationFn: (request: CreateWorkoutDayRequest) => createWorkoutDay(apiClient, planId, request),
    onSuccess: () => invalidateDayQueries(queryClient, planId),
  });

  const update = useMutation({
    mutationFn: ({ dayId, request }: { dayId: string; request: UpdateWorkoutDayRequest }) =>
      updateWorkoutDay(apiClient, planId, dayId, request),
    onSuccess: (_, { dayId }) => invalidateDayQueries(queryClient, planId, dayId),
  });

  const reorder = useMutation({
    mutationFn: (dayIds: string[]) => reorderWorkoutDays(apiClient, planId, dayIds),
    onSuccess: (days) => {
      queryClient.setQueryData(trainingKeys.days(planId), days);
    },
  });

  const remove = useMutation({
    mutationFn: (dayId: string) => deleteWorkoutDay(apiClient, planId, dayId),
    onSuccess: () => invalidateDayQueries(queryClient, planId),
  });

  const changeStatus = useMutation({
    mutationFn: ({ dayId, action }: { dayId: string; action: 'ACTIVATE' | 'COMPLETE' | 'SKIP' }) =>
      changeDayStatus(apiClient, planId, dayId, action),
    onSuccess: (_, { dayId }) => invalidateDayQueries(queryClient, planId, dayId),
  });

  return { create, update, reorder, remove, changeStatus };
}
