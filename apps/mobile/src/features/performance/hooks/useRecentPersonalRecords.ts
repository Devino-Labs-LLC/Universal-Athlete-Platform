import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/providers/AuthSessionProvider';
import { fetchRecentPersonalRecords } from '@/src/features/performance/api/personalRecordsApi';
import { performanceKeys } from '@/src/features/performance/models/performanceKeys';

export function useRecentPersonalRecords(days = 30, limit = 5) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: performanceKeys.recentRecords(days, limit),
    queryFn: () => fetchRecentPersonalRecords(apiClient, days, limit),
    enabled: status === 'AUTHENTICATED',
    staleTime: 30_000,
    retry: 1,
  });
}
