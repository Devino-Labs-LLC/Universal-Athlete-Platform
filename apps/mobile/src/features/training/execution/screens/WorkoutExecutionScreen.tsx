import { useCallback, useMemo, useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { CompletionSummaryCard } from '@/src/features/training/execution/components/CompletionSummaryCard';
import { ExerciseExecutionCard } from '@/src/features/training/execution/components/ExerciseExecutionCard';
import { SessionEffortSheet } from '@/src/features/training/execution/components/SessionEffortSheet';
import { SetEditor } from '@/src/features/training/execution/components/SetEditor';
import { WorkoutExecutionHeader } from '@/src/features/training/execution/components/WorkoutExecutionHeader';
import { WorkoutProgressSummary } from '@/src/features/training/execution/components/WorkoutProgressSummary';
import { useOccurrenceLifecycleMutations } from '@/src/features/training/execution/hooks/useOccurrenceLifecycleMutations';
import { useSessionEffort } from '@/src/features/training/execution/hooks/useSessionEffort';
import { useTrainingLoad } from '@/src/features/training/execution/hooks/useTrainingLoad';
import { useWorkoutExecution } from '@/src/features/training/execution/hooks/useWorkoutExecution';
import { useWorkoutLaunchContext } from '@/src/features/training/hooks/useWorkoutLaunchContext';
import { WorkoutExerciseSet } from '@/src/features/training/execution/models/executionSchemas';
import { executionErrorMessage, isConflictError } from '@/src/features/training/execution/utils/executionErrors';
import { isExecutionTerminal } from '@/src/features/training/execution/utils/setMetrics';
import { navigateToOccurrenceEnvironment } from '@/src/features/training/utils/trainingNavigation';

interface WorkoutExecutionScreenProps {
  planId: string;
  dayId: string;
  occurrenceId: string;
}

interface EditingSetState {
  set: WorkoutExerciseSet;
  executionId: string;
}

export function WorkoutExecutionScreen({
  planId,
  dayId,
  occurrenceId,
}: WorkoutExecutionScreenProps) {
  const theme = useAppTheme();
  const scope = useMemo(() => ({ planId, dayId, occurrenceId }), [planId, dayId, occurrenceId]);

  const { occurrenceQuery, executions, getSetsForExecution } = useWorkoutExecution(
    planId,
    dayId,
    occurrenceId,
  );
  const launchQuery = useWorkoutLaunchContext(planId, dayId, occurrenceId);
  const { startMutation, completeMutation, skipMutation } = useOccurrenceLifecycleMutations(scope);
  const loadQuery = useTrainingLoad(
    scope,
    occurrenceQuery.data?.status === 'COMPLETED',
  );
  const { effortQuery } = useSessionEffort(scope, occurrenceQuery.data?.status === 'COMPLETED');

  const [editingSet, setEditingSet] = useState<EditingSetState | null>(null);
  const [effortSheetVisible, setEffortSheetVisible] = useState(false);

  const refetchOccurrence = useCallback(() => {
    void occurrenceQuery.refetch();
  }, [occurrenceQuery]);

  if (occurrenceQuery.isLoading && !occurrenceQuery.data) {
    return <LoadingView message="Loading workout…" />;
  }

  if (occurrenceQuery.isError && !occurrenceQuery.data) {
    const message = isApiError(occurrenceQuery.error)
      ? occurrenceQuery.error.message
      : 'Failed to load workout';
    return <ErrorView message={message} onRetry={() => occurrenceQuery.refetch()} />;
  }

  const detail = occurrenceQuery.data;
  if (!detail) {
    return <LoadingView message="Loading workout…" />;
  }

  const status = detail.status;
  const readOnly = status === 'COMPLETED' || status === 'SKIPPED' || status === 'CANCELLED';
  const canSubstituteExercise =
    launchQuery.data?.actions.canSubstituteExercise?.allowed ?? false;
  const canChangeEnvironment =
    launchQuery.data?.actions.canChangeEnvironment?.allowed ?? false;
  const allExecutionsTerminal =
    executions.length > 0 && executions.every((e) => isExecutionTerminal(e.status));
  const canCompleteWorkout = status === 'IN_PROGRESS' && allExecutionsTerminal;
  const canSkipWorkout =
    status === 'IN_PROGRESS' || status === 'SCHEDULED';

  const handleStartWorkout = () => {
    if (startMutation.isPending) {
      return;
    }
    startMutation.mutate(undefined, {
      onSuccess: () => {
        refetchOccurrence();
      },
      onError: (error) => {
        if (isConflictError(error)) {
          refetchOccurrence();
        }
        Alert.alert('Cannot start workout', executionErrorMessage(error));
      },
    });
  };

  const handleCompleteWorkout = () => {
    completeMutation.mutate(undefined, {
      onSuccess: () => {
        refetchOccurrence();
        setEffortSheetVisible(true);
      },
      onError: (error) => {
        if (isConflictError(error)) {
          refetchOccurrence();
        }
        Alert.alert('Cannot complete workout', executionErrorMessage(error));
      },
    });
  };

  const handleSkipWorkout = () => {
    Alert.alert('Skip workout?', 'This workout will be marked as skipped.', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Skip workout',
        style: 'destructive',
        onPress: () => {
          skipMutation.mutate(undefined, {
            onError: (error) => {
              if (isConflictError(error)) {
                refetchOccurrence();
              }
              Alert.alert('Cannot skip workout', executionErrorMessage(error));
            },
          });
        },
      },
    ]);
  };

  return (
    <Screen scroll testID="workout-execution-screen">
      <WorkoutExecutionHeader detail={detail} />

      {status === 'SCHEDULED' ? (
        <>
          <Text style={[styles.note, { color: theme.colors.textMuted }]}>
            Tap start when you are ready to log sets and exercises.
          </Text>
          <PrimaryButton
            label="Start workout"
            onPress={handleStartWorkout}
            disabled={startMutation.isPending}
          />
        </>
      ) : null}

      {canChangeEnvironment && !readOnly ? (
        <PrimaryButton
          label="Choose Environment"
          onPress={() => navigateToOccurrenceEnvironment(planId, dayId, occurrenceId)}
        />
      ) : null}

      {status === 'IN_PROGRESS' || status === 'COMPLETED' ? (
        <>
          <WorkoutProgressSummary executions={executions} />
          {executions.map((execution) => {
            const setsQuery = getSetsForExecution(execution.id);
            const sets = (setsQuery?.data as WorkoutExerciseSet[] | undefined) ?? [];
            const showSubstitute =
              canSubstituteExercise &&
              !readOnly &&
              !isExecutionTerminal(execution.status);
            return (
              <ExerciseExecutionCard
                key={execution.id}
                planId={planId}
                dayId={dayId}
                occurrenceId={occurrenceId}
                execution={execution}
                sets={sets}
                readOnly={readOnly}
                canSubstitute={showSubstitute}
                onEditSet={(set) => setEditingSet({ set, executionId: execution.id })}
                onRefetchOccurrence={refetchOccurrence}
              />
            );
          })}
        </>
      ) : null}

      {status === 'SKIPPED' || status === 'CANCELLED' ? (
        <Text style={[styles.note, { color: theme.colors.textMuted }]}>
          This workout is read-only ({status.toLowerCase().replace('_', ' ')}).
        </Text>
      ) : null}

      {status === 'IN_PROGRESS' ? (
        <View style={styles.footerActions}>
          {canCompleteWorkout ? (
            <PrimaryButton
              label="Complete workout"
              onPress={handleCompleteWorkout}
              disabled={completeMutation.isPending}
            />
          ) : (
            <Text style={[styles.note, { color: theme.colors.textMuted }]}>
              Complete or skip all exercises to finish the workout.
            </Text>
          )}
          {canSkipWorkout ? (
            <Pressable accessibilityRole="button" onPress={handleSkipWorkout}>
              <Text style={[styles.skipLabel, { color: theme.colors.danger }]}>Skip workout</Text>
            </Pressable>
          ) : null}
        </View>
      ) : null}

      {status === 'COMPLETED' ? (
        <>
          <CompletionSummaryCard load={loadQuery.data} isLoading={loadQuery.isLoading} />
          <PrimaryButton
            label={effortQuery.data ? 'Edit session effort' : 'Log session effort'}
            onPress={() => setEffortSheetVisible(true)}
          />
        </>
      ) : null}

      {editingSet ? (
        <SetEditor
          visible
          set={editingSet.set}
          planId={planId}
          dayId={dayId}
          occurrenceId={occurrenceId}
          executionId={editingSet.executionId}
          onClose={() => setEditingSet(null)}
        />
      ) : null}

      <SessionEffortSheet
        visible={effortSheetVisible}
        planId={planId}
        dayId={dayId}
        occurrenceId={occurrenceId}
        existingEffort={effortQuery.data}
        onClose={() => setEffortSheetVisible(false)}
        onSubmitted={() => {
          void loadQuery.refetch();
        }}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  note: {
    fontSize: 14,
    lineHeight: 20,
  },
  footerActions: {
    gap: 12,
    marginTop: 8,
  },
  skipLabel: {
    fontSize: 15,
    fontWeight: '600',
    textAlign: 'center',
    paddingVertical: 10,
  },
});
