import { Alert, StyleSheet, Text } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { useGenerateManualAdaptation } from '@/src/features/adaptation/hooks/useGenerateManualAdaptation';
import {
  adaptationErrorMessage,
  isActiveProposalExistsError,
} from '@/src/features/adaptation/utils/adaptationErrors';
import { navigateToAdaptationProposal } from '@/src/features/adaptation/utils/proposalNavigation';
import { LaunchContextSections } from '@/src/features/training/components/LaunchContextSections';
import { useWorkoutLaunchContext } from '@/src/features/training/hooks/useWorkoutLaunchContext';
import { navigateToOccurrenceExecute } from '@/src/features/training/utils/trainingNavigation';

interface WorkoutLaunchScreenProps {
  planId: string;
  dayId: string;
  occurrenceId: string;
}

export function WorkoutLaunchScreen({ planId, dayId, occurrenceId }: WorkoutLaunchScreenProps) {
  const theme = useAppTheme();
  const launchQuery = useWorkoutLaunchContext(planId, dayId, occurrenceId);
  const generateMutation = useGenerateManualAdaptation();

  if (launchQuery.isLoading && !launchQuery.data) {
    return <LoadingView message="Loading workout prep…" />;
  }

  if (launchQuery.isError && !launchQuery.data) {
    const message = isApiError(launchQuery.error)
      ? launchQuery.error.message
      : 'Failed to load launch context';
    return <ErrorView message={message} onRetry={() => launchQuery.refetch()} />;
  }

  const context = launchQuery.data;
  if (!context) {
    return <LoadingView message="Loading workout prep…" />;
  }

  const { actions, occurrence, adaptation } = context;
  const canStart = actions.canStart.allowed;
  const canContinue = occurrence.status === 'IN_PROGRESS';
  const startLabel = canContinue ? 'Continue Workout' : 'Start Workout';
  const proposalId = adaptation?.adaptationProposalId;
  const canReviewAdaptation =
    actions.canApplyAdaptation.allowed || adaptation?.activeProposalPresent;
  const canGenerateAdaptation = actions.canGenerateAdaptation.allowed;

  const handleReviewAdaptation = () => {
    if (proposalId) {
      navigateToAdaptationProposal(planId, dayId, occurrenceId, proposalId);
      return;
    }
    Alert.alert('Adaptation unavailable', 'No active adaptation proposal was found.');
  };

  const handleGenerateAdaptation = () => {
    generateMutation.mutate(
      { planId, dayId, occurrenceId },
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
          if (isActiveProposalExistsError(error) && proposalId) {
            navigateToAdaptationProposal(planId, dayId, occurrenceId, proposalId);
            return;
          }
          Alert.alert('Could not generate alternatives', adaptationErrorMessage(error));
        },
      },
    );
  };

  return (
    <Screen scroll testID="workout-launch-screen">
      <LaunchContextSections context={context} />

      {canStart ? (
        <PrimaryButton
          label={startLabel}
          onPress={() => navigateToOccurrenceExecute(planId, dayId, occurrenceId)}
        />
      ) : (
        <>
          <PrimaryButton label={startLabel} disabled onPress={() => undefined} />
          <Text style={[styles.note, { color: theme.colors.textMuted }]}>
            {actions.canStart.reasonCode
              ? `Start unavailable: ${actions.canStart.reasonCode}`
              : 'Workout cannot be started right now.'}
          </Text>
        </>
      )}

      {actions.canChangeEnvironment.allowed ? (
        <Text style={[styles.note, { color: theme.colors.textMuted }]}>
          Choose Environment is available from the backend, but environment selection ships after M4.
        </Text>
      ) : null}

      {canReviewAdaptation ? (
        <PrimaryButton label="Review Adaptation" onPress={handleReviewAdaptation} />
      ) : null}

      {canGenerateAdaptation ? (
        <PrimaryButton
          testID="find-workout-alternatives"
          label="Find Workout Alternatives"
          onPress={handleGenerateAdaptation}
          disabled={generateMutation.isPending}
        />
      ) : null}

      <Text style={[styles.m5Note, { color: theme.colors.textMuted }]}>
        Set logging and workout completion happen on the execute screen after you start or continue.
      </Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  note: {
    fontSize: 13,
  },
  m5Note: {
    fontSize: 13,
    fontStyle: 'italic',
  },
});
