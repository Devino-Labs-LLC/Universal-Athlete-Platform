import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/providers/AuthSessionProvider';
import { fetchExercisePersonalRecords } from '@/src/features/performance/api/personalRecordsApi';
import { performanceKeys } from '@/src/features/performance/models/performanceKeys';

export function useExercisePersonalRecords(exercisePerformanceKey: string) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: performanceKeys.exerciseRecords(exercisePerformanceKey),
    queryFn: () => fetchExercisePersonalRecords(apiClient, exercisePerformanceKey),
    enabled: status === 'AUTHENTICATED' && Boolean(exercisePerformanceKey),
    staleTime: 30_000,
    retry: 1,
  });
}
