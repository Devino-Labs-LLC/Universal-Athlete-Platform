import { Alert } from 'react-native';
import { useLocalSearchParams } from 'expo-router';

import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { useGenerateRecommendedAdaptation } from '@/src/features/adaptation/hooks/useGenerateRecommendedAdaptation';
import {
  adaptationErrorMessage,
  isActiveProposalExistsError,
} from '@/src/features/adaptation/utils/adaptationErrors';
import { navigateToAdaptationProposal } from '@/src/features/adaptation/utils/proposalNavigation';
import { GuidanceDetailCard } from '@/src/features/recovery/components/GuidanceDetailCard';
import { useRecommendationDetail } from '@/src/features/recovery/hooks/useRecommendationDetail';

function pickModifiableOccurrence(
  recommendation: NonNullable<ReturnType<typeof useRecommendationDetail>['data']>,
) {
  const occurrences = recommendation.scheduledOccurrences ?? [];
  return (
    occurrences.find((item) => item.modifiable) ??
    occurrences.slice().sort((a, b) => a.orderIndex - b.orderIndex)[0] ??
    null
  );
}

export function TrainingGuidanceScreen() {
  const { recommendationId } = useLocalSearchParams<{ recommendationId: string }>();
  const id = recommendationId ?? '';

  const query = useRecommendationDetail(id);
  const generateMutation = useGenerateRecommendedAdaptation();

  if (query.isLoading) {
    return <LoadingView message="Loading guidance…" />;
  }

  if (query.isError || !query.data) {
    const message = isApiError(query.error)
      ? query.error.message
      : 'Failed to load training guidance';
    return <ErrorView message={message} onRetry={() => query.refetch()} />;
  }

  const handleReviewAdaptation = () => {
    const occurrence = pickModifiableOccurrence(query.data);
    if (!occurrence) {
      Alert.alert('No modifiable workout', 'There is no scheduled workout available to adapt.');
      return;
    }

    generateMutation.mutate(
      {
        recommendationId: query.data.recommendationId,
        occurrenceId: occurrence.occurrenceId,
      },
      {
        onSuccess: (proposal) => {
          navigateToAdaptationProposal(
            proposal.trainingPlanId,
            proposal.workoutDayId,
            proposal.workoutOccurrenceId,
            proposal.id,
          );
        },
        onError: (error) => {
          Alert.alert('Could not start adaptation review', adaptationErrorMessage(error));
          if (isActiveProposalExistsError(error)) {
            // Athlete should use Home or launch to open the existing proposal.
          }
        },
      },
    );
  };

  return (
    <Screen scroll testID="training-guidance-screen">
      <GuidanceDetailCard
        recommendation={query.data}
        onReviewAdaptation={handleReviewAdaptation}
        adaptationPending={generateMutation.isPending}
      />
    </Screen>
  );
}
