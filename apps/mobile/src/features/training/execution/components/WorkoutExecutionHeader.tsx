import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { occurrenceStatusLabel } from '@/src/features/home/models/todayLabels';
import { WorkoutOccurrenceDetail } from '@/src/features/training/models/browseSchemas';

interface WorkoutExecutionHeaderProps {
  detail: WorkoutOccurrenceDetail;
}

export function WorkoutExecutionHeader({ detail }: WorkoutExecutionHeaderProps) {
  const theme = useAppTheme();

  return (
    <View style={styles.container} testID="workout-execution-header">
      <View style={styles.titleRow}>
        <Text style={[styles.title, { color: theme.colors.text }]}>Workout session</Text>
        <StatusChip
          label={occurrenceStatusLabel(detail.status)}
          variant={detail.status === 'IN_PROGRESS' ? 'info' : 'default'}
        />
      </View>
      <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
        Scheduled {detail.scheduledDate}
        {detail.plannedStartTime ? ` · ${detail.plannedStartTime}` : ''}
      </Text>
      {detail.startedAt ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Started {detail.startedAt}
        </Text>
      ) : null}
      {detail.completedAt ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Completed {detail.completedAt}
        </Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 6,
    marginBottom: 8,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    flex: 1,
  },
  meta: {
    fontSize: 14,
  },
});
