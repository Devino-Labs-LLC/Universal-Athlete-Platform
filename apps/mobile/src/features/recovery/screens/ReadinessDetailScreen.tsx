import { useLocalSearchParams } from 'expo-router';

import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { ReadinessDetailCard } from '@/src/features/recovery/components/ReadinessDetailCard';
import { useReadinessAssessment } from '@/src/features/recovery/hooks/useReadinessAssessment';

export function ReadinessDetailScreen() {
  const { assessmentId } = useLocalSearchParams<{ assessmentId: string }>();
  const id = assessmentId ?? '';

  const query = useReadinessAssessment(id);

  if (query.isLoading) {
    return <LoadingView message="Loading readiness…" />;
  }

  if (query.isError || !query.data) {
    const message = isApiError(query.error)
      ? query.error.message
      : 'Failed to load readiness assessment';
    return <ErrorView message={message} onRetry={() => query.refetch()} />;
  }

  return (
    <Screen scroll testID="readiness-detail-screen">
      <ReadinessDetailCard assessment={query.data} />
    </Screen>
  );
}
