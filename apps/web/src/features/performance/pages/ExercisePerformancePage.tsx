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
        <Page title="Exercise performance">
          <EmptyView
            title="No training history yet"
            message="This exercise has not been completed in any tracked training sessions."
          />
          <p style={{ marginTop: '1rem' }}>
            <Link to="/app/performance/records">Back to records</Link>
          </p>
        </Page>
      );
    }
    return <ErrorView message={performanceErrorMessage(historyQuery.error)} onRetry={() => historyQuery.refetch()} />;
  }

  const history = historyQuery.data!;

  return (
    <Page title={history.exerciseName} description="Performance history and personal records for this exercise.">
      <section className="card" style={{ marginBottom: '1rem' }}>
        <h2 className="cardTitle">Personal records</h2>
        {recordsQuery.isLoading ? <LoadingView message="Loading personal records…" /> : null}
        {recordsQuery.isError ? (
          <ErrorView message={performanceErrorMessage(recordsQuery.error)} onRetry={() => recordsQuery.refetch()} />
        ) : null}
        {recordsQuery.data ? <PersonalRecordsTable records={recordsQuery.data} showExerciseColumn={false} /> : null}
      </section>

      <section className="card">
        <h2 className="cardTitle">
          Session history ({history.totalElements} session{history.totalElements === 1 ? '' : 's'})
        </h2>
        <ExercisePerformanceHistoryTable entries={history.entries} />
      </section>
    </Page>
  );
}
