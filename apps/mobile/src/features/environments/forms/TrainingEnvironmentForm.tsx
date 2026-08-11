import { Control, Controller } from 'react-hook-form';
import { StyleSheet, Switch, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { FormTextField } from '@/src/features/auth/components/FormTextField';
import { EquipmentChips } from '@/src/features/environments/components/EquipmentChips';
import { EquipmentPicker } from '@/src/features/environments/components/EquipmentPicker';
import { TrainingEnvironmentFormValues } from '@/src/features/environments/models/environmentSchemas';
import { trainingEnvironmentTypeOptions } from '@/src/features/environments/models/environmentLabels';
import { SelectField } from '@/src/features/profile/components/SelectField';

interface TrainingEnvironmentFormProps {
  control: Control<TrainingEnvironmentFormValues>;
  values: TrainingEnvironmentFormValues;
  setValue: (
    name: keyof TrainingEnvironmentFormValues,
    value: TrainingEnvironmentFormValues[keyof TrainingEnvironmentFormValues],
  ) => void;
  showDefaultSwitch?: boolean;
  showEquipmentPicker?: boolean;
}

export function TrainingEnvironmentForm({
  control,
  values,
  setValue,
  showDefaultSwitch = false,
  showEquipmentPicker = true,
}: TrainingEnvironmentFormProps) {
  const theme = useAppTheme();

  return (
    <View style={styles.form} testID="training-environment-form">
      <FormTextField control={control} name="name" label="Name" autoCapitalize="words" />

      <SelectField
        control={control}
        name="type"
        label="Environment type"
        options={trainingEnvironmentTypeOptions}
      />

      <View style={styles.field}>
        <Text style={[styles.label, { color: theme.colors.textMuted }]}>Equipment</Text>
        <EquipmentChips equipment={values.availableEquipment} maxVisible={6} />
        {showEquipmentPicker ? (
          <EquipmentPicker
            selected={values.availableEquipment}
            onChange={(next) => setValue('availableEquipment', next)}
          />
        ) : null}
      </View>

      <FormTextField
        control={control}
        name="description"
        label="Description (optional)"
        multiline
        numberOfLines={3}
      />

      <FormTextField
        control={control}
        name="facilityNotes"
        label="Facility notes (optional)"
        multiline
        numberOfLines={3}
      />

      {showDefaultSwitch ? (
        <Controller
          control={control}
          name="defaultEnvironment"
          render={({ field: { onChange, value } }) => (
            <View style={styles.switchRow}>
              <View style={styles.flex}>
                <Text style={[styles.label, { color: theme.colors.text }]}>Set as default</Text>
                <Text style={[styles.hint, { color: theme.colors.textMuted }]}>
                  Used when no other environment is selected for a workout.
                </Text>
              </View>
              <Switch
                accessibilityLabel="Set as default environment"
                value={Boolean(value)}
                onValueChange={onChange}
                testID="default-environment-switch"
              />
            </View>
          )}
        />
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  form: {
    gap: 16,
  },
  field: {
    gap: 8,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
  },
  hint: {
    fontSize: 12,
    marginTop: 2,
  },
  switchRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  flex: {
    flex: 1,
  },
});
