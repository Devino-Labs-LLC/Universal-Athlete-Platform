import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { LoadRangeKey } from '@/src/features/performance/utils/dateRanges';
import { loadRangeLabel } from '@/src/features/performance/models/performanceLabels';

const RANGES: LoadRangeKey[] = ['7D', '28D', '90D'];

interface LoadRangeSelectorProps {
  value: LoadRangeKey;
  onChange: (range: LoadRangeKey) => void;
}

export function LoadRangeSelector({ value, onChange }: LoadRangeSelectorProps) {
  const theme = useAppTheme();

  return (
    <View style={styles.row} testID="load-range-selector">
      {RANGES.map((range) => {
        const selected = value === range;
        return (
          <Pressable
            key={range}
            accessibilityRole="button"
            accessibilityState={{ selected }}
            testID={`load-range-${range}`}
            onPress={() => onChange(range)}
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
              {loadRangeLabel(range)}
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
