import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  fetchExercisePersonalRecords,
  fetchPersonalRecords,
  fetchRecentPersonalRecords,
} from '@/features/performance/api/personalRecordsApi';
import { performanceKeys } from '@/features/performance/models/queryKeys';

export function useRecentPersonalRecords(days = 30, limit = 5) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: performanceKeys.recentRecords(days, limit),
    queryFn: () => fetchRecentPersonalRecords(apiClient, days, limit),
    enabled: status === 'AUTHENTICATED',
  });
}

export function usePersonalRecords(filters?: { exercisePerformanceKey?: string; recordType?: string }) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: performanceKeys.personalRecords(filters?.exercisePerformanceKey, filters?.recordType),
    queryFn: () => fetchPersonalRecords(apiClient, filters),
    enabled: status === 'AUTHENTICATED',
  });
}

export function useExercisePersonalRecords(exercisePerformanceKey: string | undefined) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: performanceKeys.exerciseRecords(exercisePerformanceKey ?? ''),
    queryFn: () => fetchExercisePersonalRecords(apiClient, exercisePerformanceKey as string),
    enabled: status === 'AUTHENTICATED' && Boolean(exercisePerformanceKey),
  });
}
