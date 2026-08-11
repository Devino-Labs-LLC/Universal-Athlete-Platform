import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  activateSchedule,
  completeSchedule,
  generateOccurrences,
  pauseSchedule,
  resumeSchedule,
} from '@/features/training/api/scheduleApi';
import { invalidateScheduleQueries } from '@/features/training/models/invalidation';
import { trainingKeys } from '@/features/training/models/queryKeys';
import type {
  ActivateScheduleRequest,
  GenerateOccurrencesRequest,
} from '@/features/training/models/schemas';

export function useScheduleMutations(planId: string) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  const onPlanUpdate = () => invalidateScheduleQueries(queryClient, planId);

  const activate = useMutation({
    mutationFn: (request: ActivateScheduleRequest) => activateSchedule(apiClient, planId, request),
    onSuccess: ({ plan }) => {
      queryClient.setQueryData(trainingKeys.plan(planId), plan);
      onPlanUpdate();
    },
  });

  const pause = useMutation({
    mutationFn: () => pauseSchedule(apiClient, planId),
    onSuccess: (plan) => {
      queryClient.setQueryData(trainingKeys.plan(planId), plan);
      onPlanUpdate();
    },
  });

  const resume = useMutation({
    mutationFn: () => resumeSchedule(apiClient, planId),
    onSuccess: (plan) => {
      queryClient.setQueryData(trainingKeys.plan(planId), plan);
      onPlanUpdate();
    },
  });

  const complete = useMutation({
    mutationFn: () => completeSchedule(apiClient, planId),
    onSuccess: (plan) => {
      queryClient.setQueryData(trainingKeys.plan(planId), plan);
      onPlanUpdate();
    },
  });

  const generate = useMutation({
    mutationFn: (request: GenerateOccurrencesRequest) =>
      generateOccurrences(apiClient, planId, request),
    onSuccess: () => {
      onPlanUpdate();
      void queryClient.invalidateQueries({ queryKey: trainingKeys.all });
    },
  });

  return { activate, pause, resume, complete, generate };
}
