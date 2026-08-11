import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import type { DateOnly } from '@/core/date/dateOnly';
import { fetchTodayDashboard } from '@/features/home/api';
import { trainingClientKeys } from '@/features/home/queryKeys';

export function useTodayDashboard(date?: DateOnly) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: trainingClientKeys.today(date),
    queryFn: () => fetchTodayDashboard(apiClient, date),
    enabled: status === 'AUTHENTICATED',
  });
}
