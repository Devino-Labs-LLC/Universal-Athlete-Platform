import { StyleSheet, View } from 'react-native';
import { router } from 'expo-router';

import { Button } from '@/src/core/components/PrimaryButton';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { todayDateOnly } from '@/src/core/date/dateOnly';
import { LoadSnapshotCard } from '@/src/features/performance/components/LoadSnapshotCard';
import { ProgressSummaryCard } from '@/src/features/performance/components/ProgressSummaryCard';
import { RecentRecordsSection } from '@/src/features/performance/components/RecentRecordsSection';
import { useRecentPersonalRecords } from '@/src/features/performance/hooks/useRecentPersonalRecords';
import { useTrainingLoadHistory } from '@/src/features/performance/hooks/useTrainingLoadHistory';
import { composeAthleteProgress } from '@/src/features/performance/models/progressComposition';
import { dateRangeForLoadHistory } from '@/src/features/performance/utils/dateRanges';
import { performanceErrorMessage } from '@/src/features/performance/utils/performanceErrors';
import { useRecoveryHistory } from '@/src/features/recovery/hooks/useRecoveryHistory';
import { useTrainingOverview } from '@/src/features/training/hooks/useTrainingOverview';

export function PerformanceOverviewScreen() {
  const { startDate, endDate } = dateRangeForLoadHistory('28D', todayDateOnly());

  const recentQuery = useRecentPersonalRecords(30, 5);
  const loadQuery = useTrainingLoadHistory({
    startDate,
    endDate,
    granularity: 'WEEKLY',
    page: 0,
    size: 4,
  });
  const overviewQuery = useTrainingOverview();
  const recoveryQuery = useRecoveryHistory(startDate, endDate, true);

  const refreshing =
    recentQuery.isFetching ||
    loadQuery.isFetching ||
    overviewQuery.isFetching ||
    recoveryQuery.isFetching;
  const onRefresh = () => {
    void recentQuery.refetch();
    void loadQuery.refetch();
    void overviewQuery.refetch();
    void recoveryQuery.refetch();
  };

  if (recentQuery.isLoading && !recentQuery.data && loadQuery.isLoading && !loadQuery.data) {
    return <LoadingView message="Loading performance…" />;
  }

  if (recentQuery.isError && !recentQuery.data && loadQuery.isError && !loadQuery.data) {
    const error = recentQuery.error ?? loadQuery.error;
    const message = isApiError(error)
      ? error.message
      : performanceErrorMessage(error);
    return <ErrorView message={message} onRetry={onRefresh} />;
  }

  const recentRecords = recentQuery.data ?? [];
  const weeklySummaries = loadQuery.data?.weeklySummaries ?? [];
  const completedSessions = overviewQuery.data?.recentCompletedSessions ?? [];
  const weeklyLoad = overviewQuery.data?.weeklyLoadSummary;
  const recoveryDays = recoveryQuery.data?.days ?? [];
  const ratedSessionCount = weeklySummaries.reduce(
    (sum, week) => sum + (week.ratedOccurrenceCount ?? 0),
    0,
  );
  const progress = composeAthleteProgress({
    completedSessionCount: completedSessions.length,
    weeklyTrainingDays: weeklyLoad?.trainingDays ?? null,
    recentPersonalRecordCount: recentRecords.length,
    recoveryCheckInCount: recoveryDays.length,
    ratedSessionCount,
    weeklyLoadPointCount: weeklySummaries.length,
  });

  return (
    <Screen
      scroll
      title="Performance"
      description="Progress & insights from real sessions"
      testID="performance-overview-screen"
      refreshing={refreshing}
      onRefresh={onRefresh}>
      <ProgressSummaryCard progress={progress} />
      <RecentRecordsSection records={recentRecords} loading={recentQuery.isLoading} />
      <LoadSnapshotCard summaries={weeklySummaries} loading={loadQuery.isLoading} />

      <View style={styles.links}>
        <Button
          variant="secondary"
          label="View all records"
          onPress={() => router.push('/(tabs)/performance/records')}
        />
        <Button
          variant="secondary"
          label="Load history"
          onPress={() => router.push('/(tabs)/performance/load')}
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  links: {
    gap: 8,
  },
});
