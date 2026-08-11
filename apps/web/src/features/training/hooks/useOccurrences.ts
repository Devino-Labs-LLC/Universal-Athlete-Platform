import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  createOccurrence,
  deleteOccurrence,
  fetchOccurrenceDetail,
  fetchOccurrences,
  rescheduleOccurrence,
} from '@/features/training/api/occurrencesApi';
import { invalidateOccurrenceQueries } from '@/features/training/models/invalidation';
import { trainingKeys } from '@/features/training/models/queryKeys';
import type {
  CreateOccurrenceRequest,
  RescheduleOccurrenceRequest,
} from '@/features/training/models/schemas';

export function useOccurrences(planId: string, dayId: string) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: trainingKeys.occurrences(planId, dayId),
    queryFn: () => fetchOccurrences(apiClient, planId, dayId),
    enabled: status === 'AUTHENTICATED' && Boolean(planId) && Boolean(dayId),
  });
}

export function useOccurrenceDetail(planId: string, dayId: string, occurrenceId: string) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: trainingKeys.occurrence(planId, dayId, occurrenceId),
    queryFn: () => fetchOccurrenceDetail(apiClient, planId, dayId, occurrenceId),
    enabled:
      status === 'AUTHENTICATED' &&
      Boolean(planId) &&
      Boolean(dayId) &&
      Boolean(occurrenceId),
  });
}

export function useOccurrenceMutations(planId: string, dayId: string) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  const create = useMutation({
    mutationFn: (request: CreateOccurrenceRequest) =>
      createOccurrence(apiClient, planId, dayId, request),
    onSuccess: () => invalidateOccurrenceQueries(queryClient, planId, dayId),
  });

  const reschedule = useMutation({
    mutationFn: ({
      occurrenceId,
      request,
    }: {
      occurrenceId: string;
      request: RescheduleOccurrenceRequest;
    }) => rescheduleOccurrence(apiClient, planId, dayId, occurrenceId, request),
    onSuccess: (_, { occurrenceId }) =>
      invalidateOccurrenceQueries(queryClient, planId, dayId, occurrenceId),
  });

  const remove = useMutation({
    mutationFn: (occurrenceId: string) =>
      deleteOccurrence(apiClient, planId, dayId, occurrenceId),
    onSuccess: (_, occurrenceId) =>
      invalidateOccurrenceQueries(queryClient, planId, dayId, occurrenceId),
  });

  return { create, reschedule, remove };
}
