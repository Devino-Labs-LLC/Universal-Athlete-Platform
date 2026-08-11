import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { fetchTrainingRecommendation } from '@/src/features/recovery/api/recommendationApi';
import { recoveryKeys } from '@/src/features/recovery/models/recoveryKeys';

export function useRecommendationDetail(recommendationId: string) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: recoveryKeys.recommendation(recommendationId),
    queryFn: () => fetchTrainingRecommendation(apiClient, recommendationId),
    enabled: status === 'AUTHENTICATED' && recommendationId.length > 0,
    staleTime: 30_000,
    retry: 1,
  });
}
