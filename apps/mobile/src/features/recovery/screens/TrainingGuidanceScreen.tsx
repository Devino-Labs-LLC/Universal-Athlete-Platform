import { useLocalSearchParams } from 'expo-router';

import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { GuidanceDetailCard } from '@/src/features/recovery/components/GuidanceDetailCard';
import { useRecommendationDetail } from '@/src/features/recovery/hooks/useRecommendationDetail';

export function TrainingGuidanceScreen() {
  const { recommendationId } = useLocalSearchParams<{ recommendationId: string }>();
  const id = recommendationId ?? '';

  const query = useRecommendationDetail(id);

  if (query.isLoading) {
    return <LoadingView message="Loading guidance…" />;
  }

  if (query.isError || !query.data) {
    const message = isApiError(query.error)
      ? query.error.message
      : 'Failed to load training guidance';
    return <ErrorView message={message} onRetry={() => query.refetch()} />;
  }

  return (
    <Screen scroll testID="training-guidance-screen">
      <GuidanceDetailCard recommendation={query.data} />
    </Screen>
  );
}
