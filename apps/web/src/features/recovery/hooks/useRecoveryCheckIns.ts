import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import type { DateOnly } from '@/core/date/dateOnly';
import {
  fetchRecoveryCheckInById,
  fetchRecoveryCheckInByDate,
  fetchRecoveryCheckInList,
  fetchRecoveryCheckInRevisions,
  fetchRecoveryHistory,
} from '@/features/recovery/api/checkInsApi';
import { isNotFoundError } from '@/features/recovery/models/errors';
import type { RecoveryCheckInListFilters } from '@/features/recovery/models/queryKeys';
import { recoveryKeys } from '@/features/recovery/models/queryKeys';

export function useRecoveryCheckIn(checkInId: string | undefined) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.checkIn(checkInId ?? ''),
    queryFn: () => fetchRecoveryCheckInById(apiClient, checkInId as string),
    enabled: status === 'AUTHENTICATED' && Boolean(checkInId),
  });
}

export function useRecoveryCheckInByDate(date: DateOnly | undefined) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.checkInByDate(date ?? ('' as DateOnly)),
    queryFn: () => fetchRecoveryCheckInByDate(apiClient, date as DateOnly),
    enabled: status === 'AUTHENTICATED' && Boolean(date),
    retry: (failureCount, error) => {
      if (isNotFoundError(error)) {
        return false;
      }
      return failureCount < 1;
    },
  });
}

export function useRecoveryCheckInRevisions(checkInId: string | undefined) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.checkInRevisions(checkInId ?? ''),
    queryFn: () => fetchRecoveryCheckInRevisions(apiClient, checkInId as string),
    enabled: status === 'AUTHENTICATED' && Boolean(checkInId),
  });
}

export function useRecoveryCheckInList(
  startDate: DateOnly,
  endDate: DateOnly,
  filters?: RecoveryCheckInListFilters,
) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.checkInList(startDate, endDate, filters),
    queryFn: () => fetchRecoveryCheckInList(apiClient, startDate, endDate, filters),
    enabled: status === 'AUTHENTICATED' && Boolean(startDate) && Boolean(endDate),
  });
}

export function useRecoveryHistory(startDate: DateOnly, endDate: DateOnly, includeTrainingLoad = true) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.history(startDate, endDate, { includeTrainingLoad }),
    queryFn: () => fetchRecoveryHistory(apiClient, startDate, endDate, includeTrainingLoad),
    enabled: status === 'AUTHENTICATED' && Boolean(startDate) && Boolean(endDate),
  });
}
