import { StyleSheet, View } from 'react-native';

import { CompactInfoRow, MetricTile } from '@/src/core/components/Surface';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { RecoveryOverview } from '@/src/features/recovery/models/recoverySchemas';

interface TrainingLoadContextCardProps {
  context: NonNullable<RecoveryOverview['trainingLoadContext']>;
}

function formatNumber(value: number | null | undefined): string | null {
  if (value == null || Number.isNaN(value)) {
    return null;
  }
  return String(Math.round(value));
}

export function TrainingLoadContextCard({ context }: TrainingLoadContextCardProps) {
  const volume = formatNumber(context.totalVolumeKilograms as number);
  const sessionRpeLoad = formatNumber(context.totalSessionRpeLoad as number);
  const durationMinutes =
    context.totalDurationSeconds != null && !Number.isNaN(context.totalDurationSeconds)
      ? Math.round(context.totalDurationSeconds / 60)
      : null;

  return (
    <HomeCard testID="training-load-context-card" eyebrow="Load" title="Training load context">
      <CompactInfoRow
        label="Workouts"
        value={`${context.occurrenceCount} (${context.ratedOccurrenceCount} rated)`}
      />
      <CompactInfoRow
        label="Exercises / sets"
        value={`${context.completedExerciseCount} · ${context.completedSetCount}`}
      />
      <View style={styles.metrics}>
        <MetricTile label="Volume" value={volume != null ? `${volume} kg` : null} />
        <MetricTile
          label="Duration"
          value={durationMinutes != null ? `${durationMinutes} min` : null}
        />
        <MetricTile label="Session RPE" value={sessionRpeLoad} />
      </View>
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  metrics: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
});
