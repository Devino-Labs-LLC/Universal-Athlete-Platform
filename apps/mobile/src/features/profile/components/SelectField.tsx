import { Control, Controller, FieldPath, FieldValues } from 'react-hook-form';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';

export interface SelectOption<T extends string> {
  value: T;
  label: string;
}

interface SelectFieldProps<TFieldValues extends FieldValues, TValue extends string> {
  control: Control<TFieldValues>;
  name: FieldPath<TFieldValues>;
  label: string;
  options: SelectOption<TValue>[];
}

export function SelectField<TFieldValues extends FieldValues, TValue extends string>({
  control,
  name,
  label,
  options,
}: SelectFieldProps<TFieldValues, TValue>) {
  const theme = useAppTheme();

  return (
    <Controller
      control={control}
      name={name}
      render={({ field: { onChange, value }, fieldState: { error } }) => (
        <View style={styles.field}>
          <Text style={[styles.label, { color: theme.colors.textMuted }]}>{label}</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.row}>
            {options.map((option) => {
              const selected = value === option.value;
              return (
                <Pressable
                  key={option.value}
                  accessibilityRole="button"
                  accessibilityState={{ selected }}
                  onPress={() => onChange(option.value)}
                  style={[
                    styles.chip,
                    {
                      minHeight: 44,
                      justifyContent: 'center',
                      borderColor: selected ? theme.colors.accentCyan : theme.colors.border,
                      backgroundColor: selected
                        ? theme.colors.accentCyanMuted
                        : theme.colors.surface,
                    },
                  ]}>
                  <Text
                    style={{
                      color: selected ? theme.colors.accentCyan : theme.colors.text,
                      fontSize: 13,
                      fontWeight: selected ? '700' : '500',
                    }}>
                    {option.label}
                  </Text>
                </Pressable>
              );
            })}
          </ScrollView>
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
    gap: 8,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
  },
  row: {
    gap: 8,
    paddingVertical: 2,
  },
  chip: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  error: {
    fontSize: 13,
  },
});
