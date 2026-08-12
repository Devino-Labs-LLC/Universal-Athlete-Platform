import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EyebrowText } from '@/src/core/components/Surface';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { occurrenceStatusLabel } from '@/src/features/home/models/todayLabels';
import { WorkoutOccurrenceDetail } from '@/src/features/training/models/browseSchemas';

interface WorkoutExecutionHeaderProps {
  detail: WorkoutOccurrenceDetail;
}

export function WorkoutExecutionHeader({ detail }: WorkoutExecutionHeaderProps) {
  const theme = useAppTheme();
  const statusVariant =
    detail.status === 'IN_PROGRESS'
      ? 'info'
      : detail.status === 'COMPLETED'
        ? 'success'
        : detail.status === 'SKIPPED' || detail.status === 'CANCELLED'
          ? 'warning'
          : 'default';

  return (
    <View style={styles.container} testID="workout-execution-header">
      <EyebrowText tone="cyan">Active workout</EyebrowText>
      <View style={styles.titleRow}>
        <Text
          accessibilityRole="header"
          style={[
            styles.title,
            {
              color: theme.colors.text,
              fontSize: theme.typography.pageTitle,
            },
          ]}
          numberOfLines={2}>
          Workout session
        </Text>
        <StatusChip label={occurrenceStatusLabel(detail.status)} variant={statusVariant} />
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
    marginBottom: 4,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 12,
  },
  title: {
    fontWeight: '700',
    flex: 1,
  },
  meta: {
    fontSize: 14,
  },
});
