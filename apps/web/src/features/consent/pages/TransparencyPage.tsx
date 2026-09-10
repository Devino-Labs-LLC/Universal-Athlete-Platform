import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { Button } from '@/core/components/Button';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { fetchAthleteTransparency } from '@/features/consent/api/transparencyApi';
import { consentErrorMessage } from '@/features/consent/models/errors';
import { consentKeys } from '@/features/consent/models/queryKeys';

export function TransparencyPage() {
  const { apiClient, account } = useAuthSession();
  const [page, setPage] = useState(0);
  const activityQuery = useQuery({
    queryKey: consentKeys.transparency(account?.accountId ?? '', page),
    queryFn: () => fetchAthleteTransparency(apiClient, page, 20),
    enabled: Boolean(account?.accountId),
  });

  return (
    <Page
      title="Team activity"
      description="Recent team membership, sharing, and coach assignment events that involve you."
      actions={
        <Link to="/app/sharing" className="inlineLink">
          Sharing
        </Link>
      }
    >
      {activityQuery.isLoading ? <LoadingView message="Loading activity…" /> : null}
      {activityQuery.isError ? (
        <ErrorView
          message={consentErrorMessage(activityQuery.error, 'Unable to load activity.')}
          onRetry={() => void activityQuery.refetch()}
        />
      ) : null}
      {activityQuery.data && activityQuery.data.events.length === 0 ? (
        <EmptyView
          title="No team activity yet"
          message="Joining a team, sharing, or responding to a coach assignment will appear here."
        />
      ) : null}
      {activityQuery.data && activityQuery.data.events.length > 0 ? (
        <ul aria-label="Team activity">
          {activityQuery.data.events.map((event) => (
            <li key={`${event.type}-${event.occurredAt}-${event.teamName ?? ''}`}>
              <p>{event.description}</p>
              <p>
                {event.organizationName ? `${event.organizationName} · ` : ''}
                {event.teamName ?? 'Team'} · {event.occurredAt}
              </p>
            </li>
          ))}
        </ul>
      ) : null}
      {activityQuery.data?.hasMore ? (
        <Button type="button" variant="secondary" onClick={() => setPage((current) => current + 1)}>
          Older activity
        </Button>
      ) : null}
    </Page>
  );
}
