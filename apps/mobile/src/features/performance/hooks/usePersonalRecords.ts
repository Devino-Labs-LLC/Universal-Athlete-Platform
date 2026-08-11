import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/providers/AuthSessionProvider';
import { fetchPersonalRecords } from '@/src/features/performance/api/personalRecordsApi';
import { performanceKeys } from '@/src/features/performance/models/performanceKeys';

export function usePersonalRecords(filters?: {
  exercisePerformanceKey?: string;
  recordType?: string;
}) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: performanceKeys.personalRecords(
      filters?.exercisePerformanceKey,
      filters?.recordType,
    ),
    queryFn: () => fetchPersonalRecords(apiClient, filters),
    enabled: status === 'AUTHENTICATED',
    staleTime: 30_000,
    retry: 1,
  });
}
