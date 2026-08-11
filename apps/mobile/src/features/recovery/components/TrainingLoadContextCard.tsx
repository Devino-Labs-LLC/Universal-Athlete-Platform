import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { RecoveryOverview } from '@/src/features/recovery/models/recoverySchemas';

interface TrainingLoadContextCardProps {
  context: NonNullable<RecoveryOverview['trainingLoadContext']>;
}

function formatNumber(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) {
    return '—';
  }
  return String(Math.round(value));
}

export function TrainingLoadContextCard({ context }: TrainingLoadContextCardProps) {
  const theme = useAppTheme();

  const lines = [
    `Workouts: ${context.occurrenceCount} (${context.ratedOccurrenceCount} rated)`,
    `Exercises: ${context.completedExerciseCount} · Sets: ${context.completedSetCount}`,
    `Volume: ${formatNumber(context.totalVolumeKilograms as number)} kg`,
    `Duration: ${Math.round(context.totalDurationSeconds / 60)} min`,
    `Session RPE load: ${formatNumber(context.totalSessionRpeLoad as number)}`,
  ];

  return (
    <HomeCard testID="training-load-context-card" title="Training load context">
      {lines.map((line) => (
        <Text key={line} style={[styles.line, { color: theme.colors.text }]}>
          {line}
        </Text>
      ))}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  line: {
    fontSize: 14,
  },
});
