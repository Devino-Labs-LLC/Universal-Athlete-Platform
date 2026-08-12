import { Link, useParams } from 'react-router-dom';

import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { ExercisePerformanceHistoryTable } from '@/features/performance/components/ExercisePerformanceHistoryTable';
import { PersonalRecordsTable } from '@/features/performance/components/PersonalRecordsTable';
import { useExercisePerformanceHistory } from '@/features/performance/hooks/useExercisePerformanceHistory';
import { useExercisePersonalRecords } from '@/features/performance/hooks/usePersonalRecords';
import { isExercisePerformanceKeyNotFound, performanceErrorMessage } from '@/features/performance/models/errors';
import surfaces from '@/features/performance/styles/performanceSurfaces.module.scss';

export function ExercisePerformancePage() {
  const { exercisePerformanceKey } = useParams<{ exercisePerformanceKey: string }>();

  const historyQuery = useExercisePerformanceHistory(exercisePerformanceKey, { size: 20 });
  const recordsQuery = useExercisePersonalRecords(exercisePerformanceKey);

  if (historyQuery.isLoading) {
    return <LoadingView message="Loading exercise performance…" />;
  }

  if (historyQuery.isError) {
    if (isExercisePerformanceKeyNotFound(historyQuery.error)) {
      return (
        <Page title="Exercise performance" width="wide">
          <div className={surfaces.hub}>
            <section className={surfaces.panel}>
              <EmptyView
                title="No training history yet"
                message="This exercise has not been completed in any tracked training sessions."
              />
              <p className={surfaces.metaText}>
                <Link className={surfaces.panelLink} to="/app/performance/records">
                  Back to records
                </Link>
              </p>
            </section>
          </div>
        </Page>
      );
    }
    return <ErrorView message={performanceErrorMessage(historyQuery.error)} onRetry={() => historyQuery.refetch()} />;
  }

  const history = historyQuery.data!;

  return (
    <Page
      title={history.exerciseName}
      description="Performance history and personal records for this exercise."
      width="wide"
    >
      <div className={surfaces.hub}>
        <section className={surfaces.hero} aria-labelledby="exercise-hero-heading">
          <div className={surfaces.heroCopy}>
            <p className={surfaces.eyebrow} id="exercise-hero-heading">
              Exercise performance
            </p>
            <p className={surfaces.metaText}>
              {history.totalElements} completed session{history.totalElements === 1 ? '' : 's'} in tracked history
            </p>
          </div>
          <div className={surfaces.metricGrid}>
            <div className={surfaces.metricTile}>
              <span className={surfaces.metricLabel}>Sessions</span>
              <span className={surfaces.metricValue}>{history.totalElements}</span>
            </div>
            <div className={surfaces.metricTile}>
              <span className={surfaces.metricLabel}>Personal records</span>
              <span className={surfaces.metricValue}>
                {recordsQuery.data != null ? recordsQuery.data.length : '—'}
              </span>
            </div>
          </div>
        </section>

        <section className={surfaces.panel} aria-labelledby="exercise-prs-heading">
          <div className={surfaces.panelHeader}>
            <h2 className={surfaces.panelTitle} id="exercise-prs-heading">
              Personal records
            </h2>
          </div>
          {recordsQuery.isLoading ? <LoadingView message="Loading personal records…" /> : null}
          {recordsQuery.isError ? (
            <ErrorView message={performanceErrorMessage(recordsQuery.error)} onRetry={() => recordsQuery.refetch()} />
          ) : null}
          {recordsQuery.data ? <PersonalRecordsTable records={recordsQuery.data} showExerciseColumn={false} /> : null}
        </section>

        <section className={surfaces.panel} aria-labelledby="exercise-history-heading">
          <div className={surfaces.panelHeader}>
            <h2 className={surfaces.panelTitle} id="exercise-history-heading">
              Session history ({history.totalElements} session{history.totalElements === 1 ? '' : 's'})
            </h2>
          </div>
          <ExercisePerformanceHistoryTable entries={history.entries} />
        </section>
      </div>
    </Page>
  );
}
