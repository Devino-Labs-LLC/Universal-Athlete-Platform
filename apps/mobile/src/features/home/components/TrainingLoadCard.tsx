import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import {
  formatDecimal,
  formatDistance,
  formatDurationSeconds,
  formatVolumeKg,
} from '@/src/features/home/utils/formatMetrics';
import { TrainingTodayDashboard } from '@/src/features/training/schemas';

interface TrainingLoadCardProps {
  trainingLoad: NonNullable<TrainingTodayDashboard['trainingLoad']>;
}

export function TrainingLoadCard({ trainingLoad }: TrainingLoadCardProps) {
  const theme = useAppTheme();

  if (!trainingLoad.loadPresent) {
    return null;
  }

  const stats: string[] = [];

  if (trainingLoad.totalVolumeKilograms != null) {
    stats.push(`Volume: ${formatVolumeKg(trainingLoad.totalVolumeKilograms)}`);
  }
  if (trainingLoad.totalDistanceMeters != null) {
    stats.push(`Distance: ${formatDistance(trainingLoad.totalDistanceMeters)}`);
  }
  if (trainingLoad.totalDurationSeconds != null) {
    stats.push(`Duration: ${formatDurationSeconds(trainingLoad.totalDurationSeconds)}`);
  }
  if (trainingLoad.totalSessionRpeLoad != null) {
    stats.push(`Session load: ${formatDecimal(trainingLoad.totalSessionRpeLoad)}`);
  }
  if (trainingLoad.averageSessionRpe != null) {
    stats.push(`Avg RPE: ${formatDecimal(trainingLoad.averageSessionRpe)}`);
  }

  return (
    <HomeCard testID="training-load-card" title="Training load">
      <Text style={[styles.summary, { color: theme.colors.textMuted }]}>
        {trainingLoad.completedExerciseCount ?? 0} exercises ·{' '}
        {trainingLoad.completedSetCount ?? 0} sets · {trainingLoad.occurrenceCount ?? 0}{' '}
        sessions
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
          No load metrics recorded yet today.
        </Text>
      )}

      {(trainingLoad.unratedOccurrenceCount ?? 0) > 0 ? (
        <Text style={[styles.note, { color: theme.colors.warning }]}>
          {trainingLoad.unratedOccurrenceCount} session(s) awaiting effort rating
        </Text>
      ) : null}
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
  note: {
    fontSize: 13,
    fontWeight: '500',
  },
});
