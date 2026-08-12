import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { StatusBadge } from '@/src/core/components/Surface';
import { metricTypeLabel, trendDirectionLabel } from '@/src/features/recovery/models/recoveryLabels';
import { RecoveryOverview } from '@/src/features/recovery/models/recoverySchemas';

interface TrendRowProps {
  trend: RecoveryOverview['trends'][number];
}

function trendTone(
  direction: string | null | undefined,
): 'success' | 'warning' | 'danger' | 'info' | 'default' {
  switch (direction) {
    case 'INCREASING':
      return 'warning';
    case 'DECREASING':
      return 'info';
    case 'STABLE':
      return 'success';
    default:
      return 'default';
  }
}

export function TrendRow({ trend }: TrendRowProps) {
  const theme = useAppTheme();

  return (
    <View style={styles.row} testID={`trend-row-${trend.metricType}`}>
      <View style={styles.header}>
        <Text style={[styles.metric, { color: theme.colors.text }]}>
          {metricTypeLabel(trend.metricType)}
        </Text>
        <StatusBadge
          label={trendDirectionLabel(trend.trendDirection)}
          tone={trendTone(trend.trendDirection)}
        />
      </View>
      <Text style={[styles.direction, { color: theme.colors.textMuted }]}>
        {trendDirectionLabel(trend.trendDirection)}
        {trend.observationCount > 0 ? ` (${trend.observationCount} observations)` : ''}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    gap: 4,
    paddingVertical: 4,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 8,
  },
  metric: {
    fontSize: 14,
    fontWeight: '600',
    flex: 1,
  },
  direction: {
    fontSize: 13,
  },
});
