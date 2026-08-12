import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { CompactInfoRow } from '@/src/core/components/Surface';
import { formatEnumLabel } from '@/src/features/profile/enumLabels';
import { WorkoutLoadCategorySummary } from '@/src/features/performance/models/performanceSchemas';
import { formatLoadVolume } from '@/src/features/performance/utils/formatLoadMetrics';
import { formatDurationSeconds } from '@/src/features/home/utils/formatMetrics';

interface CategoryBreakdownListProps {
  summaries?: WorkoutLoadCategorySummary[];
}

export function CategoryBreakdownList({ summaries }: CategoryBreakdownListProps) {
  const theme = useAppTheme();

  if (!summaries || summaries.length === 0) {
    return null;
  }

  return (
    <View style={styles.container} testID="category-breakdown-list">
      <Text style={[styles.heading, { color: theme.colors.textMuted }]}>By category</Text>
      {summaries.map((item) => {
        const volume = formatLoadVolume(item.volumeKilograms);
        const parts = [
          `${item.completedExerciseCount} exercises`,
          `${item.completedSetCount} sets`,
        ];
        if (volume) {
          parts.push(volume);
        }
        if (item.durationSeconds > 0) {
          parts.push(formatDurationSeconds(item.durationSeconds));
        }
        return (
          <CompactInfoRow
            key={item.category}
            label={formatEnumLabel(item.category)}
            value={parts.join(' · ')}
          />
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginTop: 6,
    gap: 6,
  },
  heading: {
    fontSize: 12,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.6,
  },
});
