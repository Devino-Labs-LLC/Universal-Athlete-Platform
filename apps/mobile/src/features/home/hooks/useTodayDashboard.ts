import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { DateOnly } from '@/src/core/date/dateOnly';
import { fetchTrainingToday } from '@/src/features/home/api/todayApi';
import { todayQueryKeys } from '@/src/features/home/models/queryKeys';

export function useTodayDashboard(date?: DateOnly) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: todayQueryKeys.date(date),
    queryFn: () => fetchTrainingToday(apiClient, date),
    enabled: status === 'AUTHENTICATED',
    staleTime: 30_000,
    retry: 1,
    refetchOnMount: true,
  });
}
