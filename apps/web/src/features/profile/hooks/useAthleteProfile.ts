import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  createAthleteProfile,
  fetchAthleteProfile,
  updateAthleteProfile,
} from '@/features/profile/api/athleteApi';
import { athleteQueryKeys } from '@/features/profile/queryKeys';
import type {
  CreateAthleteProfileRequest,
  UpdateAthleteProfileRequest,
} from '@/features/profile/schemas';

export function useAthleteProfile() {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: athleteQueryKeys.profile(),
    queryFn: () => fetchAthleteProfile(apiClient),
    enabled: status === 'AUTHENTICATED',
  });
}

export function useCreateAthleteProfileMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateAthleteProfileRequest) =>
      createAthleteProfile(apiClient, request),
    onSuccess: (profile) => {
      queryClient.setQueryData(athleteQueryKeys.profile(), profile);
    },
  });
}

export function useUpdateAthleteProfileMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: UpdateAthleteProfileRequest) =>
      updateAthleteProfile(apiClient, request),
    onSuccess: (profile) => {
      queryClient.setQueryData(athleteQueryKeys.profile(), profile);
    },
  });
}
