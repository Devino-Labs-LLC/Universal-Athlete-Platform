import { Control, Controller, FieldPath, FieldValues } from 'react-hook-form';
import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { RecoveryRatingSelector } from '@/src/features/recovery/components/RecoveryRatingSelector';
import { RecoveryRatingMetric } from '@/src/features/recovery/models/ratingLabels';

interface ControlledRatingSelectorProps<TFieldValues extends FieldValues> {
  control: Control<TFieldValues>;
  name: FieldPath<TFieldValues>;
  metric: RecoveryRatingMetric;
  testID?: string;
}

export function ControlledRatingSelector<TFieldValues extends FieldValues>({
  control,
  name,
  metric,
  testID,
}: ControlledRatingSelectorProps<TFieldValues>) {
  const theme = useAppTheme();

  return (
    <Controller
      control={control}
      name={name}
      rules={{ required: 'Required' }}
      render={({ field: { onChange, value }, fieldState: { error } }) => (
        <View style={styles.field}>
          <RecoveryRatingSelector
            metric={metric}
            value={value as number | undefined}
            onChange={onChange}
            testID={testID}
          />
          {error ? (
            <Text style={[styles.error, { color: theme.colors.danger }]}>{error.message}</Text>
          ) : null}
        </View>
      )}
    />
  );
}

const styles = StyleSheet.create({
  field: {
    gap: 4,
  },
  error: {
    fontSize: 13,
  },
});
