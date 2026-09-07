import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { formatDateDisplay, parseDateOnly, todayDateOnly, type DateOnly } from '@/core/date/dateOnly';
import { Badge } from '@/core/components/Badge';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { CoachAssignmentPanel } from '@/features/coach/components/CoachAssignmentPanel';
import {
  OverviewSection,
  RatingRow,
  ScopeChips,
} from '@/features/coach/components/OverviewSection';
import { useCoachAthleteOverview } from '@/features/coach/hooks/useCoachQueries';
import { coachErrorMessage, isCoachNotFoundError } from '@/features/coach/models/errors';
import { formatScore } from '@/features/coach/models/formatScore';
import styles from '@/features/coach/pages/CoachPages.module.scss';
import { personalRecordTypeLabel } from '@/features/performance/models/labels';
import { formatEnumLabel } from '@/features/profile/enumLabels';

export function CoachAthleteDetailPage() {
  const { teamId, athleteId } = useParams<{ teamId: string; athleteId: string }>();
  const [viewDate, setViewDate] = useState<DateOnly>(() => todayDateOnly());
  const overviewQuery = useCoachAthleteOverview(teamId ?? null, athleteId ?? null, viewDate);

  if (!teamId || !athleteId) {
    return (
      <Page title="Athlete overview">
        <ErrorView message="Team or athlete was not specified." />
      </Page>
    );
  }

  if (overviewQuery.isLoading) {
    return <LoadingView message="Loading athlete overview…" />;
  }

  if (overviewQuery.isError) {
    if (isCoachNotFoundError(overviewQuery.error)) {
      return (
        <Page
          title="Athlete overview"
          actions={
            <Link to={`/coach/teams/${teamId}/roster`} className={styles.inlineLink}>
              Back to roster
            </Link>
          }
        >
          <EmptyView
            title="Athlete unavailable"
            message="This athlete could not be found on this team or you do not have access."
          />
        </Page>
      );
    }

    return (
      <Page title="Athlete overview">
        <ErrorView
          message={coachErrorMessage(overviewQuery.error, 'Unable to load athlete overview.')}
          onRetry={() => void overviewQuery.refetch()}
        />
      </Page>
    );
  }

  const overview = overviewQuery.data;
  if (!overview) {
    return (
      <Page title="Athlete overview">
        <EmptyView title="No overview" message="No overview data was returned." />
      </Page>
    );
  }

  const recovery = overview.recoveryCheckIn.data;
  const adherence = overview.trainingAdherence.data;
  const history = overview.performanceHistory.data;

  return (
    <Page
      title={overview.displayName}
      description={`Consent-aware overview for ${formatDateDisplay(overview.viewDate)}.`}
      actions={
        <Link to={`/coach/teams/${teamId}/roster`} className={styles.inlineLink}>
          Back to roster
        </Link>
      }
    >
      <section className={styles.identityPanel} aria-label="Athlete identity">
        <div className={styles.identityHeader}>
          <h2 className={styles.sectionTitle}>{overview.displayName}</h2>
          <Badge tone="info">{formatEnumLabel(overview.role)}</Badge>
        </div>
        <p className={styles.meta}>View date · {formatDateDisplay(overview.viewDate)}</p>
        <label className={styles.field}>
          <span className={styles.fieldLabel}>Date</span>
          <input
            className={styles.select}
            type="date"
            value={viewDate}
            onChange={(event) => {
              const next = event.target.value;
              if (!next) {
                return;
              }
              try {
                setViewDate(parseDateOnly(next));
              } catch {
                // Ignore incomplete browser date input.
              }
            }}
          />
        </label>
        <div className={styles.scopeBlock}>
          <p className={styles.fieldLabel}>Effective scopes</p>
          <ScopeChips scopes={overview.effectiveScopes} />
        </div>
      </section>

      <CoachAssignmentPanel
        teamId={teamId}
        athleteId={athleteId}
        collaborationAvailable={overview.effectiveScopes.includes('TRAINING_COLLABORATION')}
      />

      <div className={styles.sectionStack}>
        <OverviewSection title="Availability" status={overview.availability.status}>
          <p className={styles.meta}>Availability data is not available yet.</p>
        </OverviewSection>

        <OverviewSection title="Readiness category" status={overview.readinessCategory.status}>
          {overview.readinessCategory.data ? (
            <div className={styles.statGrid}>
              <div className={styles.stat}>
                <span className={styles.statLabel}>Band</span>
                <span className={styles.statValue}>
                  {formatEnumLabel(overview.readinessCategory.data.readinessBand)}
                </span>
              </div>
              <div className={styles.stat}>
                <span className={styles.statLabel}>Data sufficiency</span>
                <span className={styles.statValue}>
                  {formatEnumLabel(overview.readinessCategory.data.dataSufficiency)}
                </span>
              </div>
            </div>
          ) : null}
        </OverviewSection>

        <OverviewSection title="Readiness score" status={overview.readinessScore.status}>
          {overview.readinessScore.data ? (
            <div className={styles.statGrid}>
              <div className={styles.stat}>
                <span className={styles.statLabel}>Score</span>
                <span className={styles.statValue}>
                  {formatScore(overview.readinessScore.data.readinessScore)}
                </span>
              </div>
              <div className={styles.stat}>
                <span className={styles.statLabel}>Data sufficiency</span>
                <span className={styles.statValue}>
                  {formatEnumLabel(overview.readinessScore.data.dataSufficiency)}
                </span>
              </div>
              {overview.readinessScore.data.summaryReasonCode ? (
                <div className={styles.stat}>
                  <span className={styles.statLabel}>Summary reason</span>
                  <span className={styles.statValue}>
                    {formatEnumLabel(overview.readinessScore.data.summaryReasonCode)}
                  </span>
                </div>
              ) : null}
            </div>
          ) : null}
        </OverviewSection>

        <OverviewSection title="Limiting dimensions" status={overview.limitingDimensions.status}>
          {overview.limitingDimensions.data ? (
            overview.limitingDimensions.data.limitingDimensions.length > 0 ? (
              <ul className={styles.simpleList}>
                {overview.limitingDimensions.data.limitingDimensions.map((dimension) => (
                  <li key={dimension}>{formatEnumLabel(dimension)}</li>
                ))}
              </ul>
            ) : (
              <p className={styles.meta}>No limiting dimensions for this date.</p>
            )
          ) : null}
        </OverviewSection>

        <OverviewSection title="Recovery check-in" status={overview.recoveryCheckIn.status}>
          {recovery ? (
            <div className={styles.sectionBodyStack}>
              <p className={styles.meta}>
                Check-in date · {formatDateDisplay(recovery.checkInDate)}
                {recovery.completeness
                  ? ` · ${formatEnumLabel(recovery.completeness)}`
                  : ''}
              </p>
              <div className={styles.statGrid}>
                <RatingRow label="Sleep quality" rating={recovery.sleepQuality} />
                <RatingRow label="Mood" rating={recovery.mood} />
                <RatingRow label="Fatigue" rating={recovery.fatigue} />
                <RatingRow label="Muscle soreness" rating={recovery.muscleSoreness} />
                <RatingRow label="Stress" rating={recovery.stress} />
              </div>
              {recovery.notes ? <p className={styles.notes}>{recovery.notes}</p> : null}
              {recovery.discomfortAreas.length > 0 ? (
                <div>
                  <p className={styles.fieldLabel}>Discomfort areas</p>
                  <ul className={styles.simpleList}>
                    {recovery.discomfortAreas.map((area) => (
                      <li key={`${area.bodyArea}-${area.orderIndex}`}>
                        {formatEnumLabel(area.bodyArea)}
                        {area.side ? ` · ${formatEnumLabel(area.side)}` : ''}
                        {area.intensity
                          ? ` · ${area.intensity.label} (${area.intensity.value})`
                          : ''}
                        {area.notes ? ` — ${area.notes}` : ''}
                      </li>
                    ))}
                  </ul>
                </div>
              ) : null}
            </div>
          ) : null}
        </OverviewSection>

        <OverviewSection title="Training adherence" status={overview.trainingAdherence.status}>
          {adherence ? (
            <div className={styles.statGrid}>
              <div className={styles.stat}>
                <span className={styles.statLabel}>Scheduled</span>
                <span className={styles.statValue}>{adherence.scheduledCount}</span>
              </div>
              <div className={styles.stat}>
                <span className={styles.statLabel}>Completed</span>
                <span className={styles.statValue}>{adherence.completedCount}</span>
              </div>
              <div className={styles.stat}>
                <span className={styles.statLabel}>Skipped</span>
                <span className={styles.statValue}>{adherence.skippedCount}</span>
              </div>
              <div className={styles.stat}>
                <span className={styles.statLabel}>In progress</span>
                <span className={styles.statValue}>{adherence.inProgressCount}</span>
              </div>
              <div className={styles.stat}>
                <span className={styles.statLabel}>Cancelled</span>
                <span className={styles.statValue}>{adherence.cancelledCount}</span>
              </div>
            </div>
          ) : null}
        </OverviewSection>

        <OverviewSection title="Performance history" status={overview.performanceHistory.status}>
          {history ? (
            history.recentRecords.length > 0 ? (
              <ul className={styles.simpleList}>
                {history.recentRecords.map((record, index) => (
                  <li key={`${record.exerciseName}-${record.achievedAt}-${index}`}>
                    <strong>{record.exerciseName}</strong>
                    {' · '}
                    {personalRecordTypeLabel(record.recordType)}
                    {record.recordQualifier ? ` · ${record.recordQualifier}` : ''}
                  </li>
                ))}
              </ul>
            ) : (
              <p className={styles.meta}>No recent records.</p>
            )
          ) : null}
        </OverviewSection>
      </div>
    </Page>
  );
}
