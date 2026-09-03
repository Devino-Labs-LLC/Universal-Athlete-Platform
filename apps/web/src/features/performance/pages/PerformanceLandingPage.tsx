import { Link } from 'react-router-dom';

import { Badge } from '@/core/components/Badge';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { todayDateOnly, formatDateDisplay, parseDateOnly } from '@/core/date/dateOnly';
import { PerformanceSubNav } from '@/features/performance/components/PerformanceSubNav';
import { PersonalRecordsTable } from '@/features/performance/components/PersonalRecordsTable';
import { useRecentPersonalRecords } from '@/features/performance/hooks/usePersonalRecords';
import { useTrainingLoadHistory } from '@/features/performance/hooks/useTrainingLoadHistory';
import { composeAthleteProgress } from '@/features/performance/models/progressComposition';
import { personalRecordTypeLabel } from '@/features/performance/models/labels';
import { performanceErrorMessage } from '@/features/performance/models/errors';
import type { PersonalRecord } from '@/features/performance/models/schemas';
import surfaces from '@/features/performance/styles/performanceSurfaces.module.scss';
import { formatPersonalRecord } from '@/features/performance/utils/formatPersonalRecord';
import { useRecoveryHistory } from '@/features/recovery/hooks/useRecoveryCheckIns';
import { useTrainingOverview } from '@/features/training/hooks/usePlans';
import { addDays } from '@/features/training/utils/calendarRange';

function pickHeadlineRecord(records: PersonalRecord[]): PersonalRecord {
  return [...records].sort((left, right) => {
    const leftAt = left.achievedAt ?? '';
    const rightAt = right.achievedAt ?? '';
    return rightAt.localeCompare(leftAt);
  })[0]!;
}

