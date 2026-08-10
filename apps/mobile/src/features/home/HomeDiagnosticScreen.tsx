import { StyleSheet, Text } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EmptyView } from '@/src/core/components/EmptyView';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { useTodayQuery } from '@/src/features/home/useTodayQuery';
import { isApiError } from '@/src/core/api/errors';

export function HomeDiagnosticScreen() {
  const theme = useAppTheme();
  const todayQuery = useTodayQuery();

  if (todayQuery.isLoading) {
    return <LoadingView message="Loading today dashboard…" />;
  }

  if (todayQuery.isError) {
    const message = isApiError(todayQuery.error)
      ? todayQuery.error.message
      : 'Failed to load today dashboard';
    return <ErrorView message={message} onRetry={() => todayQuery.refetch()} />;
  }

  const data = todayQuery.data;
  if (!data) {
    return <EmptyView message="No dashboard data available." />;
  }

  return (
    <Screen title="Home" scroll>
      <Text style={[styles.label, { color: theme.colors.textMuted }]}>Date</Text>
      <Text style={[styles.value, { color: theme.colors.text }]}>{data.date}</Text>

      <Text style={[styles.label, { color: theme.colors.textMuted }]}>Recovery check-in</Text>
      <Text style={[styles.value, { color: theme.colors.text }]}>
        {data.recovery.checkInPresent ? 'Present' : 'Missing'}
      </Text>

      <Text style={[styles.label, { color: theme.colors.textMuted }]}>Readiness</Text>
      <Text style={[styles.value, { color: theme.colors.text }]}>
        {data.readiness.readinessPresent
          ? `${data.readiness.readinessBand ?? 'unknown'} (${data.readiness.readinessScore ?? 'n/a'})`
          : 'Not available'}
      </Text>

      <Text style={[styles.label, { color: theme.colors.textMuted }]}>Recommendation</Text>
      <Text style={[styles.value, { color: theme.colors.text }]}>
        {data.recommendation.recommendationPresent
          ? `${data.recommendation.overallAction ?? 'n/a'} (${data.recommendation.recommendationStatus ?? 'n/a'})`
          : 'Not available'}
      </Text>

      <Text style={[styles.label, { color: theme.colors.textMuted }]}>Scheduled workouts</Text>
      <Text style={[styles.value, { color: theme.colors.text }]}>
        {data.training.scheduledOccurrenceCount}
      </Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  label: {
    fontSize: 13,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.4,
  },
  value: {
    fontSize: 18,
    marginBottom: 8,
  },
});
