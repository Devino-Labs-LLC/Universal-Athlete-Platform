import { Link, useParams } from 'react-router-dom';

import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { useTeamRoster } from '@/features/coach/hooks/useCoachQueries';
import { coachErrorMessage, isCoachNotFoundError } from '@/features/coach/models/errors';
import styles from '@/features/coach/pages/CoachPages.module.scss';

export function TeamRosterPage() {
  const { teamId } = useParams<{ teamId: string }>();
  const rosterQuery = useTeamRoster(teamId ?? null);

  if (!teamId) {
    return (
      <Page title="Team roster">
        <ErrorView message="Team was not specified." />
      </Page>
    );
  }

  if (rosterQuery.isLoading) {
    return <LoadingView message="Loading roster…" />;
  }

  if (rosterQuery.isError) {
    if (isCoachNotFoundError(rosterQuery.error)) {
      return (
        <Page
          title="Team roster"
          actions={
            <Link to="/coach" className={styles.inlineLink}>
              Choose another team
            </Link>
          }
        >
          <EmptyView
            title="Team unavailable"
            message="This team could not be found or you do not have access."
          />
        </Page>
      );
    }

    return (
      <Page title="Team roster">
        <ErrorView
          message={coachErrorMessage(rosterQuery.error, 'Unable to load roster.')}
          onRetry={() => void rosterQuery.refetch()}
        />
      </Page>
    );
  }

  const roster = rosterQuery.data ?? [];

  if (roster.length === 0) {
    return (
      <Page
        title="Team roster"
        description="Active athletes on this team."
        actions={
          <Link to="/coach" className={styles.inlineLink}>
            Change team
          </Link>
        }
      >
        <EmptyView title="No athletes" message="There are no active athletes on this roster." />
      </Page>
    );
  }

  return (
    <Page
      title="Team roster"
      description="Active athletes on this team. Only roster-safe fields are shown."
      actions={
        <Link to="/coach" className={styles.inlineLink}>
          Change team
        </Link>
      }
    >
      <ul className={styles.list} aria-label="Team roster">
        {roster.map((entry) => (
          <li key={entry.membershipId} className={styles.card}>
            <div className={styles.cardBody}>
              <Link
                to={`/coach/teams/${teamId}/athletes/${entry.athleteId}`}
                className={styles.athleteLink}
              >
                {entry.displayName}
              </Link>
              <p className={styles.meta}>Athlete · Active</p>
            </div>
          </li>
        ))}
      </ul>
    </Page>
  );
}
