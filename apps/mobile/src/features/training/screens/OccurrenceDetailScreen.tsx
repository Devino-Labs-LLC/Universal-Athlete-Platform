import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { occurrenceStatusLabel } from '@/src/features/home/models/todayLabels';
import { formatEnumLabel } from '@/src/features/profile/enumLabels';
import { useOccurrenceDetail } from '@/src/features/training/hooks/useOccurrenceDetail';
import { navigateToOccurrenceLaunch } from '@/src/features/training/utils/trainingNavigation';

interface OccurrenceDetailScreenProps {
  planId: string;
  dayId: string;
  occurrenceId: string;
}

function launchButtonLabel(status: string): string {
  switch (status) {
    case 'IN_PROGRESS':
      return 'Continue Workout';
    case 'COMPLETED':
      return 'Review Workout';
    default:
      return 'Prepare Workout';
  }
}

export function OccurrenceDetailScreen({
  planId,
  dayId,
  occurrenceId,
}: OccurrenceDetailScreenProps) {
  const theme = useAppTheme();
  const detailQuery = useOccurrenceDetail(planId, dayId, occurrenceId);

  if (detailQuery.isLoading && !detailQuery.data) {
    return <LoadingView message="Loading workout…" />;
  }

  if (detailQuery.isError && !detailQuery.data) {
    const message = isApiError(detailQuery.error)
      ? detailQuery.error.message
      : 'Failed to load workout occurrence';
    return <ErrorView message={message} onRetry={() => detailQuery.refetch()} />;
  }

  const detail = detailQuery.data;
  if (!detail) {
    return <LoadingView message="Loading workout…" />;
  }

  const plannedEnv = detail.environment?.plannedEnvironment?.name;
  const actualEnv = detail.environment?.actualEnvironment?.name;
  const executions = detail.executions ?? [];

  return (
    <Screen scroll testID="occurrence-detail-screen">
      <HomeCard title="Workout occurrence">
        <StatusChip
          label={occurrenceStatusLabel(detail.status)}
          variant={detail.status === 'IN_PROGRESS' ? 'info' : 'default'}
        />
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Scheduled {detail.scheduledDate}
          {detail.plannedStartTime ? ` at ${detail.plannedStartTime}` : ''}
        </Text>
        {detail.manuallyRescheduled ? (
          <Text style={[styles.meta, { color: theme.colors.warning }]}>
            Rescheduled from {detail.originalScheduledDate ?? 'original date'}
          </Text>
        ) : null}
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
      </HomeCard>

      <HomeCard title="Environment">
        <Text style={[styles.meta, { color: theme.colors.text }]}>
          Actual: {actualEnv ?? 'Not set'}
        </Text>
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Planned: {plannedEnv ?? 'Not set'}
        </Text>
      </HomeCard>

      <HomeCard title="Exercise progress">
        {executions.length === 0 ? (
          <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
            No exercise executions recorded yet.
          </Text>
        ) : (
          executions.map((execution) => (
            <View key={execution.id} style={styles.executionRow}>
              <Text style={[styles.executionName, { color: theme.colors.text }]}>
                {execution.exerciseName}
              </Text>
              <View style={styles.executionMeta}>
                <StatusChip label={formatEnumLabel(execution.status)} variant="default" />
                {execution.substituted ? (
                  <StatusChip label="Substituted" variant="warning" />
                ) : null}
              </View>
              <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
                Sets: {execution.completedSetCount ?? 0}/{execution.setCount ?? 0} completed
                {(execution.skippedSetCount ?? 0) > 0
                  ? ` · ${execution.skippedSetCount} skipped`
                  : ''}
              </Text>
            </View>
          ))
        )}
      </HomeCard>

      <PrimaryButton
        label={launchButtonLabel(detail.status)}
        onPress={() => navigateToOccurrenceLaunch(planId, dayId, occurrenceId)}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  meta: {
    fontSize: 14,
  },
  executionRow: {
    gap: 4,
    marginBottom: 12,
  },
  executionName: {
    fontSize: 15,
    fontWeight: '600',
  },
  executionMeta: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
});
