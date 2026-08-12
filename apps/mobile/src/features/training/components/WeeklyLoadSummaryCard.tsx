import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { CompactInfoRow, MetricTile } from '@/src/core/components/Surface';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import {
  formatDecimal,
  formatDistance,
  formatDurationSeconds,
  formatVolumeKg,
} from '@/src/features/home/utils/formatMetrics';
import { OverviewWeeklyLoad } from '@/src/features/training/models/browseSchemas';

interface WeeklyLoadSummaryCardProps {
  load: OverviewWeeklyLoad;
}

export function WeeklyLoadSummaryCard({ load }: WeeklyLoadSummaryCardProps) {
  const theme = useAppTheme();

  return (
    <HomeCard
      testID="weekly-load-summary-card"
      eyebrow="Load"
      title="Weekly load"
      subtitle={`${load.weekStartDate} – ${load.weekEndDate}`}>
      <Text style={[styles.summary, { color: theme.colors.textMuted }]}>
        {load.occurrenceCount} sessions · {load.trainingDays} training days
      </Text>
      <View style={styles.metrics}>
        <MetricTile
          label="Volume"
          value={
            load.totalVolumeKilograms != null
              ? formatVolumeKg(load.totalVolumeKilograms)
              : null
          }
        />
        <MetricTile
          label="Avg RPE"
          value={
            load.averageSessionRpe != null ? formatDecimal(load.averageSessionRpe) : null
          }
        />
      </View>
      {load.totalDistanceMeters != null ? (
        <CompactInfoRow label="Distance" value={formatDistance(load.totalDistanceMeters)} />
      ) : null}
      {load.totalDurationSeconds != null ? (
        <CompactInfoRow
          label="Duration"
          value={formatDurationSeconds(load.totalDurationSeconds)}
        />
      ) : null}
      {load.totalSessionRpeLoad != null ? (
        <CompactInfoRow
          label="Session load"
          value={formatDecimal(load.totalSessionRpeLoad)}
        />
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  summary: {
    fontSize: 14,
  },
  metrics: {
    flexDirection: 'row',
    gap: 10,
  },
});
