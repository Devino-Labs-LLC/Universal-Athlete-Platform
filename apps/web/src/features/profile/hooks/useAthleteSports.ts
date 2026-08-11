import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  addAthleteSport,
  deleteAthleteSport,
  fetchAthleteSports,
  setPrimaryAthleteSport,
} from '@/features/profile/api/athleteApi';
import { athleteQueryKeys } from '@/features/profile/queryKeys';
import type { AddAthleteSportRequest } from '@/features/profile/schemas';

export function useAthleteSportsQuery(enabled = true) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: athleteQueryKeys.sports(),
    queryFn: () => fetchAthleteSports(apiClient),
    enabled: enabled && status === 'AUTHENTICATED',
  });
}

export function useAddAthleteSportMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: AddAthleteSportRequest) => addAthleteSport(apiClient, request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: athleteQueryKeys.sports() });
    },
  });
}

export function useSetPrimaryAthleteSportMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (sportId: string) => setPrimaryAthleteSport(apiClient, sportId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: athleteQueryKeys.sports() });
    },
  });
}

export function useDeleteAthleteSportMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (sportId: string) => deleteAthleteSport(apiClient, sportId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: athleteQueryKeys.sports() });
    },
  });
}
