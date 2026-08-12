import { StyleSheet, View } from 'react-native';

import { CompactInfoRow, MetricTile } from '@/src/core/components/Surface';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { ExerciseExecution } from '@/src/features/training/models/browseSchemas';

interface WorkoutProgressSummaryProps {
  executions: ExerciseExecution[];
}

function isTerminal(status: string): boolean {
  return status === 'COMPLETED' || status === 'SKIPPED';
}

export function WorkoutProgressSummary({ executions }: WorkoutProgressSummaryProps) {
  const total = executions.length;
  const completed = executions.filter((e) => e.status === 'COMPLETED').length;
  const skipped = executions.filter((e) => e.status === 'SKIPPED').length;
  const terminal = executions.filter((e) => isTerminal(e.status)).length;

  const totalSets = executions.reduce((sum, e) => sum + (e.setCount ?? 0), 0);
  const completedSets = executions.reduce((sum, e) => sum + (e.completedSetCount ?? 0), 0);
  const skippedSets = executions.reduce((sum, e) => sum + (e.skippedSetCount ?? 0), 0);

  return (
    <HomeCard eyebrow="Session" title="Progress" testID="workout-progress-summary">
      <View style={styles.metrics}>
        <MetricTile
          label="Exercises"
          value={`${terminal}/${total}`}
          caption={
            skipped > 0
              ? `${completed} completed, ${skipped} skipped`
              : `${completed} completed`
          }
        />
        <MetricTile
          label="Sets"
          value={`${completedSets + skippedSets}/${totalSets}`}
          caption="Logged"
        />
      </View>
      <CompactInfoRow
        label="Remaining"
        value={`${Math.max(0, total - terminal)} exercises`}
      />
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  metrics: {
    flexDirection: 'row',
    gap: 10,
  },
});
