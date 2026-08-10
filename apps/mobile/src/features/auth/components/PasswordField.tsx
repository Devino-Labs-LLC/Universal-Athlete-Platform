import { useState } from 'react';
import { Control, Controller, FieldPath, FieldValues } from 'react-hook-form';
import { Pressable, StyleSheet, Text, TextInput, TextInputProps, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';

interface PasswordFieldProps<TFieldValues extends FieldValues> extends Omit<
  TextInputProps,
  'value' | 'onChangeText' | 'secureTextEntry'
> {
  control: Control<TFieldValues>;
  name: FieldPath<TFieldValues>;
  label: string;
}

export function PasswordField<TFieldValues extends FieldValues>({
  control,
  name,
  label,
  ...inputProps
}: PasswordFieldProps<TFieldValues>) {
  const theme = useAppTheme();
  const [visible, setVisible] = useState(false);

  return (
    <Controller
      control={control}
      name={name}
      render={({ field: { onChange, onBlur, value }, fieldState: { error } }) => (
        <View style={styles.field}>
          <Text accessibilityRole="text" style={[styles.label, { color: theme.colors.textMuted }]}>
            {label}
          </Text>
          <View style={styles.inputRow}>
            <TextInput
              accessibilityLabel={label}
              autoCapitalize="none"
              autoComplete="password"
              placeholderTextColor={theme.colors.textMuted}
              secureTextEntry={!visible}
              style={[
                styles.input,
                {
                  borderColor: error ? theme.colors.danger : theme.colors.border,
                  color: theme.colors.text,
                  backgroundColor: theme.colors.surface,
                },
              ]}
              value={value ?? ''}
              onBlur={onBlur}
              onChangeText={onChange}
              {...inputProps}
            />
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={visible ? 'Hide password' : 'Show password'}
              onPress={() => setVisible((current) => !current)}
              style={styles.toggle}>
              <Text style={{ color: theme.colors.primary }}>{visible ? 'Hide' : 'Show'}</Text>
            </Pressable>
          </View>
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
  inputRow: {
    position: 'relative',
  },
  input: {
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    paddingRight: 64,
    fontSize: 16,
  },
  toggle: {
    position: 'absolute',
    right: 12,
    top: 10,
  },
  error: {
    fontSize: 13,
  },
});
