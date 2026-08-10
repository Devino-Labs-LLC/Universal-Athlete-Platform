import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Alert, Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { FormTextField } from '@/src/features/auth/components/FormTextField';
import { useSetMutations } from '@/src/features/training/execution/hooks/useSetMutations';
import {
  PatchWorkoutExerciseSetRequest,
  WorkoutExerciseSet,
} from '@/src/features/training/execution/models/executionSchemas';
import { executionErrorMessage } from '@/src/features/training/execution/utils/executionErrors';
import { resolveSetFieldKinds, weightUnitLabel } from '@/src/features/training/execution/utils/setMetrics';

interface SetEditorFormValues {
  actualReps?: number;
  actualWeight?: number;
  actualDurationSeconds?: number;
  actualDistance?: number;
  actualRestSeconds?: number;
  actualRpe?: number;
  athleteNotes?: string;
}

interface SetEditorProps {
  visible: boolean;
  set: WorkoutExerciseSet | null;
  planId: string;
  dayId: string;
  occurrenceId: string;
  executionId: string;
  onClose: () => void;
}

function toFormValues(set: WorkoutExerciseSet): SetEditorFormValues {
  return {
    actualReps: set.actualReps ?? undefined,
    actualWeight: set.actualWeight ?? undefined,
    actualDurationSeconds: set.actualDurationSeconds ?? undefined,
    actualDistance: set.actualDistance ?? undefined,
    actualRestSeconds: set.actualRestSeconds ?? undefined,
    actualRpe: set.actualRpe ?? undefined,
    athleteNotes: set.athleteNotes ?? undefined,
  };
}

export function SetEditor({
  visible,
  set,
  planId,
  dayId,
  occurrenceId,
  executionId,
  onClose,
}: SetEditorProps) {
  const theme = useAppTheme();
  const scope = { planId, dayId, occurrenceId, executionId };
  const { patchMutation, saveAndCompleteMutation } = useSetMutations(scope);

  const form = useForm<SetEditorFormValues>({
    defaultValues: set ? toFormValues(set) : {},
  });

  useEffect(() => {
    if (set) {
      form.reset(toFormValues(set));
    }
  }, [set, form]);

  if (!set) {
    return null;
  }

  const fields = resolveSetFieldKinds(set);
  const weightUnit = weightUnitLabel(set.actualWeightUnit ?? set.prescribedWeightUnit);
  const isPending = patchMutation.isPending || saveAndCompleteMutation.isPending;

  const buildRequest = (values: SetEditorFormValues): PatchWorkoutExerciseSetRequest => {
    const request: PatchWorkoutExerciseSetRequest = {};
    if (fields.includes('reps') && values.actualReps !== undefined) {
      request.actualReps = values.actualReps;
    }
    if (fields.includes('weight') && values.actualWeight !== undefined) {
      request.actualWeight = values.actualWeight;
      // Backend WeightUnit: POUND | KILOGRAM
      request.actualWeightUnit =
        set.actualWeightUnit ?? set.prescribedWeightUnit ?? 'POUND';
    }
    if (fields.includes('duration') && values.actualDurationSeconds !== undefined) {
      request.actualDurationSeconds = values.actualDurationSeconds;
    }
    if (fields.includes('distance') && values.actualDistance !== undefined) {
      request.actualDistance = values.actualDistance;
      // Backend DistanceUnit: METER | KILOMETER | MILE
      request.actualDistanceUnit =
        set.actualDistanceUnit ?? set.prescribedDistanceUnit ?? 'METER';
    }
    if (fields.includes('rest') && values.actualRestSeconds !== undefined) {
      request.actualRestSeconds = values.actualRestSeconds;
    }
    if (fields.includes('rpe') && values.actualRpe !== undefined) {
      request.actualRpe = values.actualRpe;
    }
    if (fields.includes('notes')) {
      request.athleteNotes = values.athleteNotes ?? null;
    }
    return request;
  };

  const handleClose = () => {
    if (form.formState.isDirty) {
      Alert.alert('Discard changes?', 'Your edits will be lost.', [
        { text: 'Keep editing', style: 'cancel' },
        { text: 'Discard', style: 'destructive', onPress: onClose },
      ]);
      return;
    }
    onClose();
  };

  const handleSave = form.handleSubmit((values) => {
    patchMutation.mutate(
      { setId: set.id, request: buildRequest(values) },
      {
        onSuccess: () => {
          form.reset(values);
        },
        onError: (error) => {
          Alert.alert('Save failed', executionErrorMessage(error));
        },
      },
    );
  });

  const handleSaveAndComplete = form.handleSubmit((values) => {
    saveAndCompleteMutation.mutate(
      { setId: set.id, request: buildRequest(values) },
      {
        onSuccess: () => {
          onClose();
        },
        onError: (error) => {
          Alert.alert('Complete failed', executionErrorMessage(error));
        },
      },
    );
  });

  return (
    <Modal animationType="slide" visible={visible} onRequestClose={handleClose}>
      <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
        <View style={styles.header}>
          <Text style={[styles.title, { color: theme.colors.text }]}>Set {set.setNumber}</Text>
          <Pressable accessibilityRole="button" onPress={handleClose}>
            <Text style={[styles.cancel, { color: theme.colors.textMuted }]}>Cancel</Text>
          </Pressable>
        </View>
        <ScrollView contentContainerStyle={styles.form} keyboardShouldPersistTaps="handled">
          {fields.includes('reps') ? (
            <FormTextField
              control={form.control}
              name="actualReps"
              label="Reps"
              numeric
              keyboardType="number-pad"
            />
          ) : null}
          {fields.includes('weight') ? (
            <FormTextField
              control={form.control}
              name="actualWeight"
              label={`Weight (${weightUnit})`}
              numeric
              keyboardType="decimal-pad"
            />
          ) : null}
          {fields.includes('duration') ? (
            <FormTextField
              control={form.control}
              name="actualDurationSeconds"
              label="Duration (seconds)"
              numeric
              keyboardType="number-pad"
            />
          ) : null}
          {fields.includes('distance') ? (
            <FormTextField
              control={form.control}
              name="actualDistance"
              label="Distance"
              numeric
              keyboardType="decimal-pad"
            />
          ) : null}
          {fields.includes('rpe') ? (
            <FormTextField
              control={form.control}
              name="actualRpe"
              label="RPE (0–10)"
              numeric
              keyboardType="decimal-pad"
            />
          ) : null}
          {fields.includes('rest') ? (
            <FormTextField
              control={form.control}
              name="actualRestSeconds"
              label="Rest (seconds)"
              numeric
              keyboardType="number-pad"
            />
          ) : null}
          {fields.includes('notes') ? (
            <FormTextField
              control={form.control}
              name="athleteNotes"
              label="Notes"
              multiline
            />
          ) : null}
          <Text style={[styles.hint, { color: theme.colors.textMuted }]}>
            Saving actuals auto-starts the set. No separate start action is needed.
          </Text>
        </ScrollView>
        <View style={styles.actions}>
          <PrimaryButton label="Save" onPress={handleSave} disabled={isPending} />
          <PrimaryButton
            label="Save & complete"
            onPress={handleSaveAndComplete}
            disabled={isPending}
          />
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingTop: 56,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingBottom: 12,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
  },
  cancel: {
    fontSize: 16,
  },
  form: {
    paddingHorizontal: 20,
    paddingBottom: 24,
    gap: 12,
  },
  hint: {
    fontSize: 12,
    lineHeight: 18,
  },
  actions: {
    paddingHorizontal: 20,
    paddingBottom: 32,
    gap: 10,
  },
});
