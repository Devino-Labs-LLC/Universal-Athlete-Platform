import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { todayDateOnly, type DateOnly } from '@/core/date/dateOnly';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { useTeamReadiness } from '@/features/coach/hooks/useCoachQueries';
import { coachErrorMessage, isCoachNotFoundError } from '@/features/coach/models/errors';
import type { TeamReadiness } from '@/features/coach/models/schemas';
import styles from '@/features/coach/pages/CoachPages.module.scss';

/**
 * Renders only server-approved aggregate cells. Do not derive hidden counts
 * from totals or sibling cells.
 */
export function TeamReadinessPage() {
  const { teamId } = useParams<{ teamId: string }>();
  const [date, setDate] = useState<DateOnly>(() => todayDateOnly());
  const readinessQuery = useTeamReadiness(teamId ?? null, date);

  if (!teamId) {
    return (
      <Page title="Team readiness">
        <ErrorView message="Team was not specified." />
      </Page>
    );
  }

  return (
    <Page
      title="Team readiness"
      description="Distribution of stored readiness that athletes have shared with this team. This is not a team score."
      actions={
        <Link to={`/coach/teams/${teamId}/roster`} className={styles.inlineLink}>
          Roster
        </Link>
      }
    >
      <label className={styles.field}>
        <span className={styles.fieldLabel}>Date</span>
        <input
          className={styles.select}
          type="date"
          value={date}
          onChange={(event) => setDate(event.target.value as DateOnly)}
        />
      </label>

      {readinessQuery.isLoading ? <LoadingView message="Loading team readiness…" /> : null}

      {readinessQuery.isError ? (
        isCoachNotFoundError(readinessQuery.error) ? (
          <p className={styles.meta}>This team is unavailable or you do not have access.</p>
        ) : (
          <ErrorView
            message={coachErrorMessage(readinessQuery.error, 'Unable to load team readiness.')}
            onRetry={() => void readinessQuery.refetch()}
          />
        )
      ) : null}

      {readinessQuery.data ? <ReadinessBody view={readinessQuery.data} /> : null}
    </Page>
  );
}

function ReadinessBody({ view }: { view: TeamReadiness }) {
  if (view.status === 'INSUFFICIENT_DATA' || view.cohort === 'BELOW_MINIMUM') {
    return (
      <section className={styles.identityPanel} aria-label="Team readiness distribution">
        <h2 className={styles.sectionTitle}>Not enough shared readiness</h2>
        <p className={styles.meta}>
          A published view needs at least five athletes with current consent and stored readiness
          for this date. Exact small samples are not shown.
        </p>
        <p className={styles.meta}>Availability is not available for team aggregates yet.</p>
      </section>
    );
  }

  return (
    <section className={styles.identityPanel} aria-label="Team readiness distribution">
      <h2 className={styles.sectionTitle}>Readiness categories</h2>
      <p className={styles.meta}>
        {view.cohort === 'EXACT' && view.includedCount != null
          ? `Included athletes with stored shared readiness: ${view.includedCount}.`
          : 'Some counts are withheld so a small group cannot be identified.'}
      </p>
      <ul className={styles.meta}>
        {view.categoryDistribution.cells.map((cell) => (
          <li key={cell.category}>
            {labelForCategory(cell.category)}:{' '}
            {cell.publication === 'PUBLISHED' && cell.count != null ? cell.count : 'Suppressed'}
          </li>
        ))}
      </ul>
      <h2 className={styles.sectionTitle}>Limiting dimensions</h2>
      {view.limitingDimensionDistribution.status === 'INSUFFICIENT_DATA' ? (
        <p className={styles.meta}>Not enough shared limiting-dimension data for this date.</p>
      ) : (
        <ul className={styles.meta}>
          {view.limitingDimensionDistribution.cells.map((cell) => (
            <li key={cell.dimension}>
              {cell.dimension}:{' '}
              {cell.publication === 'PUBLISHED' && cell.count != null ? cell.count : 'Suppressed'}
            </li>
          ))}
        </ul>
      )}
      <p className={styles.meta}>Availability is not aggregated. This view does not assign a team score.</p>
    </section>
  );
}

function labelForCategory(category: string): string {
  switch (category) {
    case 'HIGH':
      return 'High';
    case 'MODERATE':
      return 'Moderate';
    case 'LOW':
      return 'Low';
    case 'INSUFFICIENT_DATA':
      return 'Insufficient stored data';
    default:
      return category;
  }
}
