import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Button } from '@/src/core/components/PrimaryButton';
import { MetricTile } from '@/src/core/components/Surface';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { WeeklyTrainingLoadSummary } from '@/src/features/performance/models/performanceSchemas';
import {
  formatLoadVolume,
  formatRatedUnratedSummary,
  formatSessionRpeLoad,
} from '@/src/features/performance/utils/formatLoadMetrics';
import { formatDateDisplay } from '@/src/features/home/utils/formatDateDisplay';
import { formatDurationSeconds } from '@/src/features/home/utils/formatMetrics';

interface LoadSnapshotCardProps {
  summaries: WeeklyTrainingLoadSummary[];
  loading?: boolean;
}

export function LoadSnapshotCard({ summaries, loading }: LoadSnapshotCardProps) {
  const theme = useAppTheme();
  const latest = summaries[0];
  const volume = latest ? formatLoadVolume(latest.totalVolumeKilograms) : null;
  const sessionLoad = latest ? formatSessionRpeLoad(latest.totalSessionRpeLoad) : null;
  const duration =
    latest && latest.totalDurationSeconds > 0
      ? formatDurationSeconds(latest.totalDurationSeconds)
      : null;

  return (
    <HomeCard testID="load-snapshot-card" eyebrow="Load" title="Training load (28 days)">
      {!latest ? (
        <Text style={[styles.empty, { color: theme.colors.textMuted }]}>
          {loading ? 'Loading training load…' : 'No training load recorded in the last 28 days.'}
        </Text>
      ) : (
        <View style={styles.content}>
          <Text style={[styles.period, { color: theme.colors.text }]}>
            {formatDateDisplay(latest.weekStartDate)} – {formatDateDisplay(latest.weekEndDate)}
          </Text>
          <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
            {latest.trainingDays} training days
            {formatRatedUnratedSummary(latest) ? ` · ${formatRatedUnratedSummary(latest)}` : ''}
          </Text>
          <View style={styles.metrics}>
            <MetricTile label="Volume" value={volume} />
            <MetricTile label="Duration" value={duration} />
            <MetricTile label="Session load" value={sessionLoad} />
          </View>
        </View>
      )}
      <Button
        variant="secondary"
        label="Load history"
        onPress={() => router.push('/(tabs)/performance/load')}
      />
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  content: {
    gap: 8,
  },
  period: {
    fontSize: 15,
    fontWeight: '600',
  },
  meta: {
    fontSize: 12,
  },
  metrics: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  empty: {
    fontSize: 14,
  },
});
