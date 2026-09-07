import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { Button } from '@/core/components/Button';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import {
  useCoachOrganizationTeams,
  useCoachOrganizations,
} from '@/features/coach/hooks/useCoachQueries';
import { coachErrorMessage } from '@/features/coach/models/errors';
import { clearCoachTeamQueries } from '@/features/coach/models/invalidation';
import styles from '@/features/coach/pages/CoachPages.module.scss';

/**
 * Org → team context picker. Selected teamId is client navigation context only —
 * never treated as authorization (server enforces access).
 */
export function CoachHomePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { account } = useAuthSession();
  const organizationsQuery = useCoachOrganizations();
  const [organizationId, setOrganizationId] = useState('');
  const [teamId, setTeamId] = useState('');
  const [previousTeamId, setPreviousTeamId] = useState<string | null>(null);
  const teamsQuery = useCoachOrganizationTeams(organizationId || null);

  useEffect(() => {
    setTeamId('');
  }, [organizationId]);

  useEffect(() => {
    if (!account?.accountId) {
      return;
    }
    if (previousTeamId && previousTeamId !== teamId) {
      clearCoachTeamQueries(queryClient, account.accountId, previousTeamId);
    }
    setPreviousTeamId(teamId || null);
  }, [account?.accountId, previousTeamId, queryClient, teamId]);

  if (organizationsQuery.isLoading) {
    return <LoadingView message="Loading organizations…" />;
  }

  if (organizationsQuery.isError) {
    return (
      <Page title="Coach" description="Select an organization and team to view the roster.">
        <ErrorView
          message={coachErrorMessage(organizationsQuery.error, 'Unable to load organizations.')}
          onRetry={() => void organizationsQuery.refetch()}
        />
      </Page>
    );
  }

  const organizations = organizationsQuery.data ?? [];

  if (organizations.length === 0) {
    return (
      <Page title="Coach" description="Select an organization and team to view the roster.">
        <EmptyView
          title="No organizations"
          message="You need an organization membership before you can open a team roster."
        />
        <p className={styles.meta}>
          <Link to="/app/home">Athlete view</Link>
        </p>
      </Page>
    );
  }

  const teams = teamsQuery.data ?? [];

  return (
    <Page
      title="Coach"
      description="Choose organization and team context. Access is always checked on the server."
    >
      <form
        className={styles.pickerForm}
        onSubmit={(event) => {
          event.preventDefault();
          if (!teamId) {
            return;
          }
          navigate(`/coach/teams/${teamId}/roster`);
        }}
      >
        <label className={styles.field}>
          <span className={styles.fieldLabel}>Organization</span>
          <select
            className={styles.select}
            value={organizationId}
            onChange={(event) => setOrganizationId(event.target.value)}
          >
            <option value="">Select organization</option>
            {organizations.map((org) => (
              <option key={org.id} value={org.id}>
                {org.name}
              </option>
            ))}
          </select>
        </label>

        <label className={styles.field}>
          <span className={styles.fieldLabel}>Team</span>
          <select
            className={styles.select}
            value={teamId}
            disabled={!organizationId || teamsQuery.isLoading}
            onChange={(event) => setTeamId(event.target.value)}
          >
            <option value="">
              {!organizationId
                ? 'Select an organization first'
                : teamsQuery.isLoading
                  ? 'Loading teams…'
                  : 'Select team'}
            </option>
            {teams.map((team) => (
              <option key={team.id} value={team.id}>
                {team.name}
              </option>
            ))}
          </select>
        </label>

        {teamsQuery.isError ? (
          <ErrorView
            message={coachErrorMessage(teamsQuery.error, 'Unable to load teams.')}
            onRetry={() => void teamsQuery.refetch()}
          />
        ) : null}

        {organizationId && !teamsQuery.isLoading && !teamsQuery.isError && teams.length === 0 ? (
          <EmptyView
            title="No teams"
            message="This organization has no teams available for your memberships."
          />
        ) : null}

        <div className={styles.formActions}>
          <Button type="submit" disabled={!teamId}>
            Open roster
          </Button>
        </div>
      </form>
    </Page>
  );
}
