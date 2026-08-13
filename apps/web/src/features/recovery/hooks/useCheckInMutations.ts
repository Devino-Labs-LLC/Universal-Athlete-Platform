import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  buildCreateRequestFromForm,
  buildUpdateRequestFromForm,
  createRecoveryCheckIn,
  updateRecoveryCheckIn,
} from '@/features/recovery/api/checkInsApi';
import type { CreateCheckInFormValues } from '@/features/recovery/models/checkInForm';
import { invalidateAfterCheckInMutation } from '@/features/recovery/models/invalidation';

export interface SaveCheckInInput {
  mode: 'create' | 'update';
  checkInId?: string;
  expectedVersion?: number;
  values: CreateCheckInFormValues;
}

export function useCheckInMutations() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  const saveMutation = useMutation({
    mutationFn: async (input: SaveCheckInInput) => {
      if (input.mode === 'create') {
        return createRecoveryCheckIn(apiClient, buildCreateRequestFromForm(input.values));
      }
      if (!input.checkInId || input.expectedVersion == null) {
        throw new Error('Missing check-in id or version for update');
      }
      return updateRecoveryCheckIn(
        apiClient,
        input.checkInId,
        buildUpdateRequestFromForm(input.values, input.expectedVersion),
      );
    },
    onSuccess: (result) => {
      invalidateAfterCheckInMutation(queryClient, result.id);
    },
  });

  return { saveMutation };
}
