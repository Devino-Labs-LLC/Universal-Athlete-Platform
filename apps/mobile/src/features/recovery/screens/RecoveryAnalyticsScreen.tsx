import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { todayDateOnly } from '@/src/core/date/dateOnly';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { BaselineMetricRow } from '@/src/features/recovery/components/BaselineMetricRow';
import { TrendRow } from '@/src/features/recovery/components/TrendRow';
import { useRecoveryOverview } from '@/src/features/recovery/hooks/useRecoveryOverview';
import { TrendDays } from '@/src/features/recovery/models/recoverySchemas';

const WINDOW_OPTIONS: TrendDays[] = [7, 14, 28];

export function RecoveryAnalyticsScreen() {
  const theme = useAppTheme();
  const today = todayDateOnly();
  const [trendDays, setTrendDays] = useState<TrendDays>(7);

  const overviewQuery = useRecoveryOverview(today, trendDays);
  const data = overviewQuery.data;

  if (overviewQuery.isLoading && !data) {
    return <LoadingView message="Loading analytics…" />;
  }

  if (overviewQuery.isError && !data) {
    const message = isApiError(overviewQuery.error)
      ? overviewQuery.error.message
      : 'Failed to load analytics';
    return <ErrorView message={message} onRetry={() => overviewQuery.refetch()} />;
  }

  if (!data) {
    return <LoadingView message="Loading analytics…" />;
  }

  return (
    <Screen
      scroll
      testID="recovery-analytics-screen"
      refreshing={overviewQuery.isFetching}
      onRefresh={() => overviewQuery.refetch()}>
      <Text style={[styles.title, { color: theme.colors.text }]}>Recovery analytics</Text>

      <View style={styles.windowRow}>
        {WINDOW_OPTIONS.map((days) => {
          const selected = trendDays === days;
          return (
            <Pressable
              key={days}
              accessibilityRole="button"
              accessibilityState={{ selected }}
              testID={`window-${days}`}
              onPress={() => setTrendDays(days)}
              style={[
                styles.windowChip,
                {
                  borderColor: selected ? theme.colors.primary : theme.colors.border,
                  backgroundColor: selected ? theme.colors.primary : theme.colors.surface,
                },
              ]}>
              <Text
                style={{
                  color: selected ? theme.colors.primaryText : theme.colors.text,
                  fontWeight: '600',
                }}>
                {days}D
              </Text>
            </Pressable>
          );
        })}
      </View>

      <HomeCard title={`Baseline comparison (${trendDays} days)`}>
        {data.deviations.length === 0 ? (
          <Text style={[styles.empty, { color: theme.colors.textMuted }]}>
            Not enough data for baseline comparison yet.
          </Text>
        ) : (
          data.deviations.map((deviation) => (
            <BaselineMetricRow key={deviation.metricType} deviation={deviation} />
          ))
        )}
      </HomeCard>

      <HomeCard title="Trends">
        {data.trends.length === 0 ? (
          <Text style={[styles.empty, { color: theme.colors.textMuted }]}>
            Trend data is not available yet.
          </Text>
        ) : (
          data.trends.map((trend) => <TrendRow key={trend.metricType} trend={trend} />)
        )}
      </HomeCard>

      {data.baselines.length > 0 ? (
        <HomeCard title="Baseline statistics">
          {data.baselines.map((baseline) => (
            <View key={`${baseline.metricType}-${baseline.windowDays}`} style={styles.baselineRow}>
              <Text style={[styles.metric, { color: theme.colors.text }]}>
                {baseline.metricType.replace(/_/g, ' ')}
              </Text>
              <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
                Mean {baseline.mean ?? '—'} · {baseline.observationCount} observations ·{' '}
                {baseline.dataSufficiency.replace(/_/g, ' ').toLowerCase()}
              </Text>
            </View>
          ))}
        </HomeCard>
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: {
    fontSize: 22,
    fontWeight: '700',
  },
  windowRow: {
    flexDirection: 'row',
    gap: 8,
  },
  windowChip: {
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  empty: {
    fontSize: 14,
  },
  baselineRow: {
    gap: 2,
  },
  metric: {
    fontSize: 14,
    fontWeight: '600',
  },
  meta: {
    fontSize: 13,
  },
});
