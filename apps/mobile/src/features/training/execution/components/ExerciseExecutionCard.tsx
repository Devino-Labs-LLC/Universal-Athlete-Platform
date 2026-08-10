import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { SetRow } from '@/src/features/training/execution/components/SetRow';
import { useExecutionLifecycleMutations } from '@/src/features/training/execution/hooks/useExecutionLifecycleMutations';
import { useSetMutations } from '@/src/features/training/execution/hooks/useSetMutations';
import {
  executionStatusLabel,
  executionStatusVariant,
} from '@/src/features/training/execution/models/executionLabels';
import { WorkoutExerciseSet } from '@/src/features/training/execution/models/executionSchemas';
import { executionErrorMessage, isConflictError } from '@/src/features/training/execution/utils/executionErrors';
import { formatExecutionPrescription } from '@/src/features/training/execution/utils/setFormat';
import {
  allSetsTerminal,
  isExecutionTerminal,
  isSetMutable,
} from '@/src/features/training/execution/utils/setMetrics';
import { ExerciseExecution } from '@/src/features/training/models/browseSchemas';

interface ExerciseExecutionCardProps {
  planId: string;
  dayId: string;
  occurrenceId: string;
  execution: ExerciseExecution;
  sets: WorkoutExerciseSet[];
  readOnly?: boolean;
  onEditSet: (set: WorkoutExerciseSet) => void;
  onRefetchOccurrence: () => void;
}

export function ExerciseExecutionCard({
  planId,
  dayId,
  occurrenceId,
  execution,
  sets,
  readOnly = false,
  onEditSet,
  onRefetchOccurrence,
}: ExerciseExecutionCardProps) {
  const theme = useAppTheme();
  const scope = { planId, dayId, occurrenceId, executionId: execution.id };
  const { completeMutation, skipMutation } = useExecutionLifecycleMutations(scope);
  const { addMutation, deleteMutation } = useSetMutations(scope);

  const performedName =
    execution.performedExerciseName ?? execution.exerciseName ?? 'Exercise';
  const prescribedName = execution.prescribedExerciseName;
  const showPrescribed = execution.substituted && prescribedName && prescribedName !== performedName;

  const sortedSets = [...sets].sort(
    (a, b) => (a.displayOrder ?? a.setNumber) - (b.displayOrder ?? b.setNumber),
  );

  const terminal = isExecutionTerminal(execution.status);
  const canCompleteExercise =
    !readOnly && !terminal && allSetsTerminal(sortedSets) && sortedSets.length > 0;
  const canAddSet = !readOnly && !terminal;
  const completedSets = execution.completedSetCount ?? 0;
  const skippedSets = execution.skippedSetCount ?? 0;
  const setCount = execution.setCount ?? sortedSets.length;

  const handleCompleteExercise = () => {
    completeMutation.mutate(undefined, {
      onError: (error) => {
        if (isConflictError(error)) {
          onRefetchOccurrence();
        }
        Alert.alert('Cannot complete exercise', executionErrorMessage(error));
      },
    });
  };

  const handleSkipExercise = () => {
    Alert.alert(
      'Skip exercise?',
      'Completed sets will remain logged. Remaining sets will be skipped.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Skip exercise',
          style: 'destructive',
          onPress: () => {
            skipMutation.mutate(undefined, {
              onError: (error) => {
                if (isConflictError(error)) {
                  onRefetchOccurrence();
                }
                Alert.alert('Cannot skip exercise', executionErrorMessage(error));
              },
            });
          },
        },
      ],
    );
  };

  const handleAddSet = () => {
    addMutation.mutate(undefined, {
      onError: (error) => {
        Alert.alert('Cannot add set', executionErrorMessage(error));
      },
    });
  };

  const handleDeleteSet = (set: WorkoutExerciseSet) => {
    Alert.alert('Delete set?', `Remove set ${set.setNumber}?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => {
          deleteMutation.mutate(set.id, {
            onError: (error) => {
              Alert.alert('Cannot delete set', executionErrorMessage(error));
            },
          });
        },
      },
    ]);
  };

  return (
    <HomeCard
      title={performedName}
      testID={`exercise-execution-card-${execution.id}`}>
      {showPrescribed ? (
        <Text style={[styles.prescribed, { color: theme.colors.textMuted }]}>
          Prescribed: {prescribedName}
        </Text>
      ) : null}
      <Text style={[styles.prescription, { color: theme.colors.textMuted }]}>
        {formatExecutionPrescription(execution)}
      </Text>
      <View style={styles.chips}>
        <StatusChip
          label={executionStatusLabel(execution.status)}
          variant={executionStatusVariant(execution.status)}
        />
        {execution.substituted ? <StatusChip label="Substituted" variant="warning" /> : null}
      </View>
      <Text style={[styles.setProgress, { color: theme.colors.textMuted }]}>
        Sets {completedSets + skippedSets}/{setCount}
        {skippedSets > 0 ? ` (${skippedSets} skipped)` : ''}
      </Text>

      <View style={styles.setList}>
        {sortedSets.map((set) => (
          <SetRow
            key={set.id}
            set={set}
            readOnly={readOnly || !isSetMutable(set.status)}
            onPress={() => onEditSet(set)}
            onDelete={
              !readOnly &&
              set.status === 'NOT_STARTED' &&
              sortedSets.length > 1
                ? () => handleDeleteSet(set)
                : undefined
            }
          />
        ))}
      </View>

      {!readOnly && !terminal ? (
        <View style={styles.actions}>
          {canAddSet ? (
            <Pressable
              accessibilityRole="button"
              onPress={handleAddSet}
              disabled={addMutation.isPending}
              style={styles.secondaryAction}>
              <Text style={[styles.secondaryLabel, { color: theme.colors.primary }]}>Add set</Text>
            </Pressable>
          ) : null}
          {canCompleteExercise ? (
            <PrimaryButton
              label="Complete exercise"
              onPress={handleCompleteExercise}
              disabled={completeMutation.isPending}
            />
          ) : null}
          <Pressable
            accessibilityRole="button"
            onPress={handleSkipExercise}
            disabled={skipMutation.isPending}
            style={styles.secondaryAction}>
            <Text style={[styles.secondaryLabel, { color: theme.colors.danger }]}>
              Skip exercise
            </Text>
          </Pressable>
        </View>
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  prescribed: {
    fontSize: 13,
  },
  prescription: {
    fontSize: 14,
  },
  chips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  setProgress: {
    fontSize: 13,
  },
  setList: {
    gap: 8,
  },
  actions: {
    gap: 10,
    marginTop: 4,
  },
  secondaryAction: {
    paddingVertical: 10,
    alignItems: 'center',
  },
  secondaryLabel: {
    fontSize: 15,
    fontWeight: '600',
  },
});
