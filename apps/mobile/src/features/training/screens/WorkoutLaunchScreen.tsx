import { StyleSheet, Text } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
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

  const { actions, occurrence } = context;
  const canStart = actions.canStart.allowed;
  const canContinue = occurrence.status === 'IN_PROGRESS';
  const startLabel = canContinue ? 'Continue Workout' : 'Start Workout';
  const canReviewAdaptation =
    actions.canApplyAdaptation.allowed || context.adaptation?.activeProposalPresent;

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
        <PrimaryButton
          label="Review Adaptation"
          onPress={() =>
            router.push(
              `/(tabs)/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/adaptation-review`,
            )
          }
        />
      ) : null}

      <Text style={[styles.m5Note, { color: theme.colors.textMuted }]}>
        Live workout execution arrives in M5. The start button routes to a placeholder screen only.
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
