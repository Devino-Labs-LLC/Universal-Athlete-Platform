import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { ExerciseExecutionPerformance } from '@/src/features/performance/models/performanceSchemas';
import {
  formatPerformanceMetricsSummary,
  performanceMetricsPrIndicators,
} from '@/src/features/performance/utils/formatPerformanceMetrics';
import { formatDateDisplay } from '@/src/features/home/utils/formatDateDisplay';

interface ExerciseHistoryRowProps {
  entry: ExerciseExecutionPerformance;
}

export function ExerciseHistoryRow({ entry }: ExerciseHistoryRowProps) {
  const theme = useAppTheme();
  const indicators = performanceMetricsPrIndicators(entry.metrics);

  return (
    <View style={styles.row} testID={`exercise-history-row-${entry.executionId}`}>
      <Text style={[styles.date, { color: theme.colors.text }]}>
        {formatDateDisplay(entry.scheduledDate)}
      </Text>
      <Text style={[styles.metrics, { color: theme.colors.textMuted }]}>
        {formatPerformanceMetricsSummary(entry.metrics)}
      </Text>
      {indicators.length > 0 ? (
        <View style={styles.indicators}>
          {indicators.map((indicator) => (
            <StatusChip key={indicator} label={indicator} variant="success" />
          ))}
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    gap: 4,
    marginBottom: 12,
  },
  date: {
    fontSize: 14,
    fontWeight: '600',
  },
  metrics: {
    fontSize: 13,
  },
  indicators: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
  },
});
