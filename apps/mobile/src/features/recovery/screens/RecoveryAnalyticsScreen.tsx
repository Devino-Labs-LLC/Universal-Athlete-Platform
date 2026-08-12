import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { CompactInfoRow } from '@/src/core/components/Surface';
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
      title="Recovery analytics"
      testID="recovery-analytics-screen"
      refreshing={overviewQuery.isFetching}
      onRefresh={() => overviewQuery.refetch()}>
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
                  borderColor: selected ? theme.colors.accentCyan : theme.colors.border,
                  backgroundColor: selected
                    ? theme.colors.accentCyanMuted
                    : theme.colors.surface,
                  minHeight: 44,
                },
              ]}>
              <Text
                style={{
                  color: selected ? theme.colors.accentCyan : theme.colors.text,
                  fontWeight: '700',
                }}>
                {days}D
              </Text>
            </Pressable>
          );
        })}
      </View>

      <HomeCard
        eyebrow="Baselines"
        title={`Baseline comparison (${trendDays} days)`}>
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

      <HomeCard eyebrow="Trends" title="Trends">
        {data.trends.length === 0 ? (
          <Text style={[styles.empty, { color: theme.colors.textMuted }]}>
            Trend data is not available yet.
          </Text>
        ) : (
          data.trends.map((trend) => <TrendRow key={trend.metricType} trend={trend} />)
        )}
      </HomeCard>

      {data.baselines.length > 0 ? (
        <HomeCard eyebrow="Statistics" title="Baseline statistics">
          {data.baselines.map((baseline) => (
            <View key={`${baseline.metricType}-${baseline.windowDays}`} style={styles.baselineRow}>
              <Text style={[styles.metric, { color: theme.colors.text }]}>
                {baseline.metricType.replace(/_/g, ' ')}
              </Text>
              <CompactInfoRow
                label="Mean"
                value={baseline.mean != null ? String(baseline.mean) : '—'}
              />
              <CompactInfoRow
                label="Observations"
                value={String(baseline.observationCount)}
              />
              <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
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
  windowRow: {
    flexDirection: 'row',
    gap: 8,
  },
  windowChip: {
    borderWidth: 1.5,
    borderRadius: 10,
    paddingHorizontal: 16,
    paddingVertical: 10,
    justifyContent: 'center',
    alignItems: 'center',
    minWidth: 64,
  },
  empty: {
    fontSize: 14,
  },
  baselineRow: {
    gap: 4,
    paddingVertical: 4,
  },
  metric: {
    fontSize: 14,
    fontWeight: '600',
  },
  meta: {
    fontSize: 13,
  },
});
