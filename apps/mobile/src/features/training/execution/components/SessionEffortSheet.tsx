import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Alert, Modal, ScrollView, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { isApiError } from '@/src/core/api/errors';
import { Button, PrimaryButton } from '@/src/core/components/PrimaryButton';
import { FormTextField } from '@/src/features/auth/components/FormTextField';
import { useSessionEffort } from '@/src/features/training/execution/hooks/useSessionEffort';
import { SessionEffort, SessionEffortRequest } from '@/src/features/training/execution/models/executionSchemas';
import { executionErrorMessage } from '@/src/features/training/execution/utils/executionErrors';

interface SessionEffortSheetProps {
  visible: boolean;
  planId: string;
  dayId: string;
  occurrenceId: string;
  existingEffort?: SessionEffort | null;
  onClose: () => void;
  onSubmitted?: () => void;
}

export function SessionEffortSheet({
  visible,
  planId,
  dayId,
  occurrenceId,
  existingEffort,
  onClose,
  onSubmitted,
}: SessionEffortSheetProps) {
  const theme = useAppTheme();
  const scope = { planId, dayId, occurrenceId };
  const { submitMutation, updateMutation } = useSessionEffort(scope, visible);
  const isEdit = existingEffort != null;

  const form = useForm<SessionEffortRequest>({
    defaultValues: {
      sessionRpe: existingEffort?.sessionRpe ?? undefined,
      sessionDurationMinutes: existingEffort?.sessionDurationMinutes ?? undefined,
      perceivedNotes: existingEffort?.perceivedNotes ?? undefined,
    },
  });

  useEffect(() => {
    if (existingEffort) {
      form.reset({
        sessionRpe: existingEffort.sessionRpe,
        sessionDurationMinutes: existingEffort.sessionDurationMinutes ?? undefined,
        perceivedNotes: existingEffort.perceivedNotes ?? undefined,
      });
    }
  }, [existingEffort, form]);

  const isPending = submitMutation.isPending || updateMutation.isPending;

  const handleSubmit = form.handleSubmit((values) => {
    const mutate = isEdit ? updateMutation : submitMutation;
    mutate.mutate(values, {
      onSuccess: () => {
        onSubmitted?.();
        onClose();
      },
      onError: (error) => {
        if (isApiError(error) && error.code === 'WORKOUT_SESSION_EFFORT_ALREADY_EXISTS') {
          updateMutation.mutate(values, {
            onSuccess: () => {
              onSubmitted?.();
              onClose();
            },
            onError: (patchError) => {
              Alert.alert('Submit failed', executionErrorMessage(patchError));
            },
          });
          return;
        }
        Alert.alert('Submit failed', executionErrorMessage(error));
      },
    });
  });

  return (
    <Modal animationType="slide" visible={visible} transparent onRequestClose={onClose}>
      <View style={[styles.backdrop, { backgroundColor: theme.colors.overlay }]}>
        <View style={[styles.sheet, { backgroundColor: theme.colors.surfaceElevated }]}>
          <View style={styles.header}>
            <Text style={[styles.title, { color: theme.colors.text }]}>
              {isEdit ? 'Edit session effort' : 'Log session effort'}
            </Text>
            <Button variant="ghost" label="Skip for now" onPress={onClose} />
          </View>
          <ScrollView contentContainerStyle={styles.form} keyboardShouldPersistTaps="handled">
            <FormTextField
              control={form.control}
              name="sessionRpe"
              label="Session RPE (0–10, required)"
              numeric
              keyboardType="decimal-pad"
            />
            <FormTextField
              control={form.control}
              name="sessionDurationMinutes"
              label="Duration (minutes, optional)"
              numeric
              keyboardType="number-pad"
            />
            <FormTextField
              control={form.control}
              name="perceivedNotes"
              label="Notes (optional)"
              multiline
            />
          </ScrollView>
          <PrimaryButton
            label={isEdit ? 'Update effort' : 'Submit effort'}
            onPress={handleSubmit}
            loading={isPending}
          />
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  sheet: {
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    padding: 20,
    gap: 12,
    maxHeight: '85%',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 8,
  },
  title: {
    fontSize: 18,
    fontWeight: '700',
    flex: 1,
  },
  form: {
    gap: 12,
    paddingBottom: 8,
  },
});
