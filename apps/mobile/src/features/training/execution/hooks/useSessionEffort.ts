import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/providers/AuthSessionProvider';
import {
  fetchSessionEffort,
  submitSessionEffort,
  updateSessionEffort,
} from '@/src/features/training/execution/api/sessionEffortApi';
import { SessionEffortRequest } from '@/src/features/training/execution/models/executionSchemas';
import { invalidatePerformanceQueries } from '@/src/features/performance/models/invalidation';
import { OccurrenceScope } from '@/src/features/training/execution/models/invalidation';
import { trainingKeys } from '@/src/features/training/models/queryKeys';

export function useSessionEffort(scope: OccurrenceScope, enabled = true) {
  const { apiClient, status } = useAuthSession();
  const queryClient = useQueryClient();
  const { planId, dayId, occurrenceId } = scope;

  const effortQuery = useQuery({
    queryKey: trainingKeys.sessionEffort(planId, dayId, occurrenceId),
    queryFn: () => fetchSessionEffort(apiClient, planId, dayId, occurrenceId),
    enabled:
      enabled && status === 'AUTHENTICATED' && Boolean(planId) && Boolean(dayId) && Boolean(occurrenceId),
    staleTime: 30_000,
    retry: 1,
  });

  const submitMutation = useMutation({
    mutationFn: (request: SessionEffortRequest) =>
      submitSessionEffort(apiClient, planId, dayId, occurrenceId, request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: trainingKeys.sessionEffort(planId, dayId, occurrenceId),
      });
      await queryClient.invalidateQueries({
        queryKey: trainingKeys.trainingLoad(planId, dayId, occurrenceId),
      });
      await invalidatePerformanceQueries(queryClient);
    },
  });

  const updateMutation = useMutation({
    mutationFn: (request: SessionEffortRequest) =>
      updateSessionEffort(apiClient, planId, dayId, occurrenceId, request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: trainingKeys.sessionEffort(planId, dayId, occurrenceId),
      });
      await queryClient.invalidateQueries({
        queryKey: trainingKeys.trainingLoad(planId, dayId, occurrenceId),
      });
      await invalidatePerformanceQueries(queryClient);
    },
  });

  return { effortQuery, submitMutation, updateMutation };
}
