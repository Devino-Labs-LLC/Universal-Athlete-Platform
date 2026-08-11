import { StyleSheet, Text } from 'react-native';
import { useState } from 'react';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { todayDateOnly } from '@/src/core/date/dateOnly';
import { LoadHistoryRow } from '@/src/features/performance/components/LoadHistoryRow';
import { LoadModeSelector } from '@/src/features/performance/components/LoadModeSelector';
import { LoadRangeSelector } from '@/src/features/performance/components/LoadRangeSelector';
import { useTrainingLoadHistory } from '@/src/features/performance/hooks/useTrainingLoadHistory';
import { TrainingLoadGranularity } from '@/src/features/performance/models/performanceSchemas';
import { dateRangeForLoadHistory, LoadRangeKey } from '@/src/features/performance/utils/dateRanges';
import { performanceErrorMessage } from '@/src/features/performance/utils/performanceErrors';

export function TrainingLoadHistoryScreen() {
  const theme = useAppTheme();
  const [granularity, setGranularity] = useState<TrainingLoadGranularity>('WEEKLY');
  const [range, setRange] = useState<LoadRangeKey>('28D');
  const { startDate, endDate } = dateRangeForLoadHistory(range, todayDateOnly());

  const loadQuery = useTrainingLoadHistory({
    startDate,
    endDate,
    granularity,
    page: 0,
    size: 50,
  });

  if (loadQuery.isLoading && !loadQuery.data) {
    return <LoadingView message="Loading training load history…" />;
  }

  if (loadQuery.isError && !loadQuery.data) {
    const message = isApiError(loadQuery.error)
      ? loadQuery.error.message
      : performanceErrorMessage(loadQuery.error);
    return <ErrorView message={message} onRetry={() => loadQuery.refetch()} />;
  }

  const data = loadQuery.data;
  const occurrences = data?.occurrences ?? [];
  const dailySummaries = data?.dailySummaries ?? [];
  const weeklySummaries = data?.weeklySummaries ?? [];

  const hasRows =
    (granularity === 'OCCURRENCE' && occurrences.length > 0) ||
    (granularity === 'DAILY' && dailySummaries.length > 0) ||
    (granularity === 'WEEKLY' && weeklySummaries.length > 0);

  return (
    <Screen
      scroll
      testID="training-load-history-screen"
      refreshing={loadQuery.isFetching}
      onRefresh={() => loadQuery.refetch()}>
      <Text style={[styles.heading, { color: theme.colors.text }]}>Training load history</Text>

      <LoadModeSelector value={granularity} onChange={setGranularity} />
      <LoadRangeSelector value={range} onChange={setRange} />

      {!hasRows ? (
        <Text style={[styles.empty, { color: theme.colors.textMuted }]}>
          No training load data for this period.
        </Text>
      ) : null}

      {granularity === 'OCCURRENCE'
        ? occurrences.map((item) => (
            <LoadHistoryRow
              key={item.summary.workoutOccurrenceId}
              mode="OCCURRENCE"
              occurrence={item.summary}
            />
          ))
        : null}

      {granularity === 'DAILY'
        ? dailySummaries.map((daily) => (
            <LoadHistoryRow key={daily.date} mode="DAILY" daily={daily} />
          ))
        : null}

      {granularity === 'WEEKLY'
        ? weeklySummaries.map((weekly) => (
            <LoadHistoryRow key={weekly.weekStartDate} mode="WEEKLY" weekly={weekly} />
          ))
        : null}
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
    marginTop: 8,
  },
});
