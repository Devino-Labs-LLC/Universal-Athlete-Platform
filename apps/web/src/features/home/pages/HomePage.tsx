import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { isApiError } from '@/core/api/errors';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { AdaptationCard } from '@/features/home/components/AdaptationCard';
import { AthleteStateHero } from '@/features/home/components/AthleteStateHero';
import { HomeQuickActions } from '@/features/home/components/HomeQuickActions';
import { PrimaryWorkoutCard } from '@/features/home/components/PrimaryWorkoutCard';
import { ReadinessCard } from '@/features/home/components/ReadinessCard';
import { RecentPerformanceCard } from '@/features/home/components/RecentPerformanceCard';
import { RecommendationCard } from '@/features/home/components/RecommendationCard';
import { RecoveryCard } from '@/features/home/components/RecoveryCard';
import { TrainingLoadCard } from '@/features/home/components/TrainingLoadCard';
import { useDerivedStateMutations } from '@/features/home/hooks/useDerivedStateMutations';
import { useTodayDashboard } from '@/features/home/hooks/useTodayDashboard';
import styles from '@/features/home/pages/HomePage.module.scss';

export function HomePage() {
  const { account } = useAuthSession();
  const { snapshot } = useAthleteOnboarding();
  const todayQuery = useTodayDashboard();

  const data = todayQuery.data;
  // Date is absent while Today loads — hook must tolerate undefined (no render throw).
  const mutations = useDerivedStateMutations(data?.date);

  const pendingAction = mutations.athleteStateMutation.isPending
    ? 'state'
    : mutations.readinessMutation.isPending
      ? 'readiness'
      : mutations.recommendationMutation.isPending
        ? 'guidance'
        : null;

  if (todayQuery.isLoading && !data) {
    return <LoadingView message="Loading today dashboard…" />;
  }

  if (todayQuery.isError && !data) {
    const message = isApiError(todayQuery.error)
      ? todayQuery.error.message
      : 'Failed to load today dashboard';
    return <ErrorView message={message} onRetry={() => void todayQuery.refetch()} />;
  }

  if (!data) {
    return <LoadingView message="Loading today dashboard…" />;
  }

  return (
    <div className={styles.homeGrid}>
      <AthleteStateHero
        profileFirstName={snapshot.profile?.firstName}
        athleteDisplayName={data.athlete?.displayName}
        accountEmail={account?.email}
        date={data.date}
        readiness={data.readiness}
        recommendation={data.recommendation}
        recovery={data.recovery}
        hasWorkout={Boolean(data.training.primaryOccurrence)}
      />

      <div className={styles.spanWorkout}>
        <PrimaryWorkoutCard
          occurrence={data.training.primaryOccurrence}
          canStartWorkout={data.actions?.canStartWorkout}
          canContinueWorkout={data.actions?.canContinueWorkout}
        />
      </div>

      <div className={styles.spanSide}>
        <ReadinessCard
          readiness={data.readiness}
          checkInPresent={data.recovery.checkInPresent}
          snapshotPresent={data.athleteState?.snapshotPresent ?? false}
        />
      </div>

      <div className={styles.spanFour}>
        <RecommendationCard
          recommendation={data.recommendation}
          checkInPresent={data.recovery.checkInPresent}
          snapshotPresent={data.athleteState?.snapshotPresent ?? false}
          readinessPresent={data.readiness.readinessPresent}
        />
      </div>
      <div className={styles.spanFour}>
        <RecoveryCard recovery={data.recovery} />
      </div>
      <div className={styles.spanFour}>
        <TrainingLoadCard trainingLoad={data.trainingLoad} />
      </div>

      <div className={styles.spanSix}>
        <AdaptationCard
          adaptation={data.adaptation}
          linkedOccurrence={data.training.primaryOccurrence}
        />
      </div>
      <div className={styles.spanSix}>
        <RecentPerformanceCard records={data.recentPerformance} />
      </div>

      <div className={styles.spanFull}>
        <HomeQuickActions
          actions={data.actions}
          pendingAction={pendingAction}
          errorMessage={mutations.errorMessage}
          onGenerateState={() => mutations.athleteStateMutation.mutate()}
          onGenerateReadiness={() => mutations.readinessMutation.mutate()}
          onGenerateRecommendation={() => mutations.recommendationMutation.mutate()}
        />
      </div>
    </div>
  );
}
