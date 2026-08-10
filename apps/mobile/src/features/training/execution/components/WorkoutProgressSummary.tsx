import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { ExerciseExecution } from '@/src/features/training/models/browseSchemas';

interface WorkoutProgressSummaryProps {
  executions: ExerciseExecution[];
}

function isTerminal(status: string): boolean {
  return status === 'COMPLETED' || status === 'SKIPPED';
}

export function WorkoutProgressSummary({ executions }: WorkoutProgressSummaryProps) {
  const theme = useAppTheme();

  const total = executions.length;
  const completed = executions.filter((e) => e.status === 'COMPLETED').length;
  const skipped = executions.filter((e) => e.status === 'SKIPPED').length;
  const terminal = executions.filter((e) => isTerminal(e.status)).length;

  const totalSets = executions.reduce((sum, e) => sum + (e.setCount ?? 0), 0);
  const completedSets = executions.reduce((sum, e) => sum + (e.completedSetCount ?? 0), 0);
  const skippedSets = executions.reduce((sum, e) => sum + (e.skippedSetCount ?? 0), 0);

  return (
    <HomeCard title="Progress" testID="workout-progress-summary">
      <View style={styles.row}>
        <Text style={[styles.label, { color: theme.colors.textMuted }]}>Exercises</Text>
        <Text style={[styles.value, { color: theme.colors.text }]}>
          {terminal}/{total} done ({completed} completed{skipped > 0 ? `, ${skipped} skipped` : ''})
        </Text>
      </View>
      <View style={styles.row}>
        <Text style={[styles.label, { color: theme.colors.textMuted }]}>Sets</Text>
        <Text style={[styles.value, { color: theme.colors.text }]}>
          {completedSets + skippedSets}/{totalSets} logged
        </Text>
      </View>
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 12,
  },
  label: {
    fontSize: 14,
  },
  value: {
    fontSize: 14,
    fontWeight: '600',
    flexShrink: 1,
    textAlign: 'right',
  },
});