export function PerformanceLandingPage() {
  const endDate = todayDateOnly();
  const startDate = addDays(endDate, -27);
  const recentRecordsQuery = useRecentPersonalRecords(30, 10);
  const overviewQuery = useTrainingOverview();
  const recoveryQuery = useRecoveryHistory(startDate, endDate, true);
  const loadQuery = useTrainingLoadHistory('WEEKLY', startDate, endDate, { page: 0, size: 4 });
  const records = recentRecordsQuery.data ?? [];
  const headline = records.length > 0 ? pickHeadlineRecord(records) : null;
  const weeklySummaries = loadQuery.data?.weeklySummaries ?? [];
  const progress = composeAthleteProgress({
    completedSessionCount: overviewQuery.data?.recentCompletedSessions?.length ?? 0,
    weeklyTrainingDays: overviewQuery.data?.weeklyLoadSummary?.trainingDays ?? null,
    recentPersonalRecordCount: records.length,
    recoveryCheckInCount: recoveryQuery.data?.days?.length ?? 0,
    ratedSessionCount: weeklySummaries.reduce(
      (sum, week) => sum + (week.ratedOccurrenceCount ?? 0),
      0,
    ),
    weeklyLoadPointCount: weeklySummaries.length,
  });

  return (
    <Page
      title="Performance"
      description="Recent personal records, exercise history, and training load."
      width="wide"
      actions={
        <div className={surfaces.metaRow}>
          <Link className={surfaces.panelLink} to="/app/performance/records">
            View all records
          </Link>
          <Link className={surfaces.panelLink} to="/app/performance/load">
            Training load
          </Link>
        </div>
      }
    >
      <PerformanceSubNav />

      <section className={surfaces.panel} aria-labelledby="athlete-progress-heading">
        <div className={surfaces.panelHeader}>
          <h2 className={surfaces.panelTitle} id="athlete-progress-heading">
            Athlete progress
          </h2>
        </div>
        <p className={surfaces.metaText}>{progress.headline}</p>
        <p className={surfaces.metaText}>
          Sessions {progress.consistency.count} · Rated effort {progress.effort.count} · Records{' '}
          {progress.performance.count} · Check-ins {progress.recovery.count}
        </p>
        {!progress.canShowLoadSeries ? (
          <p className={surfaces.metaText}>
            Load charts stay hidden until at least three weekly summaries exist.
          </p>
        ) : null}
      </section>

      {recentRecordsQuery.isLoading ? <LoadingView message="Loading personal records…" /> : null}
      {recentRecordsQuery.isError ? (
        <ErrorView
          message={performanceErrorMessage(recentRecordsQuery.error)}
          onRetry={() => recentRecordsQuery.refetch()}
        />
      ) : null}

      {recentRecordsQuery.data ? (
        <div className={surfaces.hub}>
          {headline ? (
            <section className={surfaces.hero} aria-labelledby="performance-hero-heading">
              <div className={surfaces.heroCopy}>
                <p className={surfaces.eyebrow} id="performance-hero-heading">
                  Latest personal record · 30 days
                </p>
                <h2 className={surfaces.heroTitle}>{headline.exerciseName}</h2>
                <p className={surfaces.heroValue}>{formatPersonalRecord(headline)}</p>
                <div className={surfaces.metaRow}>
                  <Badge tone="accent">{personalRecordTypeLabel(headline.recordType)}</Badge>
                  {headline.estimated ? <Badge tone="info">Estimated</Badge> : null}
                  {headline.achievedAt ? (
                    <span className={surfaces.metaText}>
                      {formatDateDisplay(parseDateOnly(headline.achievedAt.slice(0, 10)))}
                    </span>
                  ) : (
                    <span className={surfaces.metaText}>Date unavailable</span>
                  )}
                </div>
                <Link
                  className={surfaces.panelLink}
                  to={`/app/performance/exercises/${headline.exercisePerformanceKey}`}
                >
                  View exercise history
                </Link>
              </div>

              <div className={surfaces.metricGrid}>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Recent PRs</span>
                  <span className={surfaces.metricValue}>{records.length}</span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Window</span>
                  <span className={surfaces.metricValue}>30d</span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Explore</span>
                  <span className={surfaces.metricValue} style={{ fontSize: 'var(--uap-font-size-sm)' }}>
                    Records · Load
                  </span>
                </div>
              </div>
            </section>
          ) : (
            <section className={surfaces.panel} aria-labelledby="performance-empty-heading">
              <EmptyView
                title="No recent personal records"
                message="Complete training sessions to start building your performance history."
              />
              <p className={surfaces.metaText} id="performance-empty-heading">
                Personal records and load summaries appear after you log completed work in Training.
              </p>
              <div className={surfaces.metaRow}>
                <Link className={surfaces.panelLink} to="/app/training">
                  Open Training
                </Link>
                <Link className={surfaces.panelLink} to="/app/performance/load">
                  Review load history
                </Link>
              </div>
            </section>
          )}

          {records.length > 0 ? (
            <>
              <section className={surfaces.panel} aria-labelledby="recent-activity-heading">
                <div className={surfaces.panelHeader}>
                  <h2 className={surfaces.panelTitle} id="recent-activity-heading">
                    Recent performance activity
                  </h2>
                  <span className={surfaces.panelHint}>Last 30 days · up to 10 records</span>
                </div>
                <ul className={surfaces.activityList}>
                  {records.map((record) => (
                    <li key={record.id} className={surfaces.activityRow}>
                      <div className={surfaces.activityPrimary}>
                        <p className={surfaces.activityName}>
                          <Link
                            className={surfaces.panelLink}
                            to={`/app/performance/exercises/${record.exercisePerformanceKey}`}
                          >
                            {record.exerciseName}
                          </Link>
                        </p>
                        <p className={surfaces.activityMeta}>
                          {personalRecordTypeLabel(record.recordType)}
                          {record.achievedAt
                            ? ` · ${formatDateDisplay(parseDateOnly(record.achievedAt.slice(0, 10)))}`
                            : ''}
                          {record.estimated ? ' · Estimated' : ''}
                        </p>
                      </div>
                      <span className={surfaces.activityResult}>{formatPersonalRecord(record)}</span>
                    </li>
                  ))}
                </ul>
              </section>

              <section className={surfaces.panel} aria-labelledby="recent-table-heading">
                <div className={surfaces.panelHeader}>
                  <h2 className={surfaces.panelTitle} id="recent-table-heading">
                    Recent personal records (last 30 days)
                  </h2>
                  <Link className={surfaces.panelLink} to="/app/performance/records">
                    All records
                  </Link>
                </div>
                <PersonalRecordsTable records={records} />
              </section>
            </>
          ) : null}
        </div>
      ) : null}
    </Page>
  );
}
