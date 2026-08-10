import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
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

  const stats: string[] = [];
  if (load.totalVolumeKilograms != null) {
    stats.push(`Volume: ${formatVolumeKg(load.totalVolumeKilograms)}`);
  }
  if (load.totalDistanceMeters != null) {
    stats.push(`Distance: ${formatDistance(load.totalDistanceMeters)}`);
  }
  if (load.totalDurationSeconds != null) {
    stats.push(`Duration: ${formatDurationSeconds(load.totalDurationSeconds)}`);
  }
  if (load.totalSessionRpeLoad != null) {
    stats.push(`Session load: ${formatDecimal(load.totalSessionRpeLoad)}`);
  }
  if (load.averageSessionRpe != null) {
    stats.push(`Avg RPE: ${formatDecimal(load.averageSessionRpe)}`);
  }

  return (
    <HomeCard
      testID="weekly-load-summary-card"
      title="Weekly load"
      subtitle={`${load.weekStartDate} – ${load.weekEndDate}`}>
      <Text style={[styles.summary, { color: theme.colors.textMuted }]}>
        {load.occurrenceCount} sessions · {load.trainingDays} training days
      </Text>
      {stats.length > 0 ? (
        <View style={styles.stats}>
          {stats.map((stat) => (
            <Text key={stat} style={[styles.stat, { color: theme.colors.text }]}>
              {stat}
            </Text>
          ))}
        </View>
      ) : (
        <Text style={[styles.summary, { color: theme.colors.textMuted }]}>
          No load metrics recorded this week.
        </Text>
      )}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  summary: {
    fontSize: 14,
  },
  stats: {
    gap: 4,
  },
  stat: {
    fontSize: 15,
    fontWeight: '500',
  },
});
