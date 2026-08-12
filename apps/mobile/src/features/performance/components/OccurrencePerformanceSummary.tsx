import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { MetricTile } from '@/src/core/components/Surface';
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

  const volume =
    totals.totalVolumeKilogramRepetitions != null
      ? formatVolumeKg(totals.totalVolumeKilogramRepetitions)
      : null;
  const duration =
    totals.totalDurationSeconds != null
      ? formatDurationSeconds(totals.totalDurationSeconds)
      : null;
  const distance =
    totals.totalDistanceMeters != null && Number(totals.totalDistanceMeters) > 0
      ? formatDistance(totals.totalDistanceMeters)
      : null;
  const avgRpe = totals.averageRpe != null ? formatDecimal(totals.averageRpe) : null;

  return (
    <HomeCard
      testID="occurrence-performance-summary"
      eyebrow="Session"
      title="Session performance">
      <View style={styles.metrics}>
        <MetricTile label="Exercises" value={totals.completedExerciseCount} />
        <MetricTile label="Sets" value={totals.completedSetCount} />
        <MetricTile
          label="Reps"
          value={totals.totalRepetitions != null ? totals.totalRepetitions : null}
        />
      </View>
      <View style={styles.metrics}>
        <MetricTile label="Volume" value={volume} />
        <MetricTile label="Duration" value={duration} />
        <MetricTile label="Avg RPE" value={avgRpe} />
      </View>
      {distance ? <MetricTile label="Distance" value={distance} /> : null}

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
  metrics: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  exercises: {
    gap: 8,
    marginTop: 4,
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
