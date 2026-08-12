import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { StatusBadge } from '@/src/core/components/Surface';
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

function bandTone(
  band: string | null | undefined,
): 'success' | 'warning' | 'danger' | 'info' | 'default' {
  switch (band) {
    case 'ABOVE_BASELINE':
    case 'FAR_ABOVE_BASELINE':
      return 'warning';
    case 'BELOW_BASELINE':
    case 'FAR_BELOW_BASELINE':
      return 'info';
    case 'WITHIN_BASELINE_RANGE':
      return 'success';
    default:
      return 'default';
  }
}

function shortBandLabel(band: string | null | undefined): string {
  switch (band) {
    case 'FAR_ABOVE_BASELINE':
      return 'Far above';
    case 'ABOVE_BASELINE':
      return 'Above';
    case 'WITHIN_BASELINE_RANGE':
      return 'In range';
    case 'BELOW_BASELINE':
      return 'Below';
    case 'FAR_BELOW_BASELINE':
      return 'Far below';
    case 'INSUFFICIENT_DATA':
      return 'Insufficient data';
    default:
      return comparisonBandLabel(band);
  }
}

export function BaselineMetricRow({ deviation }: BaselineMetricRowProps) {
  const theme = useAppTheme();
  const summary = deviationSummaryCopy(deviation.metricType, deviation.comparisonBand);

  return (
    <View style={styles.row} testID={`baseline-row-${deviation.metricType}`}>
      <View style={styles.header}>
        <Text style={[styles.metric, { color: theme.colors.text }]}>
          {metricTypeLabel(deviation.metricType)}
        </Text>
        <StatusBadge
          label={shortBandLabel(deviation.comparisonBand)}
          tone={bandTone(deviation.comparisonBand)}
        />
      </View>
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
  summary: {
    fontSize: 13,
  },
  meta: {
    fontSize: 12,
  },
});
