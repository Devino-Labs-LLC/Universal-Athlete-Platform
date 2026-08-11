import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import type { DateOnly } from '@/core/date/dateOnly';
import {
  fetchAthleteStateComparison,
  fetchAthleteStateForDate,
  fetchAthleteStateHistory,
  fetchAthleteStateSnapshot,
  fetchAthleteStateVersions,
} from '@/features/recovery/api/athleteStateApi';
import { recoveryKeys } from '@/features/recovery/models/queryKeys';

export function useAthleteStateForDate(date: DateOnly | undefined) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.athleteStateForDate(date ?? ('' as DateOnly)),
    queryFn: () => fetchAthleteStateForDate(apiClient, date as DateOnly),
    enabled: status === 'AUTHENTICATED' && Boolean(date),
  });
}

export function useAthleteStateSnapshot(snapshotId: string | undefined) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.athleteStateSnapshot(snapshotId ?? ''),
    queryFn: () => fetchAthleteStateSnapshot(apiClient, snapshotId as string),
    enabled: status === 'AUTHENTICATED' && Boolean(snapshotId),
  });
}

export function useAthleteStateVersions(date: DateOnly | undefined) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.athleteStateVersions(date ?? ('' as DateOnly)),
    queryFn: () => fetchAthleteStateVersions(apiClient, date as DateOnly),
    enabled: status === 'AUTHENTICATED' && Boolean(date),
  });
}

export function useAthleteStateHistory(startDate: DateOnly, endDate: DateOnly) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.athleteStateHistory(startDate, endDate),
    queryFn: () => fetchAthleteStateHistory(apiClient, startDate, endDate),
    enabled: status === 'AUTHENTICATED' && Boolean(startDate) && Boolean(endDate),
  });
}

export function useAthleteStateComparison(olderSnapshotId: string | undefined, newerSnapshotId: string | undefined) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.athleteStateCompare(olderSnapshotId ?? '', newerSnapshotId ?? ''),
    queryFn: () => fetchAthleteStateComparison(apiClient, olderSnapshotId as string, newerSnapshotId as string),
    enabled: status === 'AUTHENTICATED' && Boolean(olderSnapshotId) && Boolean(newerSnapshotId),
  });
}
