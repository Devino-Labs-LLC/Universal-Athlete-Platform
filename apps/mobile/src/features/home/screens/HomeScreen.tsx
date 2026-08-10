import { StyleSheet, Text } from 'react-native';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAthleteOnboarding } from '@/src/app/providers/AthleteOnboardingProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { AdaptationCard } from '@/src/features/home/components/AdaptationCard';
import { HomeQuickActions } from '@/src/features/home/components/HomeQuickActions';
import { HomeSkeleton } from '@/src/features/home/components/HomeSkeleton';
import { PrimaryWorkoutCard } from '@/src/features/home/components/PrimaryWorkoutCard';
import { ReadinessCard } from '@/src/features/home/components/ReadinessCard';
import { RecentPerformanceCard } from '@/src/features/home/components/RecentPerformanceCard';
import { RecommendationCard } from '@/src/features/home/components/RecommendationCard';
import { RecoveryCard } from '@/src/features/home/components/RecoveryCard';
import { TodayHeader } from '@/src/features/home/components/TodayHeader';
import { TrainingLoadCard } from '@/src/features/home/components/TrainingLoadCard';
import { useDerivedStateMutations } from '@/src/features/home/hooks/useDerivedStateMutations';
import { useTodayDashboard } from '@/src/features/home/hooks/useTodayDashboard';
import { buildGreeting } from '@/src/features/home/utils/greeting';

export function HomeScreen() {
  const theme = useAppTheme();
  const { account } = useAuthSession();
  const { snapshot } = useAthleteOnboarding();
  const todayQuery = useTodayDashboard();

  const data = todayQuery.data;
  const mutations = useDerivedStateMutations(data?.date ?? '');

  const pendingAction = mutations.athleteStateMutation.isPending
    ? 'state'
    : mutations.readinessMutation.isPending
      ? 'readiness'
      : mutations.recommendationMutation.isPending
        ? 'guidance'
        : null;

  if (todayQuery.isLoading && !data) {
    return (
      <Screen scroll>
        <HomeSkeleton />
      </Screen>
    );
  }

  if (todayQuery.isError && !data) {
    const message = isApiError(todayQuery.error)
      ? todayQuery.error.message
      : 'Failed to load today dashboard';
    return <ErrorView message={message} onRetry={() => todayQuery.refetch()} />;
  }

  if (!data) {
    return <LoadingView message="Loading today dashboard…" />;
  }

  const greeting = buildGreeting({
    profileFirstName: snapshot.profile?.firstName,
    athleteDisplayName: data.athlete?.displayName,
    accountEmail: account?.email,
  });

  const primaryOccurrence = data.training.primaryOccurrence;
  const inProgressPrimary = primaryOccurrence?.status === 'IN_PROGRESS';

  const workoutCard = (
    <PrimaryWorkoutCard
      occurrence={primaryOccurrence}
      canStartWorkout={data.actions?.canStartWorkout}
      canContinueWorkout={data.actions?.canContinueWorkout}
      dominant={inProgressPrimary}
    />
  );

  return (
    <Screen
      scroll
      testID="home-screen-scroll"
      refreshing={todayQuery.isFetching}
      onRefresh={() => todayQuery.refetch()}>
      <TodayHeader greeting={greeting} date={data.date} />

      {inProgressPrimary ? workoutCard : null}

      <HomeQuickActions
        actions={data.actions}
        onGenerateDailyState={() => mutations.athleteStateMutation.mutate()}
        onCalculateReadiness={() => mutations.readinessMutation.mutate()}
        onGenerateGuidance={() => mutations.recommendationMutation.mutate()}
        pendingAction={pendingAction}
      />

      {mutations.errorMessage ? (
        <Text style={[styles.error, { color: theme.colors.danger }]} testID="home-mutation-error">
          {mutations.errorMessage}
        </Text>
      ) : null}

      {!inProgressPrimary ? workoutCard : null}

      <ReadinessCard readiness={data.readiness} />
      <RecommendationCard recommendation={data.recommendation} />
      <RecoveryCard
        recovery={data.recovery}
        canCreateRecoveryCheckIn={data.actions?.canCreateRecoveryCheckIn}
        canUpdateRecoveryCheckIn={data.actions?.canUpdateRecoveryCheckIn}
      />

      {data.trainingLoad?.loadPresent ? (
        <TrainingLoadCard trainingLoad={data.trainingLoad} />
      ) : null}

      {data.adaptation ? <AdaptationCard adaptation={data.adaptation} /> : null}

      <RecentPerformanceCard records={data.recentPerformance ?? []} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  error: {
    color: '#DC2626',
    fontSize: 14,
  },
});
