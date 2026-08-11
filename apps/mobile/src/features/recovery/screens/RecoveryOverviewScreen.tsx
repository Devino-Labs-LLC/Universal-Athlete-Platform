import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useState } from 'react';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
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

  return (
    <Screen scroll testID="recovery-overview-screen" refreshing={refreshing} onRefresh={onRefresh}>
      <Text style={[styles.heading, { color: theme.colors.text }]}>Recovery</Text>
      <Text style={[styles.subheading, { color: theme.colors.textMuted }]}>
        {formatDateDisplay(data.date)}
      </Text>

      <HomeCard testID="recovery-check-in-section" title="Today's check-in">
        {data.checkInPresent && checkIn ? (
          <>
            <StatusChip label={checkIn.completeness.replace(/_/g, ' ')} variant="success" />
            <View style={styles.metrics}>
              {checkIn.fatigue != null ? (
                <Text style={[styles.metric, { color: theme.colors.text }]}>
                  Fatigue: {ratingLabelForMetric('fatigue', checkIn.fatigue)}
                </Text>
              ) : null}
              {checkIn.muscleSoreness != null ? (
                <Text style={[styles.metric, { color: theme.colors.text }]}>
                  Soreness: {ratingLabelForMetric('muscleSoreness', checkIn.muscleSoreness)}
                </Text>
              ) : null}
              {checkIn.sleepDurationMinutes != null ? (
                <Text style={[styles.metric, { color: theme.colors.text }]}>
                  Sleep: {formatSleepDuration(checkIn.sleepDurationMinutes)}
                </Text>
              ) : null}
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
        <HomeCard testID="recovery-readiness-summary" title="Readiness">
          <StatusChip label={readinessBandLabel(data.readiness.readinessBand)} variant="info" />
          {data.readiness.readinessScore != null ? (
            <Text style={[styles.score, { color: theme.colors.text }]}>
              {Math.round(Number(data.readiness.readinessScore))}
            </Text>
          ) : null}
          <PrimaryButton
            label="View readiness details"
            onPress={() =>
              router.push(`/(tabs)/recovery/readiness/${data.readiness!.readinessAssessmentId}`)
            }
          />
        </HomeCard>
      ) : null}

      {data.recommendationPresent && data.recommendation ? (
        <HomeCard testID="recovery-guidance-summary" title="Training guidance">
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
        <HomeCard testID="recovery-baselines-section" title="Personal baseline">
          {data.deviations.map((deviation) => (
            <BaselineMetricRow key={deviation.metricType} deviation={deviation} />
          ))}
          <PrimaryButton
            label="View analytics"
            onPress={() => router.push('/(tabs)/recovery/analytics')}
          />
        </HomeCard>
      ) : null}

      {data.trends.length > 0 ? (
        <HomeCard testID="recovery-trends-section" title="Trends">
          {data.trends.map((trend) => (
            <TrendRow key={trend.metricType} trend={trend} />
          ))}
        </HomeCard>
      ) : null}

      {data.discomfort.length > 0 ? (
        <HomeCard testID="recovery-discomfort-section" title="Discomfort">
          {data.discomfort.map((item) => (
            <Text
              key={`${item.bodyArea}-${item.bodySide}-${item.orderIndex}`}
              style={[styles.metric, { color: theme.colors.text }]}>
              {bodyAreaLabel(item.bodyArea)} ({bodySideLabel(item.bodySide)}): intensity{' '}
              {item.intensity}
              {item.notes ? ` — ${item.notes}` : ''}
            </Text>
          ))}
        </HomeCard>
      ) : null}

      {data.trainingLoadContext ? (
        <TrainingLoadContextCard context={data.trainingLoadContext} />
      ) : null}

      <View style={styles.links}>
        <PrimaryButton label="History" onPress={() => router.push('/(tabs)/recovery/history')} />
        <PrimaryButton
          label="Analytics"
          onPress={() => router.push('/(tabs)/recovery/analytics')}
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  heading: {
    fontSize: 24,
    fontWeight: '700',
  },
  subheading: {
    fontSize: 14,
  },
  body: {
    fontSize: 15,
  },
  metrics: {
    gap: 4,
  },
  metric: {
    fontSize: 14,
  },
  score: {
    fontSize: 28,
    fontWeight: '700',
  },
  links: {
    gap: 8,
  },
});
