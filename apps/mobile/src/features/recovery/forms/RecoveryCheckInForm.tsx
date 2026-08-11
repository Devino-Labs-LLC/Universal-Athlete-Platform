import { Control } from 'react-hook-form';
import { StyleSheet, Text, TextInput, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ControlledRatingSelector } from '@/src/features/recovery/components/ControlledRatingSelector';
import { DiscomfortEditor } from '@/src/features/recovery/components/DiscomfortEditor';
import { RecoveryRatingSelector } from '@/src/features/recovery/components/RecoveryRatingSelector';
import { SleepDurationInput } from '@/src/features/recovery/components/SleepDurationInput';
import { CreateCheckInFormValues } from '@/src/features/recovery/models/recoverySchemas';

interface RecoveryCheckInFormProps {
  control: Control<CreateCheckInFormValues>;
  values: CreateCheckInFormValues;
  setValue: (
    name: keyof CreateCheckInFormValues,
    value: CreateCheckInFormValues[keyof CreateCheckInFormValues],
  ) => void;
}

export function RecoveryCheckInForm({ control, values, setValue }: RecoveryCheckInFormProps) {
  const theme = useAppTheme();

  return (
    <View style={styles.form} testID="recovery-check-in-form">
      <ControlledRatingSelector control={control} name="fatigue" metric="fatigue" testID="rating-fatigue" />
      <ControlledRatingSelector
        control={control}
        name="muscleSoreness"
        metric="muscleSoreness"
        testID="rating-soreness"
      />
      <ControlledRatingSelector control={control} name="stress" metric="stress" testID="rating-stress" />
      <ControlledRatingSelector control={control} name="mood" metric="mood" testID="rating-mood" />
      <ControlledRatingSelector
        control={control}
        name="motivation"
        metric="motivation"
        testID="rating-motivation"
      />

      <SleepDurationInput
        totalMinutes={values.sleepDurationMinutes}
        onChange={(minutes) => setValue('sleepDurationMinutes', minutes)}
        testID="sleep-duration"
      />

      <View style={styles.field}>
        <RecoveryRatingSelector
          metric="sleepQuality"
          value={values.sleepQuality}
          onChange={(value) => setValue('sleepQuality', value)}
          testID="rating-sleep-quality"
        />
      </View>

      <DiscomfortEditor
        value={values.discomfortAreas ?? []}
        onChange={(entries) => setValue('discomfortAreas', entries)}
        testID="discomfort-editor"
      />

      <View style={styles.field}>
        <Text style={[styles.label, { color: theme.colors.textMuted }]}>Notes (optional)</Text>
        <TextInput
          accessibilityLabel="Recovery notes"
          multiline
          numberOfLines={4}
          placeholder="Anything else to note about recovery today"
          placeholderTextColor={theme.colors.textMuted}
          value={values.notes ?? ''}
          onChangeText={(text) => setValue('notes', text.slice(0, 2000))}
          style={[
            styles.notes,
            {
              borderColor: theme.colors.border,
              color: theme.colors.text,
              backgroundColor: theme.colors.surface,
            },
          ]}
          testID="check-in-notes"
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  form: {
    gap: 16,
  },
  field: {
    gap: 6,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
  },
  notes: {
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 15,
    minHeight: 96,
    textAlignVertical: 'top',
  },
});
