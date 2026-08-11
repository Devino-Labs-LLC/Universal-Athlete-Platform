import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import type { DateOnly } from '@/core/date/dateOnly';
import { fetchRecoveryOverview } from '@/features/recovery/api/overviewApi';
import { recoveryKeys } from '@/features/recovery/models/queryKeys';
import type { TrendDays } from '@/features/recovery/models/schemas';

export function useRecoveryOverview(date: DateOnly, trendDays: TrendDays = 7) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.overview(date, trendDays),
    queryFn: () => fetchRecoveryOverview(apiClient, date, trendDays),
    enabled: status === 'AUTHENTICATED' && Boolean(date),
  });
}
