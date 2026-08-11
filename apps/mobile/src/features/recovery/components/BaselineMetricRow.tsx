import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import {
  baselineSufficiencyLabel,
  comparisonBandLabel,
  metricTypeLabel,
} from '@/src/features/recovery/models/recoveryLabels';
import { RecoveryOverview } from '@/src/features/recovery/models/recoverySchemas';
import { deviationSummaryCopy } from '@/src/features/recovery/utils/comparisonCopy';

interface BaselineMetricRowProps {
  deviation: RecoveryOverview['deviations'][number];
}

export function BaselineMetricRow({ deviation }: BaselineMetricRowProps) {
  const theme = useAppTheme();
  const summary = deviationSummaryCopy(deviation.metricType, deviation.comparisonBand);

  return (
    <View style={styles.row} testID={`baseline-row-${deviation.metricType}`}>
      <Text style={[styles.metric, { color: theme.colors.text }]}>
        {metricTypeLabel(deviation.metricType)}
      </Text>
      <Text style={[styles.summary, { color: theme.colors.textMuted }]}>{summary}</Text>
      <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
        {comparisonBandLabel(deviation.comparisonBand)} ·{' '}
        {baselineSufficiencyLabel(deviation.dataSufficiency)}
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
  summary: {
    fontSize: 13,
  },
  meta: {
    fontSize: 12,
  },
});
