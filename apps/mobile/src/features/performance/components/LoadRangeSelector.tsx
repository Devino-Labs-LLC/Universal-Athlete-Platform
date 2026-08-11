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
            testID={`load-range-${range}`}
            onPress={() => onChange(range)}
            style={[
              styles.chip,
              {
                backgroundColor: selected ? theme.colors.primary : theme.colors.surface,
                borderColor: theme.colors.border,
              },
            ]}>
            <Text
              style={[
                styles.label,
                { color: selected ? theme.colors.surface : theme.colors.text },
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
    borderWidth: 1,
    borderRadius: 16,
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
  },
});
