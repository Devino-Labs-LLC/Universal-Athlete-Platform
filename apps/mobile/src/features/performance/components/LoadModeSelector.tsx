import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { TrainingLoadGranularity } from '@/src/features/performance/models/performanceSchemas';
import { trainingLoadGranularityLabel } from '@/src/features/performance/models/performanceLabels';

const MODES: TrainingLoadGranularity[] = ['OCCURRENCE', 'DAILY', 'WEEKLY'];

interface LoadModeSelectorProps {
  value: TrainingLoadGranularity;
  onChange: (mode: TrainingLoadGranularity) => void;
}

export function LoadModeSelector({ value, onChange }: LoadModeSelectorProps) {
  const theme = useAppTheme();

  return (
    <View style={styles.row} testID="load-mode-selector">
      {MODES.map((mode) => {
        const selected = value === mode;
        return (
          <Pressable
            key={mode}
            accessibilityRole="button"
            accessibilityState={{ selected }}
            testID={`load-mode-${mode}`}
            onPress={() => onChange(mode)}
            style={[
              styles.chip,
              {
                backgroundColor: selected
                  ? theme.colors.accentCyanMuted
                  : theme.colors.surface,
                borderColor: selected ? theme.colors.accentCyan : theme.colors.border,
                minHeight: 44,
              },
            ]}>
            <Text
              style={[
                styles.label,
                { color: selected ? theme.colors.accentCyan : theme.colors.text },
              ]}>
              {trainingLoadGranularityLabel(mode)}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  chip: {
    borderWidth: 1.5,
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 10,
    justifyContent: 'center',
  },
  label: {
    fontSize: 13,
    fontWeight: '700',
  },
});
