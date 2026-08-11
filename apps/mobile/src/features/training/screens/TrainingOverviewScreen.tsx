import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EmptyView } from '@/src/core/components/EmptyView';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { occurrenceStatusLabel } from '@/src/features/home/models/todayLabels';
import { formatEnumLabel } from '@/src/features/profile/enumLabels';
import { TrainingPlanCard } from '@/src/features/training/components/TrainingPlanCard';
import { WeeklyLoadSummaryCard } from '@/src/features/training/components/WeeklyLoadSummaryCard';
import { WorkoutOccurrenceCard } from '@/src/features/training/components/WorkoutOccurrenceCard';
import { useTrainingOverview } from '@/src/features/training/hooks/useTrainingOverview';
import {
  OverviewAdaptation,
  OverviewOccurrence,
  TrainingOverview,
} from '@/src/features/training/models/browseSchemas';
import {
  navigateToAdaptationProposal,
} from '@/src/features/adaptation/utils/proposalNavigation';
import {
  navigateToOccurrenceLaunch,
  navigateToOccurrenceDetail,
} from '@/src/features/training/utils/trainingNavigation';

function findNextOccurrence(overview: TrainingOverview): OverviewOccurrence | null {
  const upcoming = overview.upcomingOccurrences ?? [];
  const inProgress = upcoming.find((item) => item.status === 'IN_PROGRESS');
  if (inProgress) {
    return inProgress;
  }
  return upcoming.find((item) => item.status === 'SCHEDULED') ?? upcoming[0] ?? null;
}

function resolveAdaptationRoute(
  adaptation: OverviewAdaptation,
  overview: TrainingOverview,
): { planId: string; dayId: string; occurrenceId: string } | null {
  const candidates = [
    ...(overview.upcomingOccurrences ?? []),
    ...(overview.recentCompletedSessions ?? []).map((session) => ({
      occurrenceId: session.occurrenceId,
      trainingPlanId: session.trainingPlanId,
      workoutDayId: session.workoutDayId,
      trainingPlanName: session.trainingPlanName,
      workoutDayName: session.workoutDayName,
      scheduledDate: session.scheduledDate,
      status: 'COMPLETED',
      exerciseCount: session.exerciseCount,
      completedExerciseCount: session.completedExerciseCount,
    })),
  ];

  const match = candidates.find((item) => item.occurrenceId === adaptation.occurrenceId);
  if (!match) {
    return null;
  }

  return {
    planId: match.trainingPlanId,
    dayId: match.workoutDayId,
    occurrenceId: match.occurrenceId,
  };
}

