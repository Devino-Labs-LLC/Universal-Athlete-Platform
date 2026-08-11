import { Link, useParams } from 'react-router-dom';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import tableStyles from '@/core/components/Table.module.scss';
import type { DateOnly } from '@/core/date/dateOnly';
import { useAthleteStateSnapshot, useAthleteStateVersions } from '@/features/recovery/hooks/useAthleteState';
import { recoveryErrorMessage } from '@/features/recovery/models/errors';
import { formatVolumeKg, formatDistance, formatDurationSeconds } from '@/features/recovery/utils/formatMetrics';

export function AthleteStatePage() {
  const { snapshotId } = useParams<{ snapshotId: string }>();
  const snapshotQuery = useAthleteStateSnapshot(snapshotId);
  const versionsQuery = useAthleteStateVersions(snapshotQuery.data?.stateDate as DateOnly | undefined);

  if (snapshotQuery.isLoading) {
    return <LoadingView message="Loading daily athlete state…" />;
  }

  if (snapshotQuery.isError) {
    return <ErrorView message={recoveryErrorMessage(snapshotQuery.error)} onRetry={() => snapshotQuery.refetch()} />;
  }

  const snapshot = snapshotQuery.data!;
  const otherVersions = (versionsQuery.data ?? []).filter((version) => version.snapshotId !== snapshot.snapshotId);

  return (
    <Page title="Daily athlete state" description={`Snapshot for ${snapshot.stateDate} (version ${snapshot.snapshotVersion}).`}>
      <section className="card" style={{ marginBottom: '1rem' }}>
        <h2 className="cardTitle">Training load</h2>
        <div className="statGrid">
          <div className="stat">
            <span className="statLabel">Volume</span>
            <span className="statValue">
              {snapshot.trainingLoad.totalVolumeKilograms != null
                ? formatVolumeKg(snapshot.trainingLoad.totalVolumeKilograms)
                : '—'}
            </span>
          </div>
          <div className="stat">
            <span className="statLabel">Duration</span>
            <span className="statValue">{formatDurationSeconds(snapshot.trainingLoad.totalDurationSeconds)}</span>
          </div>
          <div className="stat">
            <span className="statLabel">Distance</span>
            <span className="statValue">
              {snapshot.trainingLoad.totalDistanceMeters != null
                ? formatDistance(snapshot.trainingLoad.totalDistanceMeters)
                : '—'}
            </span>
          </div>
          <div className="stat">
            <span className="statLabel">Completed exercises</span>
            <span className="statValue">{snapshot.trainingLoad.completedExerciseCount}</span>
          </div>
        </div>
      </section>

      <section className="card" style={{ marginBottom: '1rem' }}>
        <h2 className="cardTitle">Schedule</h2>
        <p className={tableStyles.subtle}>
          {snapshot.schedule.scheduledOccurrenceCount} scheduled · {snapshot.schedule.completedScheduledCount} completed
          · {snapshot.schedule.skippedScheduledCount} skipped · {snapshot.schedule.cancelledScheduledCount} cancelled
        </p>
      </section>

      {otherVersions.length > 0 ? (
        <section className="card">
          <h2 className="cardTitle">Other versions on this date</h2>
          <ul style={{ display: 'grid', gap: '0.35rem', margin: 0, paddingLeft: '1.1rem' }}>
            {otherVersions.map((version) => (
              <li key={version.snapshotId}>
                Version {version.snapshotVersion} ({version.generationReason ?? 'generated'}) —{' '}
                <Link to={`/app/recovery/state/${snapshot.snapshotId}/compare?other=${version.snapshotId}`}>
                  Compare
                </Link>
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </Page>
  );
}
