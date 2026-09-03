import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { DateOnly } from '@/src/core/date/dateOnly';
import { todayQueryKeys } from '@/src/features/home/models/queryKeys';
import { createAndActivatePersonalPlan } from '@/src/features/training/api/personalPlanApi';

export function useCreatePersonalPlan() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: { name: string; startDate: DateOnly; timezone: string }) =>
      createAndActivatePersonalPlan(apiClient, input),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: todayQueryKeys.all }),
        queryClient.invalidateQueries({ queryKey: ['training', 'overview'] }),
        queryClient.invalidateQueries({ queryKey: ['training', 'calendar'] }),
      ]);
    },
  });
}
