import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  createWorkoutExercise,
  deleteWorkoutExercise,
  fetchDayExercises,
  reorderWorkoutExercises,
  updateWorkoutExercise,
} from '@/features/training/api/exercisesApi';
import { invalidateExerciseQueries } from '@/features/training/models/invalidation';
import { trainingKeys } from '@/features/training/models/queryKeys';
import type {
  CreateWorkoutExerciseRequest,
  UpdateWorkoutExerciseRequest,
} from '@/features/training/models/schemas';

export function useDayExercises(planId: string, dayId: string | null) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: trainingKeys.exercises(planId, dayId ?? ''),
    queryFn: () => fetchDayExercises(apiClient, planId, dayId!),
    enabled: status === 'AUTHENTICATED' && Boolean(planId) && Boolean(dayId),
  });
}

export function useExerciseMutations(planId: string, dayId: string) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  const create = useMutation({
    mutationFn: (request: CreateWorkoutExerciseRequest) =>
      createWorkoutExercise(apiClient, planId, dayId, request),
    onSuccess: () => invalidateExerciseQueries(queryClient, planId, dayId),
  });

  const update = useMutation({
    mutationFn: ({
      exerciseId,
      request,
    }: {
      exerciseId: string;
      request: UpdateWorkoutExerciseRequest;
    }) => updateWorkoutExercise(apiClient, planId, dayId, exerciseId, request),
    onSuccess: () => invalidateExerciseQueries(queryClient, planId, dayId),
  });

  const reorder = useMutation({
    mutationFn: (exerciseIds: string[]) =>
      reorderWorkoutExercises(apiClient, planId, dayId, exerciseIds),
    onSuccess: (exercises) => {
      queryClient.setQueryData(trainingKeys.exercises(planId, dayId), exercises);
    },
  });

  const remove = useMutation({
    mutationFn: (exerciseId: string) => deleteWorkoutExercise(apiClient, planId, dayId, exerciseId),
    onSuccess: () => invalidateExerciseQueries(queryClient, planId, dayId),
  });

  return { create, update, reorder, remove };
}
