import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { DateOnly } from '@/src/core/date/dateOnly';
import { fetchRecoveryHistory } from '@/src/features/recovery/api/recoveryHistoryApi';
import { recoveryKeys } from '@/src/features/recovery/models/recoveryKeys';

export function useRecoveryHistory(
  startDate: DateOnly,
  endDate: DateOnly,
  includeTrainingLoad = true,
) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: recoveryKeys.history(startDate, endDate, includeTrainingLoad),
    queryFn: () =>
      fetchRecoveryHistory(apiClient, startDate, endDate, includeTrainingLoad),
    enabled: status === 'AUTHENTICATED',
    staleTime: 30_000,
    retry: 1,
  });
}
