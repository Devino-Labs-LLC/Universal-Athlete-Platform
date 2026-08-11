import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { DateOnly, parseDateOnly } from '@/src/core/date/dateOnly';
import {
  buildCreateRequestFromForm,
  buildUpdateRequestFromForm,
  createRecoveryCheckIn,
  updateRecoveryCheckIn,
} from '@/src/features/recovery/api/checkInApi';
import { CreateCheckInFormValues } from '@/src/features/recovery/models/recoverySchemas';
import { invalidateAfterCheckInMutation } from '@/src/features/recovery/models/invalidation';

interface SaveCheckInInput {
  mode: 'create' | 'update';
  checkInId?: string;
  expectedVersion?: number;
  values: CreateCheckInFormValues;
}

export function useCheckInMutations(date: DateOnly) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  const saveMutation = useMutation({
    mutationFn: async (input: SaveCheckInInput) => {
      if (input.mode === 'create') {
        return createRecoveryCheckIn(
          apiClient,
          buildCreateRequestFromForm(input.values),
        );
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
    onSuccess: async (result) => {
      await invalidateAfterCheckInMutation(
        queryClient,
        parseDateOnly(result.checkInDate),
        result.id,
      );
    },
  });

  return { saveMutation };
}
