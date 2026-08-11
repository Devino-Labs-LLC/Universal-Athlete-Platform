import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import type { DateOnly } from '@/core/date/dateOnly';
import {
  fetchRecommendation,
  fetchRecommendationComparison,
  fetchRecommendationHistory,
} from '@/features/recovery/api/recommendationsApi';
import { recoveryKeys } from '@/features/recovery/models/queryKeys';

export function useRecommendation(recommendationId: string | undefined) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.recommendation(recommendationId ?? ''),
    queryFn: () => fetchRecommendation(apiClient, recommendationId as string),
    enabled: status === 'AUTHENTICATED' && Boolean(recommendationId),
  });
}

export function useRecommendationHistory(startDate: DateOnly, endDate: DateOnly) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.recommendationHistory(startDate, endDate),
    queryFn: () => fetchRecommendationHistory(apiClient, startDate, endDate),
    enabled: status === 'AUTHENTICATED' && Boolean(startDate) && Boolean(endDate),
  });
}

export function useRecommendationComparison(
  olderRecommendationId: string | undefined,
  newerRecommendationId: string | undefined,
) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.recommendationCompare(olderRecommendationId ?? '', newerRecommendationId ?? ''),
    queryFn: () =>
      fetchRecommendationComparison(apiClient, olderRecommendationId as string, newerRecommendationId as string),
    enabled: status === 'AUTHENTICATED' && Boolean(olderRecommendationId) && Boolean(newerRecommendationId),
  });
}
