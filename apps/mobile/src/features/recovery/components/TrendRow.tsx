import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { metricTypeLabel, trendDirectionLabel } from '@/src/features/recovery/models/recoveryLabels';
import { RecoveryOverview } from '@/src/features/recovery/models/recoverySchemas';

interface TrendRowProps {
  trend: RecoveryOverview['trends'][number];
}

export function TrendRow({ trend }: TrendRowProps) {
  const theme = useAppTheme();

  return (
    <View style={styles.row} testID={`trend-row-${trend.metricType}`}>
      <Text style={[styles.metric, { color: theme.colors.text }]}>
        {metricTypeLabel(trend.metricType)}
      </Text>
      <Text style={[styles.direction, { color: theme.colors.textMuted }]}>
        {trendDirectionLabel(trend.trendDirection)}
        {trend.observationCount > 0 ? ` (${trend.observationCount} observations)` : ''}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    gap: 2,
  },
  metric: {
    fontSize: 14,
    fontWeight: '600',
  },
  direction: {
    fontSize: 13,
  },
});
