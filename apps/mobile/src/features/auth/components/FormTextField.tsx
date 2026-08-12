import { Control, Controller, FieldPath, FieldValues } from 'react-hook-form';
import { StyleSheet, Text, TextInput, TextInputProps, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';

interface FormTextFieldProps<TFieldValues extends FieldValues> extends Omit<
  TextInputProps,
  'value' | 'onChangeText'
> {
  control: Control<TFieldValues>;
  name: FieldPath<TFieldValues>;
  label: string;
  numeric?: boolean;
}

export function FormTextField<TFieldValues extends FieldValues>({
  control,
  name,
  label,
  numeric = false,
  ...inputProps
}: FormTextFieldProps<TFieldValues>) {
  const theme = useAppTheme();

  return (
    <Controller
      control={control}
      name={name}
      render={({ field: { onChange, onBlur, value }, fieldState: { error } }) => (
        <View style={styles.field}>
          <Text accessibilityRole="text" style={[styles.label, { color: theme.colors.textMuted }]}>
            {label}
          </Text>
          <TextInput
            accessibilityLabel={label}
            placeholderTextColor={theme.colors.textMuted}
            {...inputProps}
            style={[
              styles.input,
              {
                borderColor: error ? theme.colors.danger : theme.colors.border,
                color: theme.colors.text,
                backgroundColor: theme.colors.surfaceElevated,
                minHeight: inputProps.multiline ? 88 : 44,
              },
              inputProps.style,
            ]}
            value={value === undefined || value === null ? '' : String(value)}
            onBlur={onBlur}
            selectionColor={theme.colors.accentCyan}
            onChangeText={(text) => {
              if (numeric) {
                onChange(text === '' ? undefined : Number(text));
                return;
              }
              onChange(text);
            }}
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
    gap: 6,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
  },
  input: {
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
  },
  error: {
    fontSize: 13,
  },
});