export function TrainingOverviewScreen() {
  const theme = useAppTheme();
  const overviewQuery = useTrainingOverview();
  const data = overviewQuery.data;

  if (overviewQuery.isLoading && !data) {
    return <LoadingView message="Loading training overview…" />;
  }

  if (overviewQuery.isError && !data) {
    const message = isApiError(overviewQuery.error)
      ? overviewQuery.error.message
      : 'Failed to load training overview';
    return <ErrorView message={message} onRetry={() => overviewQuery.refetch()} />;
  }

  if (!data) {
    return <LoadingView message="Loading training overview…" />;
  }

  const nextOccurrence = findNextOccurrence(data);
  const upcoming = data.upcomingOccurrences ?? [];
  const activePlans = data.activePlans ?? [];
  const completed = data.recentCompletedSessions ?? [];
  const adaptations = data.outstandingAdaptationProposals ?? [];

  return (
    <Screen
      scroll
      testID="training-overview-screen"
      refreshing={overviewQuery.isFetching}
      onRefresh={() => overviewQuery.refetch()}>
      {nextOccurrence ? (
        <WorkoutOccurrenceCard
          testID="next-workout-card"
          occurrence={nextOccurrence}
          onPress={() =>
            navigateToOccurrenceDetail(
              nextOccurrence.trainingPlanId,
              nextOccurrence.workoutDayId,
              nextOccurrence.occurrenceId,
            )
          }
          onPrimaryAction={() =>
            navigateToOccurrenceLaunch(
              nextOccurrence.trainingPlanId,
              nextOccurrence.workoutDayId,
              nextOccurrence.occurrenceId,
            )
          }
        />
      ) : (
        <EmptyView message="No upcoming workouts scheduled." />
      )}

      <PrimaryButton label="Open Calendar" onPress={() => router.push('/(tabs)/training/calendar')} />

      <HomeCard title="Upcoming" testID="upcoming-section">
        {upcoming.length === 0 ? (
          <Text style={[styles.empty, { color: theme.colors.textMuted }]}>
            No upcoming workouts in the next few weeks.
          </Text>
        ) : (
          upcoming.map((occurrence) => (
            <WorkoutOccurrenceCard
              key={occurrence.occurrenceId}
              occurrence={occurrence}
              onPress={() =>
                navigateToOccurrenceDetail(
                  occurrence.trainingPlanId,
                  occurrence.workoutDayId,
                  occurrence.occurrenceId,
                )
              }
              onPrimaryAction={() =>
                navigateToOccurrenceLaunch(
                  occurrence.trainingPlanId,
                  occurrence.workoutDayId,
                  occurrence.occurrenceId,
                )
              }
            />
          ))
        )}
      </HomeCard>

      <HomeCard title="Active plans" testID="active-plans-section">
        {activePlans.length === 0 ? (
          <Text style={[styles.empty, { color: theme.colors.textMuted }]}>No active training plans.</Text>
        ) : (
          activePlans.map((plan) => <TrainingPlanCard key={plan.trainingPlanId} plan={plan} />)
        )}
      </HomeCard>

      <HomeCard title="Recently completed" testID="completed-section">
        {completed.length === 0 ? (
          <Text style={[styles.empty, { color: theme.colors.textMuted }]}>
            No recently completed sessions.
          </Text>
        ) : (
          completed.map((session) => (
            <WorkoutOccurrenceCard
              key={session.occurrenceId}
              occurrence={{
                occurrenceId: session.occurrenceId,
                trainingPlanId: session.trainingPlanId,
                trainingPlanName: session.trainingPlanName,
                workoutDayId: session.workoutDayId,
                workoutDayName: session.workoutDayName,
                scheduledDate: session.scheduledDate,
                status: 'COMPLETED',
                exerciseCount: session.exerciseCount,
                completedExerciseCount: session.completedExerciseCount,
              }}
              onPress={() =>
                navigateToOccurrenceDetail(
                  session.trainingPlanId,
                  session.workoutDayId,
                  session.occurrenceId,
                )
              }
              onPrimaryAction={() =>
                navigateToOccurrenceLaunch(
                  session.trainingPlanId,
                  session.workoutDayId,
                  session.occurrenceId,
                )
              }
              primaryActionLabel="Review"
            />
          ))
        )}
      </HomeCard>

      {data.weeklyLoadSummary ? (
        <WeeklyLoadSummaryCard load={data.weeklyLoadSummary} />
      ) : null}

      {adaptations.length > 0 ? (
        <HomeCard title="Outstanding adaptations" testID="adaptations-section">
          {adaptations.map((adaptation) => {
            const route = resolveAdaptationRoute(adaptation, data);
            return (
              <View key={adaptation.adaptationProposalId} style={styles.adaptationRow}>
                <View style={styles.adaptationHeader}>
                  <StatusChip
                    label={formatEnumLabel(adaptation.status)}
                    variant="warning"
                  />
                  <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
                    {adaptation.unresolvedCount} unresolved
                  </Text>
                </View>
                {route ? (
                  <PrimaryButton
                    label="Review adaptation"
                    onPress={() =>
                      navigateToAdaptationProposal(
                        route.planId,
                        route.dayId,
                        route.occurrenceId,
                        adaptation.adaptationProposalId,
                      )
                    }
                  />
                ) : (
                  <PrimaryButton
                    label="Find in calendar"
                    onPress={() => router.push('/(tabs)/training/calendar')}
                  />
                )}
              </View>
            );
          })}
        </HomeCard>
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  empty: {
    fontSize: 14,
  },
  meta: {
    fontSize: 14,
  },
  adaptationRow: {
    gap: 8,
  },
  adaptationHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
});
