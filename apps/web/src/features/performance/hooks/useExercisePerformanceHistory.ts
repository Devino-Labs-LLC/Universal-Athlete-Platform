import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  type ExerciseHistoryParams,
  fetchExercisePerformanceHistory,
} from '@/features/performance/api/exercisePerformanceApi';
import { performanceKeys } from '@/features/performance/models/queryKeys';

export function useExercisePerformanceHistory(
  exercisePerformanceKey: string | undefined,
  params: ExerciseHistoryParams = {},
) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: performanceKeys.exerciseHistory(exercisePerformanceKey ?? '', params),
    queryFn: () => fetchExercisePerformanceHistory(apiClient, exercisePerformanceKey as string, params),
    enabled: status === 'AUTHENTICATED' && Boolean(exercisePerformanceKey),
  });
}
