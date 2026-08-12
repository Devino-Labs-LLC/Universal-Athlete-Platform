import { Link, useParams } from 'react-router-dom';

import { Badge } from '@/core/components/Badge';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { OccurrencePerformanceSummary } from '@/features/performance/components/OccurrencePerformanceSummary';
import { useOccurrencePerformance } from '@/features/performance/hooks/useOccurrencePerformance';
import { performanceErrorMessage } from '@/features/performance/models/errors';
import surfaces from '@/features/performance/styles/performanceSurfaces.module.scss';
import { performanceStatusBadgeTone } from '@/features/performance/utils/performanceVisual';

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
      width="wide"
      actions={
        <Link
          className={surfaces.panelLink}
          to={`/app/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}`}
        >
          View training session
        </Link>
      }
    >
      <div className={surfaces.hub}>
        <section className={surfaces.hero} aria-labelledby="session-hero-heading">
          <div className={surfaces.heroCopy}>
            <p className={surfaces.eyebrow} id="session-hero-heading">
              Session performance
            </p>
            <h2 className={surfaces.heroTitle}>{performance.scheduledDate}</h2>
            <div className={surfaces.metaRow}>
              <Badge tone={performanceStatusBadgeTone(performance.status)}>{performance.status}</Badge>
              {performance.completedAt ? (
                <span className={surfaces.metaText}>Completed {performance.completedAt.slice(0, 16).replace('T', ' ')}</span>
              ) : performance.startedAt ? (
                <span className={surfaces.metaText}>Started {performance.startedAt.slice(0, 16).replace('T', ' ')}</span>
              ) : (
                <span className={surfaces.metaText}>Timing unavailable</span>
              )}
            </div>
          </div>
          <div className={surfaces.metricGrid}>
            <div className={surfaces.metricTile}>
              <span className={surfaces.metricLabel}>Exercises</span>
              <span className={surfaces.metricValue}>{performance.totals.completedExerciseCount}</span>
            </div>
            <div className={surfaces.metricTile}>
              <span className={surfaces.metricLabel}>Sets</span>
              <span className={surfaces.metricValue}>{performance.totals.completedSetCount}</span>
            </div>
          </div>
        </section>

        <section className={surfaces.panel} aria-labelledby="session-detail-heading">
          <div className={surfaces.panelHeader}>
            <h2 className={surfaces.panelTitle} id="session-detail-heading">
              Session totals &amp; exercises
            </h2>
          </div>
          {performance.exercises.length === 0 ? (
            <EmptyView title="No performance data yet" message="This session has no completed exercises to summarize." />
          ) : (
            <OccurrencePerformanceSummary performance={performance} />
          )}
        </section>
      </div>
    </Page>
  );
}
