import { StyleSheet, Text, TextInput, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import {
  minutesToHoursMinutes,
  parseSleepDurationInput,
} from '@/src/features/recovery/utils/sleepDuration';

interface SleepDurationInputProps {
  totalMinutes?: number;
  onChange: (minutes: number | undefined) => void;
  testID?: string;
}

export function SleepDurationInput({ totalMinutes, onChange, testID }: SleepDurationInputProps) {
  const theme = useAppTheme();
  const { hours, minutes } =
    totalMinutes != null ? minutesToHoursMinutes(totalMinutes) : { hours: undefined, minutes: undefined };

  const hoursValue = hours != null ? String(hours) : '';
  const minutesValue = minutes != null ? String(minutes) : '';

  const handleChange = (hoursText: string, minutesText: string) => {
    const parsed = parseSleepDurationInput(hoursText, minutesText);
    onChange(parsed);
  };

  return (
    <View style={styles.container} testID={testID}>
      <Text style={[styles.label, { color: theme.colors.textMuted }]}>Sleep duration (optional)</Text>
      <View style={styles.row}>
        <View style={styles.field}>
          <Text style={[styles.sublabel, { color: theme.colors.textMuted }]}>Hours</Text>
          <TextInput
            accessibilityLabel="Sleep hours"
            keyboardType="number-pad"
            placeholder="0"
            placeholderTextColor={theme.colors.textMuted}
            value={hoursValue}
            onChangeText={(text) => handleChange(text, minutesValue)}
            style={[
              styles.input,
              {
                borderColor: theme.colors.border,
                color: theme.colors.text,
                backgroundColor: theme.colors.surface,
              },
            ]}
            testID={testID ? `${testID}-hours` : undefined}
          />
        </View>
        <View style={styles.field}>
          <Text style={[styles.sublabel, { color: theme.colors.textMuted }]}>Minutes</Text>
          <TextInput
            accessibilityLabel="Sleep minutes"
            keyboardType="number-pad"
            placeholder="0"
            placeholderTextColor={theme.colors.textMuted}
            value={minutesValue}
            onChangeText={(text) => handleChange(hoursValue, text)}
            style={[
              styles.input,
              {
                borderColor: theme.colors.border,
                color: theme.colors.text,
                backgroundColor: theme.colors.surface,
              },
            ]}
            testID={testID ? `${testID}-minutes` : undefined}
          />
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 6,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
  },
  row: {
    flexDirection: 'row',
    gap: 12,
  },
  field: {
    flex: 1,
    gap: 4,
  },
  sublabel: {
    fontSize: 12,
  },
  input: {
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 12,
    fontSize: 16,
    minHeight: 44,
  },
});
