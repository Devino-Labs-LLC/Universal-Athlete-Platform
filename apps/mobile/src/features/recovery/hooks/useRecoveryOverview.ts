import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { DateOnly, todayDateOnly } from '@/src/core/date/dateOnly';
import { fetchRecoveryOverview } from '@/src/features/recovery/api/recoveryOverviewApi';
import { recoveryKeys } from '@/src/features/recovery/models/recoveryKeys';
import { TrendDays } from '@/src/features/recovery/models/recoverySchemas';

export function useRecoveryOverview(date?: DateOnly, trendDays: TrendDays = 7) {
  const { apiClient, status } = useAuthSession();
  const queryDate = date ?? todayDateOnly();

  return useQuery({
    queryKey: recoveryKeys.overview(queryDate, trendDays),
    queryFn: () => fetchRecoveryOverview(apiClient, queryDate, trendDays),
    enabled: status === 'AUTHENTICATED',
    staleTime: 30_000,
    retry: 1,
  });
}
