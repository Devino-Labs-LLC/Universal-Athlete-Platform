import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import {
  formatDecimal,
  formatDistance,
  formatDurationSeconds,
  formatVolumeKg,
} from '@/src/features/home/utils/formatMetrics';
import { WorkoutOccurrencePerformance } from '@/src/features/performance/models/performanceSchemas';

interface OccurrencePerformanceSummaryProps {
  performance: WorkoutOccurrencePerformance;
}

export function OccurrencePerformanceSummary({ performance }: OccurrencePerformanceSummaryProps) {
  const theme = useAppTheme();
  const { totals } = performance;
  const stats: string[] = [];

  stats.push(`${totals.completedExerciseCount} exercises`);
  stats.push(`${totals.completedSetCount} sets`);
  if (totals.totalRepetitions != null) {
    stats.push(`${totals.totalRepetitions} reps`);
  }
  if (totals.totalVolumeKilogramRepetitions != null) {
    stats.push(`Volume: ${formatVolumeKg(totals.totalVolumeKilogramRepetitions)}`);
  }
  if (totals.totalDurationSeconds != null) {
    stats.push(`Duration: ${formatDurationSeconds(totals.totalDurationSeconds)}`);
  }
  if (totals.totalDistanceMeters != null && Number(totals.totalDistanceMeters) > 0) {
    stats.push(`Distance: ${formatDistance(totals.totalDistanceMeters)}`);
  }
  if (totals.averageRpe != null) {
    stats.push(`Avg RPE: ${formatDecimal(totals.averageRpe)}`);
  }

  return (
    <HomeCard testID="occurrence-performance-summary" title="Session performance">
      <Text style={[styles.summary, { color: theme.colors.textMuted }]}>
        {stats.join(' · ')}
      </Text>
      {performance.exercises.length > 0 ? (
        <View style={styles.exercises}>
          {performance.exercises.map((exercise) => (
            <View key={exercise.executionId} style={styles.exerciseRow}>
              <Text style={[styles.exerciseName, { color: theme.colors.text }]}>
                {exercise.exerciseName}
              </Text>
              <Text style={[styles.exerciseMeta, { color: theme.colors.textMuted }]}>
                {exercise.metrics.completedSetCount} sets
                {exercise.metrics.totalRepetitions != null
                  ? ` · ${exercise.metrics.totalRepetitions} reps`
                  : ''}
              </Text>
            </View>
          ))}
        </View>
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  summary: {
    fontSize: 14,
  },
  exercises: {
    gap: 8,
    marginTop: 8,
  },
  exerciseRow: {
    gap: 2,
  },
  exerciseName: {
    fontSize: 14,
    fontWeight: '600',
  },
  exerciseMeta: {
    fontSize: 12,
  },
});
