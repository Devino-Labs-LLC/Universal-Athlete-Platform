import { Link, useParams } from 'react-router-dom';

import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { OccurrencePerformanceSummary } from '@/features/performance/components/OccurrencePerformanceSummary';
import { useOccurrencePerformance } from '@/features/performance/hooks/useOccurrencePerformance';
import { performanceErrorMessage } from '@/features/performance/models/errors';

export function SessionPerformancePage() {
  const { planId, dayId, occurrenceId } = useParams<{ planId: string; dayId: string; occurrenceId: string }>();
  const performanceQuery = useOccurrencePerformance(planId, dayId, occurrenceId);

  if (performanceQuery.isLoading) {
    return <LoadingView message="Loading session performance…" />;
  }

  if (performanceQuery.isError) {
    return (
      <ErrorView message={performanceErrorMessage(performanceQuery.error)} onRetry={() => performanceQuery.refetch()} />
    );
  }

  const performance = performanceQuery.data!;

  return (
    <Page
      title="Session performance"
      description={`Session on ${performance.scheduledDate}.`}
      actions={
        <Link to={`/app/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}`}>
          View training session
        </Link>
      }
    >
      <section className="card">
        {performance.exercises.length === 0 ? (
          <EmptyView title="No performance data yet" message="This session has no completed exercises to summarize." />
        ) : (
          <OccurrencePerformanceSummary performance={performance} />
        )}
      </section>
    </Page>
  );
}
