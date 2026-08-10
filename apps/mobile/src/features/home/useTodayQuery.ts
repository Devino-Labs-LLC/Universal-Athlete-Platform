import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { fetchTrainingToday } from '@/src/features/training/api';

export function useTodayQuery() {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: ['training', 'today'],
    queryFn: () => fetchTrainingToday(apiClient),
    enabled: status === 'AUTHENTICATED',
  });
}
