import { useEffect, useRef, useState } from 'react';
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

function initialSleepText(totalMinutes?: number): { hours: string; minutes: string } {
  if (totalMinutes == null) {
    return { hours: '', minutes: '' };
  }
  const split = minutesToHoursMinutes(totalMinutes);
  return { hours: String(split.hours), minutes: String(split.minutes) };
}

export function SleepDurationInput({ totalMinutes, onChange, testID }: SleepDurationInputProps) {
  const theme = useAppTheme();
  const initial = initialSleepText(totalMinutes);
  const [hoursText, setHoursText] = useState(initial.hours);
  const [minutesText, setMinutesText] = useState(initial.minutes);
  const lastEmitted = useRef(totalMinutes);

  useEffect(() => {
    if (totalMinutes === lastEmitted.current) {
      return;
    }
    lastEmitted.current = totalMinutes;
    const next = initialSleepText(totalMinutes);
    setHoursText(next.hours);
    setMinutesText(next.minutes);
  }, [totalMinutes]);

  const handleChange = (nextHours: string, nextMinutes: string) => {
    setHoursText(nextHours);
    setMinutesText(nextMinutes);
    const parsed = parseSleepDurationInput(nextHours, nextMinutes);
    lastEmitted.current = parsed;
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
            placeholderTextColor={theme.colors.textMuted}
            value={hoursText}
            onChangeText={(text) => handleChange(text, minutesText)}
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
            placeholderTextColor={theme.colors.textMuted}
            value={minutesText}
            onChangeText={(text) => handleChange(hoursText, text)}
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
