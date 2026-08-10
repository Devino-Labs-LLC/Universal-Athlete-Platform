import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { DateOnly, todayDateOnly } from '@/src/core/date/dateOnly';
import { fetchTrainingOverview } from '@/src/features/training/api/overviewApi';
import { trainingKeys } from '@/src/features/training/models/queryKeys';

export function useTrainingOverview(date?: DateOnly) {
  const { apiClient, status } = useAuthSession();
  const queryDate = date ?? todayDateOnly();

  return useQuery({
    queryKey: trainingKeys.overview(queryDate),
    queryFn: () => fetchTrainingOverview(apiClient, queryDate),
    enabled: status === 'AUTHENTICATED',
    staleTime: 30_000,
    retry: 1,
  });
}
