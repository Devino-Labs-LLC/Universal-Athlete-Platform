import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { ExerciseHistoryRow } from '@/src/features/performance/components/ExerciseHistoryRow';
import { PersonalRecordCard } from '@/src/features/performance/components/PersonalRecordCard';
import { useExercisePerformanceHistory } from '@/src/features/performance/hooks/useExercisePerformanceHistory';
import { useExercisePersonalRecords } from '@/src/features/performance/hooks/useExercisePersonalRecords';
import { performanceErrorMessage } from '@/src/features/performance/utils/performanceErrors';

interface ExercisePerformanceScreenProps {
  exercisePerformanceKey: string;
}

export function ExercisePerformanceScreen({ exercisePerformanceKey }: ExercisePerformanceScreenProps) {
  const theme = useAppTheme();
  const recordsQuery = useExercisePersonalRecords(exercisePerformanceKey);
  const historyQuery = useExercisePerformanceHistory(exercisePerformanceKey);

  const firstPage = historyQuery.data?.pages[0];
  const exerciseName = firstPage?.exerciseName ?? 'Exercise';
  const historyEntries = historyQuery.data?.pages.flatMap((page) => page.entries) ?? [];

  if (
    (recordsQuery.isLoading && !recordsQuery.data) ||
    (historyQuery.isLoading && !historyQuery.data)
  ) {
    return <LoadingView message="Loading exercise performance…" />;
  }

  if (
    (recordsQuery.isError && !recordsQuery.data) ||
    (historyQuery.isError && !historyQuery.data)
  ) {
    const error = recordsQuery.error ?? historyQuery.error;
    const message = isApiError(error)
      ? error.message
      : performanceErrorMessage(error);
    return (
      <ErrorView
        message={message}
        onRetry={() => {
          void recordsQuery.refetch();
          void historyQuery.refetch();
        }}
      />
    );
  }

  const records = recordsQuery.data ?? [];

  return (
    <Screen
      scroll
      testID="exercise-performance-screen"
      refreshing={recordsQuery.isFetching || historyQuery.isFetching}
      onRefresh={() => {
        void recordsQuery.refetch();
        void historyQuery.refetch();
      }}>
      <Text style={[styles.heading, { color: theme.colors.text }]}>{exerciseName}</Text>

      <HomeCard title="Current personal records">
        {records.length === 0 ? (
          <Text style={[styles.empty, { color: theme.colors.textMuted }]}>
            No personal records for this exercise yet.
          </Text>
        ) : (
          <View style={styles.list}>
            {records.map((record) => (
              <PersonalRecordCard key={record.id} record={record} />
            ))}
          </View>
        )}
      </HomeCard>

      <HomeCard title="Performance history">
        {historyEntries.length === 0 ? (
          <Text style={[styles.empty, { color: theme.colors.textMuted }]}>
            No completed executions recorded yet.
          </Text>
        ) : (
          historyEntries.map((entry) => <ExerciseHistoryRow key={entry.executionId} entry={entry} />)
        )}
        {historyQuery.hasNextPage ? (
          <PrimaryButton
            label={historyQuery.isFetchingNextPage ? 'Loading…' : 'Load more'}
            onPress={() => historyQuery.fetchNextPage()}
          />
        ) : null}
      </HomeCard>
    </Screen>
  );
}

const styles = StyleSheet.create({
  heading: {
    fontSize: 22,
    fontWeight: '700',
  },
  empty: {
    fontSize: 14,
  },
  list: {
    gap: 12,
  },
});
