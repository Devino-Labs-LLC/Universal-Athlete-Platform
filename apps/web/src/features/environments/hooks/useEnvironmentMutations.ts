import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  archiveEnvironment,
  createEnvironment,
  setDefaultEnvironment,
  updateEnvironment,
} from '@/features/environments/api/environmentsApi';
import { invalidateEnvironmentQueries } from '@/features/environments/models/invalidation';
import { environmentKeys } from '@/features/environments/models/queryKeys';
import type { CreateEnvironmentRequest, UpdateEnvironmentRequest } from '@/features/environments/models/schemas';

export function useCreateEnvironmentMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateEnvironmentRequest) => createEnvironment(apiClient, request),
    onSuccess: (environment) => {
      queryClient.setQueryData(environmentKeys.detail(environment.id), environment);
      invalidateEnvironmentQueries(queryClient, environment.id);
    },
  });
}

export function useUpdateEnvironmentMutation(environmentId: string) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (patch: UpdateEnvironmentRequest) => updateEnvironment(apiClient, environmentId, patch),
    onSuccess: (environment) => {
      queryClient.setQueryData(environmentKeys.detail(environmentId), environment);
      invalidateEnvironmentQueries(queryClient, environmentId);
    },
  });
}

export function useArchiveEnvironmentMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (environmentId: string) => archiveEnvironment(apiClient, environmentId),
    onSuccess: (_result, environmentId) => {
      invalidateEnvironmentQueries(queryClient, environmentId);
    },
  });
}

export function useSetDefaultEnvironmentMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (environmentId: string) => setDefaultEnvironment(apiClient, environmentId),
    onSuccess: (environment) => {
      queryClient.setQueryData(environmentKeys.detail(environment.id), environment);
      invalidateEnvironmentQueries(queryClient, environment.id);
    },
  });
}
