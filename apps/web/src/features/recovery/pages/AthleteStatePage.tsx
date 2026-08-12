import { Link, useParams } from 'react-router-dom';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import type { DateOnly } from '@/core/date/dateOnly';
import { useAthleteStateSnapshot, useAthleteStateVersions } from '@/features/recovery/hooks/useAthleteState';
import { recoveryErrorMessage } from '@/features/recovery/models/errors';
import surfaces from '@/features/recovery/styles/recoverySurfaces.module.scss';
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
    <Page
      title="Daily athlete state"
      description={`Snapshot for ${snapshot.stateDate} (version ${snapshot.snapshotVersion}).`}
      width="wide"
    >
      <div className={surfaces.hub}>
        <section className={surfaces.panel} aria-labelledby="load-heading">
          <div className={surfaces.panelHeader}>
            <h2 className={surfaces.panelTitle} id="load-heading">
              Training load
            </h2>
            <span className={surfaces.panelHint}>Historical snapshot · reference only</span>
          </div>
          <div className={surfaces.metricGrid}>
            <div className={surfaces.metricTile}>
              <span className={surfaces.metricLabel}>Volume</span>
              <span className={surfaces.metricValue}>
                {snapshot.trainingLoad.totalVolumeKilograms != null
                  ? formatVolumeKg(snapshot.trainingLoad.totalVolumeKilograms)
                  : '—'}
              </span>
            </div>
            <div className={surfaces.metricTile}>
              <span className={surfaces.metricLabel}>Duration</span>
              <span className={surfaces.metricValue}>
                {formatDurationSeconds(snapshot.trainingLoad.totalDurationSeconds)}
              </span>
            </div>
            <div className={surfaces.metricTile}>
              <span className={surfaces.metricLabel}>Distance</span>
              <span className={surfaces.metricValue}>
                {snapshot.trainingLoad.totalDistanceMeters != null
                  ? formatDistance(snapshot.trainingLoad.totalDistanceMeters)
                  : '—'}
              </span>
            </div>
            <div className={surfaces.metricTile}>
              <span className={surfaces.metricLabel}>Completed exercises</span>
              <span className={surfaces.metricValue}>{snapshot.trainingLoad.completedExerciseCount}</span>
            </div>
          </div>
        </section>

        <section className={surfaces.panel} aria-labelledby="schedule-heading">
          <div className={surfaces.panelHeader}>
            <h2 className={surfaces.panelTitle} id="schedule-heading">
              Schedule
            </h2>
          </div>
          <p className={surfaces.metaText}>
            {snapshot.schedule.scheduledOccurrenceCount} scheduled · {snapshot.schedule.completedScheduledCount}{' '}
            completed · {snapshot.schedule.skippedScheduledCount} skipped ·{' '}
            {snapshot.schedule.cancelledScheduledCount} cancelled
          </p>
        </section>

        {otherVersions.length > 0 ? (
          <section className={surfaces.panel} aria-labelledby="versions-heading">
            <div className={surfaces.panelHeader}>
              <h2 className={surfaces.panelTitle} id="versions-heading">
                Other versions on this date
              </h2>
            </div>
            <ul className={surfaces.versionList}>
              {otherVersions.map((version) => (
                <li key={version.snapshotId} className={surfaces.trendRow}>
                  <span className={surfaces.trendName}>
                    Version {version.snapshotVersion} ({version.generationReason ?? 'generated'})
                  </span>
                  <Link
                    className={surfaces.panelLink}
                    to={`/app/recovery/state/${snapshot.snapshotId}/compare?other=${version.snapshotId}`}
                  >
                    Compare
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        ) : null}
      </div>
    </Page>
  );
}
