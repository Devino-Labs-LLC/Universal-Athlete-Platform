import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useState } from 'react';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Button, PrimaryButton } from '@/src/core/components/PrimaryButton';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import {
  CompactInfoRow,
  MetricTile,
  ScoreRing,
} from '@/src/core/components/Surface';
import { isApiError } from '@/src/core/api/errors';
import { todayDateOnly } from '@/src/core/date/dateOnly';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import {
  readinessBandLabel,
  recommendationActionLabel,
} from '@/src/features/home/models/todayLabels';
import { useTodayDashboard } from '@/src/features/home/hooks/useTodayDashboard';
import { BaselineMetricRow } from '@/src/features/recovery/components/BaselineMetricRow';
import { InsightsStepList } from '@/src/features/recovery/components/InsightsStepList';
import { TrainingLoadContextCard } from '@/src/features/recovery/components/TrainingLoadContextCard';
import { TrendRow } from '@/src/features/recovery/components/TrendRow';
import { useRecoveryOverview } from '@/src/features/recovery/hooks/useRecoveryOverview';
import {
  bodyAreaLabel,
  bodySideLabel,
} from '@/src/features/recovery/models/recoveryLabels';
import { ratingLabelForMetric } from '@/src/features/recovery/models/ratingLabels';
import { TrendDays } from '@/src/features/recovery/models/recoverySchemas';
import { formatSleepDuration } from '@/src/features/recovery/utils/sleepDuration';
import { formatDateDisplay } from '@/src/features/home/utils/formatDateDisplay';

export function RecoveryOverviewScreen() {
  const theme = useAppTheme();
  const today = todayDateOnly();
  const [trendDays] = useState<TrendDays>(7);

  const overviewQuery = useRecoveryOverview(today, trendDays);
  const todayQuery = useTodayDashboard(today);
  const data = overviewQuery.data;

  const refreshing = overviewQuery.isFetching || todayQuery.isFetching;

  const onRefresh = () => {
    void overviewQuery.refetch();
    void todayQuery.refetch();
  };

  if (overviewQuery.isLoading && !data) {
    return <LoadingView message="Loading recovery overview…" />;
  }

  if (overviewQuery.isError && !data) {
    const message = isApiError(overviewQuery.error)
      ? overviewQuery.error.message
      : 'Failed to load recovery overview';
    return <ErrorView message={message} onRetry={() => overviewQuery.refetch()} />;
  }

  if (!data) {
    return <LoadingView message="Loading recovery overview…" />;
  }

  const checkIn = data.checkIn;
  const readinessScore =
    data.readiness?.readinessScore != null && !Number.isNaN(Number(data.readiness.readinessScore))
      ? Number(data.readiness.readinessScore)
      : null;

  return (
    <Screen
      scroll
      title="Recovery"
      description={formatDateDisplay(data.date)}
      testID="recovery-overview-screen"
      refreshing={refreshing}
      onRefresh={onRefresh}>
      <HomeCard
        testID="recovery-check-in-section"
        eyebrow="Today"
        title="Today's check-in">
        {data.checkInPresent && checkIn ? (
          <>
            <StatusChip label={checkIn.completeness.replace(/_/g, ' ')} variant="success" />
            <View style={styles.metricRow}>
              <MetricTile
                label="Fatigue"
                value={
                  checkIn.fatigue != null
                    ? ratingLabelForMetric('fatigue', checkIn.fatigue)
                    : null
                }
              />
              <MetricTile
                label="Soreness"
                value={
                  checkIn.muscleSoreness != null
                    ? ratingLabelForMetric('muscleSoreness', checkIn.muscleSoreness)
                    : null
                }
              />
              <MetricTile
                label="Sleep"
                value={
                  checkIn.sleepDurationMinutes != null
                    ? formatSleepDuration(checkIn.sleepDurationMinutes)
                    : null
                }
              />
            </View>
            <PrimaryButton
              label="Update Check In"
              onPress={() => router.push('/(tabs)/recovery/check-in')}
            />
          </>
        ) : (
          <>
            <Text style={[styles.body, { color: theme.colors.textMuted }]}>
              Log how you feel to unlock personalized insights.
            </Text>
            <PrimaryButton
              label="Check In"
              onPress={() => router.push('/(tabs)/recovery/check-in')}
            />
          </>
        )}
      </HomeCard>

      <InsightsStepList
        date={data.date}
        overviewCheckInPresent={data.checkInPresent}
        actions={todayQuery.data?.actions}
        athleteState={todayQuery.data?.athleteState}
        readinessPresent={todayQuery.data?.readiness?.readinessPresent ?? data.readinessPresent}
        recommendationPresent={
          todayQuery.data?.recommendation?.recommendationPresent ?? data.recommendationPresent
        }
      />

      {data.readinessPresent && data.readiness ? (
        <HomeCard testID="recovery-readiness-summary" eyebrow="Athlete state" title="Readiness">
          <StatusChip label={readinessBandLabel(data.readiness.readinessBand)} variant="info" />
          <ScoreRing score={readinessScore} label="Score" />
          <PrimaryButton
            label="View readiness details"
            onPress={() =>
              router.push(`/(tabs)/recovery/readiness/${data.readiness!.readinessAssessmentId}`)
            }
          />
        </HomeCard>
      ) : null}

      {data.recommendationPresent && data.recommendation ? (
        <HomeCard
          testID="recovery-guidance-summary"
          eyebrow="Guidance"
          title="Training guidance">
          <StatusChip
            label={recommendationActionLabel(data.recommendation.overallAction)}
            variant="info"
          />
          <PrimaryButton
            label="View guidance details"
            onPress={() =>
              router.push(`/(tabs)/recovery/guidance/${data.recommendation!.recommendationId}`)
            }
          />
        </HomeCard>
      ) : null}

      {data.deviations.length > 0 ? (
        <HomeCard testID="recovery-baselines-section" eyebrow="Baselines" title="Personal baseline">
          {data.deviations.map((deviation) => (
            <BaselineMetricRow key={deviation.metricType} deviation={deviation} />
          ))}
          <Button
            variant="secondary"
            label="View analytics"
            onPress={() => router.push('/(tabs)/recovery/analytics')}
          />
        </HomeCard>
      ) : null}

      {data.trends.length > 0 ? (
        <HomeCard testID="recovery-trends-section" eyebrow="Trends" title="Trends">
          {data.trends.map((trend) => (
            <TrendRow key={trend.metricType} trend={trend} />
          ))}
        </HomeCard>
      ) : null}

      {data.discomfort.length > 0 ? (
        <HomeCard testID="recovery-discomfort-section" eyebrow="Body" title="Discomfort">
          {data.discomfort.map((item) => (
            <CompactInfoRow
              key={`${item.bodyArea}-${item.bodySide}-${item.orderIndex}`}
              label={`${bodyAreaLabel(item.bodyArea)} (${bodySideLabel(item.bodySide)})`}
              value={`Intensity ${item.intensity}${item.notes ? ` — ${item.notes}` : ''}`}
            />
          ))}
        </HomeCard>
      ) : null}

      {data.trainingLoadContext ? (
        <TrainingLoadContextCard context={data.trainingLoadContext} />
      ) : null}

      <View style={styles.links}>
        <Button
          variant="secondary"
          label="History"
          onPress={() => router.push('/(tabs)/recovery/history')}
        />
        <Button
          variant="secondary"
          label="Analytics"
          onPress={() => router.push('/(tabs)/recovery/analytics')}
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  body: {
    fontSize: 15,
  },
  metricRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  links: {
    gap: 8,
  },
});
