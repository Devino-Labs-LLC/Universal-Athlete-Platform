import { Pressable, StyleSheet, Text } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EmptyView } from '@/src/core/components/EmptyView';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { formatEnumLabel } from '@/src/features/profile/enumLabels';
import { useTrainingPlan } from '@/src/features/training/hooks/useTrainingPlan';
import { useWorkoutDays } from '@/src/features/training/hooks/useWorkoutDays';

interface TrainingPlanDetailScreenProps {
  planId: string;
}

export function TrainingPlanDetailScreen({ planId }: TrainingPlanDetailScreenProps) {
  const theme = useAppTheme();
  const planQuery = useTrainingPlan(planId);
  const daysQuery = useWorkoutDays(planId);

  const loading = (planQuery.isLoading || daysQuery.isLoading) && !planQuery.data && !daysQuery.data;
  const error = (planQuery.isError || daysQuery.isError) && !planQuery.data && !daysQuery.data;

  if (loading) {
    return <LoadingView message="Loading plan…" />;
  }

  if (error) {
    const err = planQuery.error ?? daysQuery.error;
    const message = isApiError(err) ? err.message : 'Failed to load training plan';
    return (
      <ErrorView
        message={message}
        onRetry={() => {
          planQuery.refetch();
          daysQuery.refetch();
        }}
      />
    );
  }

  const plan = planQuery.data;
  const days = [...(daysQuery.data ?? [])].sort((a, b) => a.displayOrder - b.displayOrder);

  if (!plan) {
    return <LoadingView message="Loading plan…" />;
  }

  return (
    <Screen scroll testID="training-plan-detail-screen">
      <HomeCard title={plan.name} subtitle={plan.description ?? undefined}>
        <StatusChip label={formatEnumLabel(plan.status)} variant="info" />
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          {formatEnumLabel(plan.type)} · {plan.startDate}
          {plan.endDate ? ` – ${plan.endDate}` : ''}
        </Text>
        {plan.scheduleStatus ? (
          <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
            Schedule: {formatEnumLabel(plan.scheduleStatus)}
          </Text>
        ) : null}
      </HomeCard>

      <Text style={[styles.sectionTitle, { color: theme.colors.text }]}>Workout days</Text>

      {days.length === 0 ? (
        <EmptyView message="No workout days in this plan." />
      ) : (
        days.map((day) => (
          <Pressable
            key={day.id}
            testID={`workout-day-${day.id}`}
            onPress={() =>
              router.push(`/(tabs)/training/plans/${planId}/days/${day.id}`)
            }>
            <HomeCard>
              <Text style={[styles.dayTitle, { color: theme.colors.text }]}>
                {day.displayOrder}. {day.title}
              </Text>
              {day.description ? (
                <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
                  {day.description}
                </Text>
              ) : null}
              <StatusChip label={formatEnumLabel(day.status)} variant="default" />
            </HomeCard>
          </Pressable>
        ))
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  meta: {
    fontSize: 14,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
  },
  dayTitle: {
    fontSize: 16,
    fontWeight: '600',
  },
});
