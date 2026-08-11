import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { DateOnly } from '@/src/core/date/dateOnly';
import { fetchRecoveryCheckInByDate } from '@/src/features/recovery/api/checkInApi';
import { recoveryKeys } from '@/src/features/recovery/models/recoveryKeys';
import { isNotFoundError } from '@/src/features/recovery/utils/recoveryErrors';

export function useCheckInByDate(date: DateOnly, enabled = true) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: recoveryKeys.checkInByDate(date),
    queryFn: () => fetchRecoveryCheckInByDate(apiClient, date),
    enabled: status === 'AUTHENTICATED' && enabled,
    staleTime: 30_000,
    retry: (failureCount, error) => {
      if (isNotFoundError(error)) {
        return false;
      }
      return failureCount < 1;
    },
  });
}
