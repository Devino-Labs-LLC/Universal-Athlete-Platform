import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import {
  archiveTrainingEnvironment,
  buildCreateRequestFromForm,
  buildUpdateRequestFromForm,
  createTrainingEnvironment,
  setDefaultTrainingEnvironment,
  updateTrainingEnvironment,
} from '@/src/features/environments/api/environmentsApi';
import { TrainingEnvironmentFormValues } from '@/src/features/environments/models/environmentSchemas';
import { invalidateAfterEnvironmentMutation } from '@/src/features/environments/models/invalidation';

export function useEnvironmentMutations() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: (values: TrainingEnvironmentFormValues) =>
      createTrainingEnvironment(apiClient, buildCreateRequestFromForm(values)),
    onSuccess: async (result) => {
      await invalidateAfterEnvironmentMutation(queryClient, result.id);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({
      environmentId,
      values,
    }: {
      environmentId: string;
      values: TrainingEnvironmentFormValues;
    }) => updateTrainingEnvironment(apiClient, environmentId, buildUpdateRequestFromForm(values)),
    onSuccess: async (result) => {
      await invalidateAfterEnvironmentMutation(queryClient, result.id);
    },
  });

  const archiveMutation = useMutation({
    mutationFn: (environmentId: string) => archiveTrainingEnvironment(apiClient, environmentId),
    onSuccess: async (_result, environmentId) => {
      await invalidateAfterEnvironmentMutation(queryClient, environmentId);
    },
  });

  const setDefaultMutation = useMutation({
    mutationFn: (environmentId: string) => setDefaultTrainingEnvironment(apiClient, environmentId),
    onSuccess: async (result) => {
      await invalidateAfterEnvironmentMutation(queryClient, result.id);
    },
  });

  return {
    createMutation,
    updateMutation,
    archiveMutation,
    setDefaultMutation,
  };
}
